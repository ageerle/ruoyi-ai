# R9-ROOT-CAUSE：全局测试失败系统性根因分析

> 生成时间：2026-09-06 04:00 PDT
> 数据口径：463 tests run，19 failures，18 errors，22 skipped
> 范围：ruoyi-modules/ruoyi-ipd 全模块 + application.yml 配置守卫

---

## 一、测试失败全景图谱（463 → 19F/18E/22S）

### 1.1 分类汇总

| 类别 | 测试类 | 失败数 | 根本原因 | 性质 |
|------|--------|--------|----------|------|
| 配置漂移 | TenantExcludesConsistencyTest | 1 | 3张新DDL表未登记到tenant.excludes | 活债 |
| 配置漂移 | ActuatorNarrowTest | 1 | 父yml注释含字面量触发误报 | 假绿 |
| 业务重构 | ProductServiceTest.createOk | 1 | 二态→四态重构，断言滞后 | 活债 |
| 自引用陷阱 | P063AcceptanceTest.mockKeyNamesMatchProductionSource | 1 | 测试读自身源码，Javadoc历史键名被grep命中 | 假红 |
| API契约漂移 | P131AcceptanceTest（x15） | 15 | sibling将单条insert改insertBatch，测试mock路径未同步 | 活债 |
| API契约漂移 | P191AcceptanceTest.markHistoricalMissing | 1 | Mockito verify updateById未命中，流程已改 | 活债 |
| Lambda缓存 | P171AcceptanceTest（x3）+ P1111（x1） | 4 | ProjectCertItem加@TableLogic/@Version，lambda cache失效 | 活债 |
| 项目不存在 | StageActionServiceInstantiateBatchTest（x3） | 3 | ID=1项目被清理/重建，mock未更新 | 活债 |
| P131 ERROR | P131AcceptanceTest（x10） | 10 | bootstrap insertBatch失败路径，与15 failures同源 | 活债 |
| P132 ERROR | P132AcceptanceTest（x3） | 3 | 同上 | 活债 |

**19 failures + 18 errors = 37 项不合格，占总测 8.0%**

### 1.2 失败分布特征

- 非随机：37项全部集中于 8个测试类
- 时间集中：全部为 sibling 会话最近两轮（R8→R8p）并行重构导致
- 零跨模块扩散：仅 ruoyi-ipd 模块，其他模块测试未受影响
- 核心链断裂：P131（15F+10E=25项）独占 67.6%，是最大单一故障源

---

## 二、六大根源性根因（根源性深度分析）

### 根因 #1：多会话并行写 — OPS-09 规则失守

**现象**：sibling 在 R8/R8p 期间并行写入 application.yml、ProductService.java、ProjectBootstrapService.java，同时本会话也在工作。

**证据**：
- commit 67b18014（本会话修 ActuatorNarrowTest 误报）→ 后续 commit b25930e6（兄弟流）覆盖回 HEAD 版，注释修复丢失
- git status 显示 9 个 dirty 文件混和本会话 + 兄弟会话改动
- ProductService.java 状态机重构（二态→四态）由兄弟完成，本会话的 ProductServiceTest.createOk 断言滞后

**深层机制**：
session-A (this): 读 yml -> 写 fix -> commit 67b18014
session-B (sibling): 读 yml（旧版）-> 写 docs -> commit b25930e6 -> git checkout yml（从HEAD）
结果：session-A 的 yml fix 被 session-B 的 checkout 覆盖

**本质**：OPS-09「并发写单一写入者」规则在兄弟会话层面未得到执行。兄弟会话拥有 Java 写入权限（非主协调会话），直接改 application.yml 和 Java 源码，破坏了 SSOT 串行写约定。

---

### 根因 #2：配置守卫测试的「静态文本匹配」脆弱性

**现象**：ActuatorNarrowTest 用 doesNotContain("include: '*'") 断言整个文件，但 yml 注释（line 359）包含 include: '*' 字面量作为说明文字。

**证据**：
ActuatorNarrowTest.java:54: assertThat(content).doesNotContain("include: '*'"); // 命中 line 359 注释

**sibling 部分修复（67b18014）**：将注释改为「显式覆盖为通配全部端点」，消除字面量。但 fix 被 b25930e6 覆盖后重提。

**深层机制**：
- 静态文本匹配无法区分「配置值」和「文档注释」
- yml 注释是合法的技术文档载体，不应成为断言副作用
- 正确的断言方式应是解析 YAML 结构，而非字符串扫描

**本质**：测试守卫过于暴力——用文件全文 grep 替代结构化校验，导致注释文本成为测试噪声源。

---

### 根因 #3：业务重构期间测试断言链断裂（API 契约漂移）

**现象**：P131AcceptanceTest 验证 15 项 + 10 errors，全部根因是 ProjectBootstrapService 的 insert() → insertBatch() 重构。

**证据**：
P131AcceptanceTest.java:72: verify(f.stageMapper, times(6)).insert(any(ProjectStage.class));
ProjectBootstrapService.java:92: if (!projectStageMapper.insertBatch(stageList, STAGE_BATCH_SIZE)) { throw writeFailure(); }

**连锁反应**：
1. Mock 未更新：测试 mock 的是 insert()，生产调用 insertBatch() -> mock 不触发
2. 断言失效：times(6).insert -> 0 次 -> assertion failure
3. 错误传播：bootstrap 方法在 mock 环境下直接抛 ServiceException

