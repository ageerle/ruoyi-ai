# Token 窗口内存管理实现总结

## 概述
基于 LangChain4j 的 ChatMemory 机制，实现了一套智能的 Token 窗口内存管理系统。该系统能够根据不同模型的 Token 限制，自动管理对话上下文，支持**策略模式**的压缩机制，包括摘要压缩、Token 截断和滑动窗口三种策略。

## 架构设计

### 1. 整体框架
```
用户消息 → ChatServiceFacade → ChatMemoryFactory ──→ 创建摘要模型
                                      │
                                      ▼
                          ┌─────────────────────────┐
                          │  strategy 配置判断       │
                          └───────────┬─────────────┘
                                      │
              ┌───────────────────────┼───────────────────────┐
              ▼                       ▼                       ▼
        strategy=message       strategy=token          strategy=hybrid
              │                       │                       │
              ▼                       ▼                       ▼
    MessageWindowChatMemory   TokenBasedChatMemory   TokenBasedChatMemory
    (LangChain4j原生)                │                       │
              │                       └───────────┬───────────┘
              │                                   │
              ▼                                   ▼
        固定消息数量                    PersistentChatMemoryStore
        不走策略框架                            │
                                              ▼
                                        数据库查询历史消息
                                              │
                                              ▼
                                        Token 计数检查
                                              │
                                    ┌─────────┴─────────┐
                                    ▼                   ▼
                              Token 未超限        Token 超限/达到阈值
                                    │                   │
                                    ▼                   ▼
                              直接返回消息     CompressionStrategyManager
                                                        │
                                          ┌────────────┼────────────┐
                                          ▼            ▼            ▼
                                    Summarization  Truncation  SlidingWindow
                                     (优先级50)    (优先级100)   (优先级150)
                                          ▼            ▼            ▼
                                     摘要压缩      Token截断    保留最近N条
```

### 2. 职责划分

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           ChatServiceFacade                                  │
│  职责：路由协调、上下文构建、响应处理                                         │
│  方法：                                                                      │
│  - createChatMemory(memoryId, model) → 委托给 ChatMemoryFactory              │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           ChatMemoryFactory                                  │
│  职责：                                                                      │
│  - 创建 ChatMemory 实例                                                      │
│  - 自动创建摘要模型（根据配置策略）                                           │
│  - 智能映射轻量级摘要模型                                                     │
│  - 处理未知模型回退逻辑                                                       │
│  方法：                                                                      │
│  - create(memoryId, model) → 自动创建摘要模型                                │
│  - create(memoryId, model, summarizer) → 手动指定摘要模型                    │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2. 策略模式架构

```
                    ┌──────────────────────────┐
                    │ MemoryCompressionStrategy│ (接口)
                    └────────────┬─────────────┘
                                 │
        ┌────────────────────────┼────────────────────────┐
        │                        │                        │
        ▼                        ▼                        ▼
┌───────────────┐      ┌─────────────────┐      ┌──────────────────┐
│Summarization  │      │  Truncation     │      │  SlidingWindow   │
│  Strategy     │      │   Strategy      │      │    Strategy      │
├───────────────┤      ├─────────────────┤      ├──────────────────┤
│优先级: 50     │      │优先级: 100      │      │优先级: 150       │
│可组合: true   │      │可组合: false    │      │可组合: false     │
│触发: 达到比例 │      │触发: Token超限  │      │触发: 消息数超限  │
└───────────────┘      └─────────────────┘      └──────────────────┘
```

### 3. 核心组件

#### A. ModelTokenLimits (模型 Token 限制映射)
- **文件**: `org.ruoyi.service.chat.impl.memory.ModelTokenLimits`
- **职责**: 维护 100+ 主流 AI 模型的 Token 限制信息
- **支持模型**:
  - OpenAI: GPT-4.1, GPT-4o, o1/o3/o4-mini 系列
  - DeepSeek: V3, R1 系列
  - 智谱: GLM-5, GLM-4.5, GLM-5.1 系列
  - 通义千问: Qwen3, Qwen-max/plus 系列
  - Claude: Claude 4, Claude 3.5/3.7 系列
  - Google: Gemini 2.5 Pro/Flash 系列
  - 字节豆包: Doubao 1.5 系列
  - xAI: Grok 3 系列
  - Ollama 本地模型: llama3, mistral, qwen2 等

