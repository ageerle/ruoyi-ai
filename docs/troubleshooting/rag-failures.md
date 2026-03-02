<a id="top"></a>

# RAG 常见故障排查（16 问题清单）

当知识库已经接入，系统也能正常回答，但结果仍然出现命中错误、引用旧内容、推理漂移、跨轮次失忆，或部署后表面可用但实际异常时，最常见的问题不是“模型不行”，而是**不同层的故障被混在一起处理**。

这份页面不重新发明一套新方法。  
它直接使用一份固定的 **16 问题清单** 作为排查主轴，让你先把问题标到正确的 **No.X**，再决定下一步查哪里、改哪里，而不是一次性乱改检索、模型、切块、会话和部署配置。

这份清单的核心目的只有一个：

**先把问题放进正确的故障域，再做修复。**

快速导航：  
[这页怎么用](#how-to-use) | [标签说明](#legend) | [常见症状入口](#symptoms) | [16 问题清单](#map16) | [按层排查](#by-layer) |

---

<a id="how-to-use"></a>

## 一、这页怎么用

这不是一篇“从头到尾照着做”的传统教程。  
它更像一张固定的 RAG 故障地图，作用是先帮助你**判断故障属于哪一种类型**。

建议按下面顺序使用：

### 1. 先看现象，不要先改配置

先回答两个问题：

1. 你看到的故障，最像哪一种症状
2. 这个故障更像发生在输入检索层、推理层、状态层，还是部署层

在还没判断层级之前，不要先一起改这些东西：

- 检索条数
- 切块大小
- 会话配置
- 模型参数
- 部署顺序
- 依赖服务

如果先全部一起动，问题通常只会更难定位。

### 2. 先给问题打上 No.X 标签

这份页面最重要的动作，不是“立刻修好”，而是先做一件小事：

**给当前问题贴上最接近的 No.X。**

例如：

- 检索结果看起来相似，但其实答非所问，先看 `No.1` 或 `No.5`
- chunk 是对的，但结论还是错，先看 `No.2`
- 系统回答很自信，但没有根据，先看 `No.4`
- 刚部署完就炸，先看 `No.14` 到 `No.16`

### 3. 一次只排一个故障域

同一个表面现象，背后可能是不同层的问题。  
例如“答案不对”既可能是：

- `No.1` 检索漂移
- `No.2` 理解塌陷
- `No.4` 自信乱答
- `No.8` 根本看不到错误路径

所以这张表的用法不是“多选全改”，而是：

**先挑最接近的一项，优先验证这一项是否成立。**

[返回顶部](#top) | [下一节：标签说明](#legend)

---

<a id="legend"></a>

## 二、标签说明

这份 16 问题清单本身已经带有 layer / tag 结构。  
这些标签不是装饰，而是用来帮助你快速判断故障发生在哪一层。

### 1. layer 标签

- `[IN]`：Input & Retrieval  
  输入、切块、召回、语义匹配、可见性问题

- `[RE]`：Reasoning & Planning  
  理解、推理、归纳、逻辑链、抽象处理问题

- `[ST]`：State & Context  
  会话、记忆、上下文连续性、多代理状态问题

- `[OP]`：Infra & Deployment  
  启动顺序、依赖就绪、部署锁死、预部署状态问题

### 2. `{OBS}` 标签

带 `{OBS}` 的项，通常都和“**你是否看得见问题是怎么坏掉的**”有关。  
它们往往不是单纯回答错误，而是：

- 错误路径不可见
- 漂移过程不可见
- 状态熔化过程不可见
- 多代理覆盖过程不可见

所以一旦你发现“我知道结果错，但我根本看不到它是怎么错的”，通常就已经很接近 `{OBS}` 类问题了。

### 3. 为什么要保留这些标签

因为同样叫“答错了”，实际含义完全不同。

例如：

- `[IN]` 的答错，常常是**拿错材料**
- `[RE]` 的答错，常常是**拿对材料但理解错**
- `[ST]` 的答错，常常是**前文断掉、状态漂移**
- `[OP]` 的答错，常常是**系统根本没在完整状态下运行**

如果不先分层，就会掉进典型的 RAG 地狱：  
表面在改答案，实际上在盲修。

[返回顶部](#top) | [下一节：常见症状入口](#symptoms)

---

<a id="symptoms"></a>

## 三、常见症状入口

如果你现在还不知道该从哪一项开始，就先从症状入口反查。

### 1. 检索返回了错误内容，或看起来相关但其实不回答问题

这类问题最常见的是：  
“有命中，但命中的不是该用的内容。”

优先看：

- [No.1](#no1) `hallucination & chunk drift`
- [No.5](#no5) `semantic ≠ embedding`
- [No.8](#no8) `debugging is a black box`

### 2. chunk 本身是对的，但最终答案还是错的

这类问题不是简单没检索到，而是后面那层坏了。

优先看：

- [No.2](#no2) `interpretation collapse`
- [No.4](#no4) `bluffing / overconfidence`
- [No.6](#no6) `logic collapse & recovery`

### 3. 多步任务一开始正常，后面越来越偏

这类问题通常不是单点错误，而是中途漂移或熔化。

优先看：

- [No.3](#no3) `long reasoning chains`
- [No.6](#no6) `logic collapse & recovery`
- [No.9](#no9) `entropy collapse`

### 4. 多轮对话后开始失忆，跨轮次接不上

这类问题一般已经进入状态层。

优先看：

- [No.7](#no7) `memory breaks across sessions`
- [No.9](#no9) `entropy collapse`
- [No.13](#no13) `multi-agent chaos`

### 5. 遇到抽象、逻辑、规则、符号关系就崩

这类问题通常不是检索空，而是推理结构扛不住。

优先看：

- [No.11](#no11) `symbolic collapse`
- [No.12](#no12) `philosophical recursion`

### 6. 你根本不知道错在哪一层，只知道结果不对

这类问题先不要乱调参数。  
先解决“不可见”的问题。

优先看：

- [No.8](#no8) `debugging is a black box`

### 7. 刚部署完最容易炸，首轮调用异常，重启后偶尔恢复

这类问题通常不在答案逻辑，而在部署状态。

优先看：

- [No.14](#no14) `bootstrap ordering`
- [No.15](#no15) `deployment deadlock`
- [No.16](#no16) `pre-deploy collapse`

[返回顶部](#top) | [下一节：16 问题清单](#map16)

---

<a id="map16"></a>

## 四、16 问题清单（固定主表）

下面这 16 项按固定顺序使用。  
不要先重组，不要先混类，先判断最接近哪一个 **No.X**。

| # | problem domain (with layer/tags) | what breaks |
|---|---|---|
| <a id="no1"></a> 1 | `[IN] hallucination & chunk drift {OBS}` | retrieval returns wrong/irrelevant content |
| <a id="no2"></a> 2 | `[RE] interpretation collapse` | chunk is right, logic is wrong |
| <a id="no3"></a> 3 | `[RE] long reasoning chains {OBS}` | drifts across multi-step tasks |
| <a id="no4"></a> 4 | `[RE] bluffing / overconfidence` | confident but unfounded answers |
| <a id="no5"></a> 5 | `[IN] semantic ≠ embedding {OBS}` | cosine match ≠ true meaning |
| <a id="no6"></a> 6 | `[RE] logic collapse & recovery {OBS}` | dead-ends, needs controlled reset |
| <a id="no7"></a> 7 | `[ST] memory breaks across sessions` | lost threads, no continuity |
| <a id="no8"></a> 8 | `[IN] debugging is a black box {OBS}` | no visibility into failure path |
| <a id="no9"></a> 9 | `[ST] entropy collapse` | attention melts, incoherent output |
| <a id="no10"></a> 10 | `[RE] creative freeze` | flat, literal outputs |
| <a id="no11"></a> 11 | `[RE] symbolic collapse` | abstract/logical prompts break |
| <a id="no12"></a> 12 | `[RE] philosophical recursion` | self-reference loops, paradox traps |
| <a id="no13"></a> 13 | `[ST] multi-agent chaos {OBS}` | agents overwrite or misalign logic |
| <a id="no14"></a> 14 | `[OP] bootstrap ordering` | services fire before deps ready |
| <a id="no15"></a> 15 | `[OP] deployment deadlock` | circular waits in infra |
| <a id="no16"></a> 16 | `[OP] pre-deploy collapse {OBS}` | version skew / missing secret on first call |

这张表是主表。  
如果你时间很少，只做一件事也行：

**先从这 16 项里选出最接近的一项。**

[返回顶部](#top) | [下一节：按层排查](#by-layer)

---

<a id="by-layer"></a>

## 五、按层排查：不要改错层

这一节不重写 16 项，只是告诉你：  
当你已经选到某个 No.X 时，第一眼应该优先查哪一层。

### A. `[IN]` 层：先确认你拿到的是不是对的材料

对应编号：

- [No.1](#no1)
- [No.5](#no5)
- [No.8](#no8)

这层最常见的误判是：

“我以为系统理解错了，其实它一开始就拿错了东西。”

如果你命中了弱相关片段、表面相似文本、错误 chunk，后面推理再强也没用。  
所以 `[IN]` 层优先看的是：

1. 原始召回内容到底是什么
2. 命中的片段是否只是“相似”，而不是“正确”
3. 你是否能看到检索过程，还是整个过程像黑箱

这层如果没先排好，后面的推理诊断通常会失真。

### B. `[RE]` 层：材料可能是对的，但系统用错了

对应编号：

- [No.2](#no2)
- [No.3](#no3)
- [No.4](#no4)
- [No.6](#no6)
- [No.10](#no10)
- [No.11](#no11)
- [No.12](#no12)

这层最常见的误判是：

“我以为是检索坏了，其实是后面理解、归纳、逻辑链坏了。”

例如：

- chunk 是对的，但结论错了 → 常见是 `No.2`
- 多步任务中途开始偏 → 常见是 `No.3`
- 回答很笃定，但完全站不住 → 常见是 `No.4`
- 遇到抽象规则就崩 → 常见是 `No.11`
- 陷入循环解释 → 常见是 `No.12`

如果 `[IN]` 层已经基本没问题，答案还是不对，就应该优先回到 `[RE]` 层判断是哪一种塌陷。

### C. `[ST]` 层：单轮正常，不代表状态层正常

对应编号：

- [No.7](#no7)
- [No.9](#no9)
- [No.13](#no13)

这层最常见的误判是：

“单轮看起来还行，所以我以为系统没问题。”

其实很多 RAG 地狱不是单轮错误，而是：

- 多轮之后前文断掉
- 上下文越来越乱
- 多角色、多代理之间互相覆盖

如果你发现：

- 第一轮没事，后面越来越歪
- 切换角色后前面的约束消失
- 多个步骤之间状态彼此污染

那就不要再只盯着检索条数了，应该直接回到 `[ST]` 层看 `No.7 / No.9 / No.13`。

### D. `[OP]` 层：别把部署问题误诊成回答问题

对应编号：

- [No.14](#no14)
- [No.15](#no15)
- [No.16](#no16)

这层最常见的误判是：

“答案不稳定，所以我先去调模型或检索。”

但如果系统根本没有在完整状态下启动，所有上层表现都会像鬼打墙。  
尤其是这些情况：

- 依赖还没就绪，服务先起了 → `No.14`
- 多个组件互相等待，长期半可用 → `No.15`
- 首次调用就因为版本、密钥、环境没对齐而塌陷 → `No.16`

只要你看到“刚部署最容易出事”“首轮异常最严重”“重启后暂时恢复”，就要优先怀疑 `[OP]` 层，而不是先改 prompt 或参数。

[返回顶部](#top) |

---

<a id="issue-report"></a>


## 六、快速返回

[返回顶部](#top) | [这页怎么用](#how-to-use) | [标签说明](#legend) | [常见症状入口](#symptoms) | [16 问题清单](#map16) | [按层排查](#by-layer) 