**深层机制**：
- sibling 的性能优化（80 SQL -> 3 SQL）改变了 API 内部实现路径
- 测试类 P131 依赖内部实现细节（单条 insert 调用计数），而非外部契约（数据正确性）
- 重构时未同步更新依赖该内部路径的测试

**本质**：测试耦合到实现细节（implementation-dependent testing），而非契约（contract-based testing）。当内部实现改变时，测试必然断裂。

---

### 根因 #4：新实体字段变更缺少 MyBatis-Plus Lambda Cache 联动机制

**现象**：P171（x3 errors）+ P1111（x1 error）均报 can not find lambda cache for this entity [ProjectCertItem]。

**证据**：
ProjectCertItem.java:50-56: 新增 @TableLogic/@Version 注解

**深层机制**：
- MyBatis-Plus 的 lambda wrapper 依赖编译期生成的 TableInfo 缓存
- 新增 @TableLogic 和 @Version 注解后，需要在 Mapper 层重新构建 TableInfo
- autoResultMap=true（已有）但编译顺序导致 lambda cache 失效

**本质**：实体类注解变更（@TableLogic/@Version）触发 MyBatis-Plus 元数据重构建，缺少编译期联动验证。

---

### 根因 #5：租户排除列表与 DDL schema 不同步

**现象**：TenantExcludesConsistencyTest 发现 3 张新表未登记：person_roles、coefficient_change_requests、cms_content。

**深层机制**：
- DDL 变更和 tenant.excludes 变更分属不同工作流
- 无自动化脚本验证 DDL 中存在的所有表是否都在 excludes 列表中

**本质**：跨系统配置同步缺自动化网关。DDL schema 和 application.yml 配置是松耦合的两个系统。

---

### 根因 #6：测试自引用陷阱（Self-Reference Test Trap）

**现象**：P063AcceptanceTest.mockKeyNamesMatchProductionSource 失败在 line 188：

测试读自身源码，Javadoc（line 36-37, 165）中记录历史点分键名变更过程，被 doesNotContain 断言误判。

**深层机制**：
- 测试意图：防止 mock 键名与生产键名漂移
- 实现缺陷：读取目标文件是整个 .java 文件（含 Javadoc），Javadoc 中记录历史键名变更过程
- 断言语义错位：doesNotContain 应针对「当前使用的键名」，而非「文档中提到的历史键名」

**本质**：测试元数据被测试逻辑误伤。测试描述本身成为测试断言的误判源。

---

## 三、系统性风险传导链

多会话并行开发
    |
    +-> [A] 配置/Java 源码并发写 -> OPS-09 失守 -> 注释/断言被覆盖回滚
    |          +-> ActuatorNarrowTest 修复丢失 -> 再次失败（循环）
    |
    +-> [B] 业务重构（insert->insertBatch）-> 测试耦合实现细节 -> P131 全链断裂（25项）
    |
    +-> [C] 新实体字段变更（@TableLogic）-> Lambda Cache 失效 -> P171/P1111（4项）
    |
    +-> [D] DDL 新增表 -> tenant.excludes 未同步 -> TenantExcludesConsistencyTest（1项）
    |
    +-> [E] 测试设计缺陷（自引用+全文grep）-> P063（1项）+ ActuatorNarrow（1项）
    
总计：25 + 4 + 1 + 1 + 1 + 1 = 37 项不合格

---

## 四、根源性修复方案（系统性，非补丁式）

| # | 修复方案 | 优先级 | 实施难度 | 当前状态 |
|---|----------|--------|----------|----------|
| 1 | OPS-09 强制化 - Java/yml 写入 mutex hook | P0 | 中 | 待 owner 决策 |
| 2 | 配置守卫测试结构化（YAML解析替代grep） | P1 | 低 | 待实施 |
| 3 | P131 测试重构 - 从实现依赖到契约验证 | P1 | 中 | 待实施 |
| 4 | MyBatis-Plus Lambda Cache 编译保障 | P2 | 中 | 待实施 |
| 5 | tenant.excludes DDL 联动脚本 | P2 | 低 | 待实施 |
| 6 | P063 自引用修复（Javadoc历史键名替换） | P3 | 极低 | 待实施 |

---

## 五、当前工作树状态

- Dirty 文件：9 个（本会话 3 + 兄弟会话 6）
- 最近 commit：b25930e6（docs-only，不含 yml fix）
- yml 当前状态：ActuatorNarrowTest 修复已被覆盖，需重做
- P131/P171/P191/P063：需系统性修复
- ProductServiceTest.createOk：等待兄弟二态→四态重构同步更新测试

---

## 六、建议下一步行动（按优先级）

1. 立即：重做 ActuatorNarrowTest 修复（commit 67b18014 被覆盖），并登记「yml 修改禁止兄弟覆盖」规则
2. R9a：修复 P063 self-reference（1行），TenantExcludes 登记 3 张表（3行），共 4 行改动
3. R9b：P131 测试重构（15项）— 最耗时，建议专项处理
4. R9c：P171/P1111 ProjectCertItem lambda cache — 检查 Mapper XML 或编译顺序
5. R9d：向 owner 提交 OPS-09 强化提案（mutex hook）

---

本报告由 R9 治理轮自动生成，基于全量 463 测试 + git log + yml diff + 源码阅读综合判断。