```java
// 获取模型的 Token 限制
int limit = ModelTokenLimits.getLimit("gpt-4o");  // 返回 128000

// 获取输入 Token 上限（预留回复空间）
int inputLimit = ModelTokenLimits.getInputLimit("gpt-4o", 2000);  // 返回 126000

// 检查模型是否已知
boolean known = ModelTokenLimits.isKnownModel("gpt-4o");  // 返回 true
```

#### B. TokenCounter (Token 计数器)
- **文件**: `org.ruoyi.service.chat.impl.memory.TokenCounter`
- **职责**: 估算文本和消息的 Token 数量
- **估算规则**:
  - 中文: 约 2 字符 = 1 token
  - 英文: 约 4 字符 = 1 token
  - 每条消息固定开销: 4 tokens（格式标记）
  - 对话总开销: 3 tokens

```java
TokenCounter counter = new TokenCounter();

// 计算文本 Token 数
int tokens = counter.countTokens("你好世界 Hello World");

// 计算消息列表 Token 数
int total = counter.countMessages(messages);

// 估算指定 Token 预算下可容纳的消息数量
int maxMsgs = counter.estimateMaxMessages(8000, 50);
```

#### C. TokenBasedChatMemory (Token 窗口内存)
- **文件**: `org.ruoyi.service.chat.impl.memory.TokenBasedChatMemory`
- **职责**: 核心内存管理实现，支持策略框架

```java
TokenBasedChatMemory memory = TokenBasedChatMemory.builder()
    .memoryId(sessionId)
    .maxTokens(128000)                   // 最大 Token 数
    .tokenCounter(tokenCounter)          // Token 计数器
    .store(persistentStore)              // 持久化存储
    .summarizeEnabled(true)              // 启用摘要
    .summarizeTokenRatio(0.7)            // 70% 时触发摘要
    .summarizeThreshold(10)              // 最少 10 条消息才摘要
    .summarizer(llmModel)                // 摘要模型
    .preserveSystemMessages(true)        // 保护系统消息
    .reservedForReply(2000)              // 预留回复空间
    .strategyManager(strategyManager)    // 策略管理器
    .build();
```

#### D. CompressionStrategyManager (策略管理器)
- **文件**: `org.ruoyi.service.chat.impl.memory.strategy.CompressionStrategyManager`
- **职责**: 管理多个压缩策略，按优先级执行

```java
// 执行压缩（按优先级依次尝试策略）
CompressionResult result = strategyManager.execute(context);

// 获取可用策略列表
List<String> strategies = strategyManager.getAvailableStrategies();
```

#### E. MemoryCompressionStrategy (压缩策略接口)
- **文件**: `org.ruoyi.service.chat.impl.memory.strategy.MemoryCompressionStrategy`
- **职责**: 定义压缩策略的抽象行为

```java
public interface MemoryCompressionStrategy {
    String getName();                                    // 策略名称
    boolean needsCompression(CompressionContext context); // 是否需要压缩
    CompressionResult compress(CompressionContext context); // 执行压缩
    default int getPriority() { return 100; }            // 优先级
    default boolean isComposable() { return false; }     // 是否可组合
}
```

#### F. ChatMemoryFactory (内存工厂)
- **文件**: `org.ruoyi.service.chat.impl.memory.ChatMemoryFactory`
- **职责**:
  - 根据配置和模型创建合适的 ChatMemory 实例
  - 自动创建摘要模型（根据配置策略）
  - 智能映射轻量级摘要模型
  - API 地址修正（智谱 GLM 等需要特殊路径）

```java
// 创建内存（自动创建摘要模型）
ChatMemory memory = chatMemoryFactory.create(sessionId, chatModelVo);

// 创建内存（手动指定摘要模型）
ChatMemory memory = chatMemoryFactory.create(sessionId, chatModelVo, summarizerModel);

// 获取模型 Token 限制
int limit = chatMemoryFactory.getModelTokenLimit("gpt-4o");
```

**摘要模型策略**（由 ChatMemoryFactory 内部处理）：
- `current`: 使用当前对话模型（默认）
- `smart`: 智能映射到轻量级模型
- `custom`: 使用自定义模型

**API 地址修正**：
- 智谱 GLM 系列：添加 `/api/paas/v4/` 路径
- 千问 Qwen 系列：添加 `/compatible-mode/v1/` 路径
- 摘要模型创建时自动处理此修正

### 4. 配置体系

#### ChatMemoryProperties
配置文件前缀：`chat.memory`
```yaml
chat:
  memory:
    # 是否启用长期记忆
    enabled: true

    # ========== 内存管理策略 ==========
    # message: 固定消息数量（使用 LangChain4j 原生 MessageWindowChatMemory，不支持 Token 管理和摘要）
    # token: 基于 Token 数量（使用策略框架，支持摘要/截断/滑动窗口）
    # hybrid: Token + 摘要（使用策略框架，功能最全）
    strategy: token

    # ========== 通用配置（所有策略生效）==========
    # 预留给回复的 Token 数
    reserved-for-reply: 2000
    # 是否保护系统消息不被截断
    preserve-system-messages: true

    # ========== message 策略配置 ==========
    # 仅当 strategy=message 时生效，控制保留的消息数量
    max-messages: 20

    # ========== token/hybrid 策略配置 ==========
    # 以下配置仅当 strategy=token 或 hybrid 时生效
    # strategy=message 时这些配置不生效：
    # - max-tokens: 不考虑 Token 限制
    # - summarize-token-ratio: 不触发摘要
    # - summarize-threshold: 不触发摘要
    # - summarizer-strategy: 不创建摘要模型

    # 最大 Token 数（null 则根据模型自动获取）
    max-tokens: null

    # 触发摘要的 Token 使用比例（0.7 = 70%）
    summarize-token-ratio: 0.7

    # 触发摘要的最小消息数
    summarize-threshold: 10

    # 摘要模型策略: current(当前模型) / smart(智能映射) / custom(自定义)
    summarizer-strategy: current
```

#### 三种顶层策略对比

| strategy | 创建的实例 | 压缩策略框架 | Token 管理 | 摘要功能 | 适用场景 |
|----------|-----------|-------------|-----------|---------|---------|
| `message` | MessageWindowChatMemory | ❌ 不使用 | ❌ 不考虑 | ❌ 不支持 | 简单对话、固定轮次 |
| `token` | TokenBasedChatMemory | ✅ 使用 | ✅ 根据模型自动 | ✅ 支持 | 通用场景 |
| `hybrid` | TokenBasedChatMemory | ✅ 使用 | ✅ 根据模型自动 | ✅ 支持 | 长对话、需要保留语义 |

> **注意**: `message` 策略使用 LangChain4j 原生的 `MessageWindowChatMemory`，不走自定义的策略框架，因此不支持 Token 管理和摘要功能。

## 策略触发机制

### 三种策略对比

| 策略 | 优先级 | 触发条件 | 截断依据 | 系统消息 |
|------|--------|----------|----------|----------|
| **摘要策略** | 50 (最高) | Token 达到比例阈值 | Token（摘要压缩） | 保留 |
| **截断策略** | 100 (默认) | Token 超限 | Token（精确截断） | 保留 |
| **滑动窗口** | 150 (兜底) | **仅消息数 > 窗口大小** | **消息数量**（不考虑 Token） | 保留 |

> **重要说明**: 滑动窗口策略**只检查消息数量**，完全不考虑 Token 限制。这意味着即使单条消息 Token 很大，只要消息数不超过窗口大小，就不会触发截断。适用于简单场景或需要固定对话轮次的场景。

### 策略适用场景

| 策略 | 适用场景 | 说明 |
|------|----------|------|
| **SummarizationStrategy** | 长对话、需要保留语义 | 提前压缩，保留对话要点 |
| **TruncationStrategy** | Token 精确控制 | 按 Token 限制精确截断 |
| **SlidingWindowStrategy** | 简单场景、固定对话轮次 | 只按消息数量截断，不考虑 Token 大小，适合需要保留固定轮次对话的场景 |

### 触发流程图

```
Token 使用情况:
  0% ────────────────────── 70% ────────────────────── 100%
  │                         │                         │
  正常                      触发摘要                   触发截断
                            (保留语义)                 (强制兜底)

执行顺序:
  1. SummarizationStrategy (优先级50)
     └─ Token >= 比例阈值 → 执行摘要压缩
     └─ 压缩后检查是否超限

  2. TruncationStrategy (优先级100)
     └─ 如果仍超限 → 执行截断
     └─ 保证最终不超限

  3. SlidingWindowStrategy (优先级150)
     └─ 消息数 > 窗口大小 → 保留最近 N 条
     └─ 注意：不检查 Token，只检查消息数量
```

### 详细触发条件

#### 摘要策略 (SummarizationStrategy)
```java
// 触发条件（无需超限，达到比例即可）
return context.getSummarizer() != null                    // 有摘要模型
    && context.getMessages().size() > threshold           // 消息数足够
    && context.getUsageRatio() >= summarizeTokenRatio;    // Token 达到比例阈值
```

#### 截断策略 (TruncationStrategy)
```java
// 触发条件（必须超限）
return context.isOverLimit();  // Token > 有效上限
```

#### 滑动窗口策略 (SlidingWindowStrategy)
```java
// 触发条件（只检查消息数量，不考虑 Token）
return context.getMessages().size() > windowSize;  // 消息数 > 窗口大小

// 处理逻辑
// 1. 分离系统消息和普通消息
// 2. 保留所有系统消息
// 3. 保留最近 N 条普通消息（N = 窗口大小，默认 20）
// 4. 合并返回
```

### 生产环境示例

以 GLM-4.5-AIR (131072 tokens) 为例：

```
maxTokens = 131072 (自动获取)
reservedForReply = 2000
有效上限 = 129072 tokens

摘要触发阈值 = 129072 × 70% = 90,350 tokens

当 Token 达到 90,350 且消息数 > 10 时：
  → 触发摘要压缩（提前介入，保留语义）
  → 摘要后通常 < 129072，无需截断

如果摘要后仍超限：
  → 触发截断策略（强制兜底）
```

### 滑动窗口策略独立说明

滑动窗口策略是一个**独立的消息数量控制策略**，与 Token 无关：

```
特点：
  - 只检查消息数量，不考虑 Token 大小
  - 保留系统消息 + 最近 N 条普通消息
  - 默认窗口大小 = 20 条

适用场景：
  - 需要固定对话轮次的场景
  - 不关心 Token 消耗的简单对话
  - 作为其他策略的补充

注意事项：
  - 如果单条消息 Token 很大，可能导致 API 调用失败
  - 建议与 Token 截断策略配合使用
```

## 降级机制

### 多层保障架构

```
Token 超限/达到阈值
        │
        ▼
┌───────────────────────────────┐
│ CompressionStrategyManager    │
│ (策略框架)                     │
│                               │
│ 1. 按优先级执行策略            │
│ 2. 检查结果是否仍超限          │
│ 3. 超限则执行 forceTruncate() │
└───────────────────────────────┘
        │
        ├─► 成功且不超限 → 返回
        │
        └─► 失败/仍超限 → forceTruncate() 强制截断
                              │
                              ▼
                    ┌─────────────────────┐
                    │ emergencyTruncate() │
                    │ (TokenBasedChatMemory)│
                    │ 最终兜底，确保不超限  │
                    └─────────────────────┘
```

### 四层保障机制

| 层级 | 组件 | 职责 | 触发条件 |
|------|------|------|----------|
| 第1层 | SummarizationStrategy | 摘要压缩，保留语义 | Token 达到比例阈值 |
| 第2层 | TruncationStrategy | Token 截断 | Token 超限 |
| 第3层 | forceTruncate() | 策略后强制截断 | 策略执行后仍超限 |
| 第4层 | emergencyTruncate() | 紧急截断兜底 | 无策略框架或策略失败 |

### 边界保护

| 检查点 | 保护措施 | 说明 |
|--------|----------|------|
| effectiveMaxTokens | `Math.max(1, effective)` | 确保返回值 >= 1，避免负数/零 |
| getUsageRatio() | 除零保护 | 当 effectiveMax <= 0 时返回 1.0 |
| 系统消息超长 | 仅返回系统消息 | 系统消息占用全部空间时的降级 |

### 降级场景示例

| 场景 | 处理方式 |
|------|----------|
| reservedForReply >= maxTokens | effectiveMaxTokens = 1，视为已满，仅保留系统消息 |
| 所有策略执行后仍超限 | forceTruncate() 强制截断到限制内 |
| 策略框架不可用 | emergencyTruncate() 直接截断 |
| 系统消息占用全部空间 | 只返回系统消息 |

## 摘要机制详解

### 摘要逻辑

**只摘要前半部分的消息，保留后半部分完整**：

```
原消息: [Msg1, Msg2, Msg3, Msg4, Msg5, Msg6, Msg7, Msg8, Msg9, Msg10]

         ↓ 分割（一半摘要，一半保留）

待摘要: [Msg1, Msg2, Msg3, Msg4, Msg5]  → 生成摘要
保留:   [Msg6, Msg7, Msg8, Msg9, Msg10] → 不处理

         ↓ 合并结果

[摘要消息, Msg6, Msg7, Msg8, Msg9, Msg10]  → 6 条消息
```

### 设计原因

| 方案 | 问题 |
|------|------|
| 摘要全部 | 丢失最近对话的完整上下文，AI 可能无法理解当前话题 |
| 摘要前半 | 保留最近对话的完整性，压缩早期对话为摘要 ✓ |

**最近的消息最重要**，所以保留后半部分不处理。

### 摘要模型策略

系统支持三种摘要模型策略，可通过配置选择：

| 策略 | 配置值 | 说明 | 适用场景 |
|------|--------|------|----------|
| **当前模型** | `current` (默认) | 使用当前对话模型进行摘要 | 追求摘要质量，成本不敏感 |
| **智能映射** | `smart` | 自动映射到轻量级模型 | 追求低成本，如 gpt-4o-mini、glm-4-flash |
| **自定义** | `custom` | 使用指定的自定义模型 | 有特定需求，需配合 `summarizer-custom-model` 配置 |

```yaml
chat:
  memory:
    # 摘要模型策略: current(当前模型) / smart(智能映射) / custom(自定义)
    summarizer-strategy: current

    # 自定义摘要模型（仅当 summarizer-strategy=custom 时生效）
    # summarizer-custom-model: gpt-4o-mini
```

### 智能映射规则

当选择 `smart` 策略时，系统自动根据主模型选择合适的轻量级摘要模型：

| 主模型 | 摘要模型 | 说明 |
|--------|----------|------|
| glm-5 | glm-5-flash | 智谱最新轻量版 |
| glm-4.5-air | glm-4.5-air | 保持原模型 |
| glm-4 | glm-4-flash | 智谱便宜版本 |
| gpt-4 | gpt-4o-mini | OpenAI 最便宜 |
| claude | claude-3-5-haiku | Anthropic 轻量版 |
| deepseek | deepseek-chat | 本身便宜 |
| qwen | qwen-turbo | 阿里轻量版 |
| doubao | doubao-1.5-lite | 字节轻量版 |

## 工作流程示例

### 场景 1: 正常对话（Token 未达到阈值）

```
1. 用户发送消息
   ↓
2. 从数据库查询历史消息
   ↓
3. TokenCounter 计算当前 Token 数
   ↓
4. Token 数 < 有效上限 × 比例阈值
   ↓
5. 直接返回所有消息，不做处理
```

### 场景 2: Token 达到比例阈值（触发摘要）

```
1. 用户发送消息，Token 累积
   ↓
2. Token 数 >= 有效上限 × 比例阈值（如 70%）
   ↓
3. SummarizationStrategy 触发
   ↓
4. 调用 LLM 生成历史对话摘要（前半部分）
   ↓
5. 替换旧消息为摘要消息
   ↓
6. 检查是否仍超限
   ↓ 是
7. TruncationStrategy 触发，执行截断
```

### 场景 3: Token 直接超限（触发截断）

```
1. 用户发送消息，Token 累积
   ↓
2. Token 数 > 有效上限
   ↓
3. 检查摘要条件（比例、消息数等）
   ↓ 不满足
4. TruncationStrategy 触发
   ↓
5. 从旧消息开始截断，直到 Token 在限制内
```

### 场景 4: 策略执行后仍超限（强制兜底）

```
1. 用户发送消息，Token 累积
   ↓
2. Token 数远超有效上限
   ↓
3. SummarizationStrategy 执行 → 仍超限
   ↓
4. TruncationStrategy 执行 → 仍超限
   ↓
5. SlidingWindowStrategy 执行 → 仍超限
   ↓
6. forceTruncate() 强制截断到限制内
   ↓
7. 返回不超限的消息
```

### 场景 5: 无策略框架（紧急截断）

```
1. 用户发送消息，Token 累积
   ↓
2. Token 数 > 有效上限
   ↓
3. 无 StrategyManager 或策略失败
   ↓
4. emergencyTruncate() 紧急截断
   ↓
5. 保留系统消息 + 最近消息
   ↓
6. 返回不超限的消息
```

## 日志输出示例

```
# 创建内存
创建 ChatMemory: strategy=hybrid, memoryId=12345
[Hybrid内存] 创建混合策略内存: maxTokens=131072, summarizeEnabled=true, summarizeTokenRatio=0.7

# 正常状态
[Token内存管理] 会话=12345, 消息数=15, 当前Token=8500, Token上限=131072, 预留回复空间=2000

# Token 达到比例阈值，触发摘要
[策略管理器] 执行策略: summarization
[摘要策略] 开始摘要: 原消息数=25, 待摘要=12, 保留=13
[摘要策略] 生成摘要成功: 本次对话主要讨论了用户的项目需求...
[摘要策略] 完成: 原消息数=25 → 新消息数=14, Token: 90000 → 55000
[策略管理器] 策略 summarization 执行成功, Token: 90000 → 55000
[策略管理器] 压缩完成，已达到目标范围

# Token 超限，触发截断
[策略管理器] 执行策略: truncation
[截断策略] 完成: 原消息数=30 → 截断后=18, Token: 130000 → 124500

# 滑动窗口策略（保留系统消息）
[策略管理器] 执行策略: sliding-window
[滑动窗口策略] 完成: 原消息数=50 → 截断后=22, 系统消息=2, 窗口大小=20

# 策略执行后仍超限，强制截断兜底
[策略管理器] 策略执行后仍超限 (5500 > 4000)，执行强制截断兜底
[强制截断] 完成: 原消息数=25 → 截断后=15, 系统消息=2

# 无策略框架，紧急截断
[Token内存管理] 无策略框架且超限，执行紧急截断
[紧急截断] 完成: 原消息数=30 → 截断后=18, 系统消息=2
```

## 性能考虑

### 当前方案

| 操作 | 说明 |
|------|------|
| **读取** | 每次对话都从数据库查询全部历史消息 |
| **写入** | 每条新消息单独 INSERT 到数据库 |
| **摘要** | 只在内存中处理，不更新数据库 |
| **截断** | 只在内存中处理，不更新数据库 |

### 优化建议

| 场景 | 建议 |
|------|------|
| 当前规模（用户少） | 现有方案够用 |
| 中等规模 | 加 Redis 缓存 |
| 大规模 | Redis + 分页查询 + 异步摘要 |

## 安全考虑

1. **Token 估算**: 使用中英文混合估算，不依赖外部 Tokenizer，性能高
2. **摘要开销**: 摘要会增加 LLM API 调用，建议仅在长对话场景启用
3. **系统消息保护**: 所有策略都会保留系统消息，确保 AI 角色设定不丢失
4. **回复空间预留**: 预留 reservedForReply tokens 给 AI 回复，避免上下文占满
5. **未知模型处理**: 回退到固定消息数量策略，避免使用默认 4096 导致截断过多
6. **数据完整性**: 摘要和截断不修改数据库，保留完整历史记录
7. **边界保护**: effectiveMaxTokens 保证 >= 1，避免负数/零导致的计算错误
8. **多层兜底**: 策略失败时有多层降级机制，确保返回的消息永不超限
9. **null 值过滤**: 流式响应中过滤 null 值，避免拼接成无效消息内容

## 关键修复记录

### P0 级别修复

| 问题 | 修复 | 文件 |
|------|------|------|
| SlidingWindowStrategy 不保留系统消息 | 分离系统消息，保留系统消息 + 最近 N 条普通消息 | `SlidingWindowStrategy.java` |
| effectiveMaxTokens 可能为负数/零 | 添加 `Math.max(1, effective)` 边界检查 | `CompressionContext.java` |
| SlidingWindowStrategy 错误检查 Token | 移除 Token 超限检查，只检查消息数量 | `SlidingWindowStrategy.java` |

### P1 级别修复

| 问题 | 修复 | 文件 |
|------|------|------|
| 策略执行后仍超限无保障 | 添加 `forceTruncate()` 强制截断兜底 | `CompressionStrategyManager.java` |
| 降级机制不完善 | 添加 `emergencyTruncate()` 紧急截断 | `TokenBasedChatMemory.java` |

## 模型 Token 限制参考

| 模型系列 | 代表模型 | Token 限制 |
|----------|----------|------------|
| GPT-4.1 | gpt-4.1, gpt-4.1-mini | 1,047,576 |
| GPT-4o | gpt-4o, gpt-4o-mini | 128,000 |
| o 系列 | o1, o3, o4-mini | 200,000 |
| DeepSeek | deepseek-chat, deepseek-reasoner | 64,000 |
| GLM-5 | glm-5, glm-5.1 | 128,000 |
| GLM-4-long | glm-4-long | 1,024,000 |
| Qwen3 | qwen3, qwen3-235b | 128,000 |
| Qwen-long | qwen-long | 1,000,000 |
| Claude 4 | claude-4-opus, claude-4-sonnet | 200,000 |
| Claude 3.5 | claude-3.5-sonnet | 200,000 |
| Gemini 2.5 | gemini-2.5-pro, gemini-2.5-flash | 1,048,576 |
| Doubao 1.5 | doubao-1.5-pro, doubao-1.5-thinking | 256,000 |
| Grok 3 | grok-3, grok-3-mini | 131,072 |
| Llama 3.1/3.2 | llama3.1, llama3.2 | 131,072 |

## 扩展指南

### 添加新模型

在 `ModelTokenLimits.java` 的 `TOKEN_LIMITS` Map 中添加：

```java
TOKEN_LIMITS.put("new-model-name", 64000);
```

### 添加新的压缩策略

1. 实现 `MemoryCompressionStrategy` 接口：

```java
@Component
public class MyStrategy implements MemoryCompressionStrategy {
    @Override
    public String getName() { return "my-strategy"; }

    @Override
    public int getPriority() { return 80; }  // 介于摘要和截断之间

    @Override
    public boolean needsCompression(CompressionContext context) {
        // 自定义触发条件
        return true;
    }

    @Override
    public CompressionResult compress(CompressionContext context) {
        // 自定义压缩逻辑
        return CompressionResult.success(...);
    }
}
```

2. Spring 会自动注入到 `CompressionStrategyManager`

### 自定义摘要模型映射

在 `ChatServiceFacade.getSmartSummarizerModel()` 中添加：

```java
if (model.contains("new-model")) return "new-model-lite";
```

### 自定义 Token 计数

实现 `TokenCounter` 接口或继承现有类，重写 `countTokens()` 方法。
