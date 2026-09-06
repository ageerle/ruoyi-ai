# 审计 hash 链顶层架构设计稿 — DEF-9 / DEF-6 / QA-05-P2 同根收口方案

> **性质**：只读设计稿，零代码改动；为 DEF-9 / DEF-6 / QA-05-P2 三张同根卡出顶层方案矩阵，跨 commit 互锁 + owner 矩阵 + 用户决策项。
> **作者**：审计链顶层架构师（sub-agent, zero-code mode）
> **日期**：2026-09-05
> **凭据**：本地配置 `.codex/ipd-dev/config/xxx` 引用，明文口令不入文
> **范围**：DEF-4（已闭环）/ DEF-6（建卡待裁）/ DEF-9（建卡待裁）/ QA-05-P2（U1 todo）四张卡同源——审计 hash 链**写协议 + 验协议 + 持久层列型 + 串行化边界**四件套同时破

---

## 1. 现状拓扑

### 1.1 已修复（DEF-4 闭环，commit 7cae2138 / c23fd1f2 / 5b95a9d0 / 9f1fb63b）

| 修复层 | commit | 代码 / DDL 锚点 | 状态 |
|---|---|---|---|
| **第1层：毫秒对称** | 5b95a9d0 | `AuditLogService.java:184-186` `secondMillis()` 写库前毫秒归零 + `AuditLogService.java:67-70` 入参归零调用 | 闭环 |
| **第2层：升序锚定** | 7cae2138 | `AuditLogService.java:193-195` `orderBySeqAsc()` + `:102` 锚点 = `all.get(0).getPrevHash()` | 闭环 |
| **第3层：竞态防护** | c23fd1f2 | `AuditLogService.java:74-91` 去 FOR UPDATE 改 `DuplicateKeyException` 3 次重试（DB 最小权限适配） | **结构死代码**（见 §2 真因 5） |

### 1.2 残留根因（DEF-6 + DEF-9 + QA-05-P2 同源）

| 缺陷 | 代码 / DDL 锚点 | 现象 |
|---|---|---|
| **DEF-9 第①面：seq 由 DB AUTO_INCREMENT 赋值，应用从不写** | `audit_logs` DDL 第 490 行 `seq bigint not null auto_increment` + `AuditLog.java:21` `@TableField(value = "seq", insertStrategy = FieldStrategy.NEVER)` | 并发写者全部读到同一 last 行 → 算出相同 `prev_hash` + 相同 canonical seq → DB 给互不相同的 seq → 仅第一行自洽、其余全部 prev_hash 链接错 + curr_hash 内嵌 seq 与实际不符 |
| **DEF-9 第②面：uk_audit_seq 永不冲突** | `audit_logs` DDL 第 507 行 `unique key uk_audit_seq (seq)` + `AuditLog.java:21` NEVER 注解 | 应用从不写 seq → uk 约束从不被 INSERT 触发 → `catch (DuplicateKeyException)` 分支永不执行 → **DEF-4 第3修复「多实例共库靠 uk 冲突自愈重试」是结构性死代码** |
| **DEF-9 第③面：verifyChain 连续性判据 + 空洞永久 BROKEN** | `AuditLogService.java:108` `!expectSeq.equals(log.getSeq())` + `:112` `expectSeq = log.getSeq() + 1` | InnoDB AUTO_INCREMENT 在 DELETE / 事务回滚后**不回填** → 任何空洞让 verifyChain 永久 BROKEN；`rebuildChain()` 只重算 prev/curr 两列、**不治缺行** |
| **DEF-6：MySQL json 列规范化渲染** | `audit_logs` DDL 第 497-498 行 `before_data json null / after_data json null` + `AuditHashChain.java:124-130` payloadV1 | MySQL 读回时键按 UTF-8 字节长度→字典序重排 + 成员插 `", "` + `1e3→1000.0` → 与 Jackson 紧凑串永不相等 → 带载荷审计行写完即被 verifyChain 判裂（实证 seq 466/485/502） |
| **QA-05-P2：写串行化** | `AuditLogService.java:171-174` `selectLast()` + `:75-90` 重试循环 | 100 并发写审计 → 每条 `ORDER BY seq DESC LIMIT 1` 全表倒序扫描 50 万行 → 事务串行化放大持池时间；实测 100 并发写 P95=30s，拐点 10~20 与池上限 10 对应 |

### 1.3 DDL 三铁证（P0-9.1 治理轮取证@18:44）

ipd_dev 实测（铁证由兄弟 QA-05-P1 压测 484 处断裂转出）：
- **铁证1**：多行共享同一 prev_hash —— 16 行 seq 1022-1037、15 行 1241-1255、13 行 812-824、11 行 1081-1091、9 行 673-681、9 行 859/861-868（全为 LOGIN_FAIL）
- **铁证2**：`LAG()` 算 `link_breaks=484`、`first_break=600`、`last_break=1264`
- **铁证3**：断裂点 seq 600/601/602/603 的 `got_prev` 全为 `d873f9a57c4c`（同一值）而 `want_prev` 各不相同

空洞实证@19:07：清库后 audit_logs 重整为 seq 2..609 而 AUTO_INCREMENT 停在 1309 → P0-9.1 七跑 74/83，残留 9 项 FAIL（4 检查点各 2 条链硬门 + L7 seq 零跳号）全部归因该单一空洞，`broken=[1309]` 且该行 `no_payload=1`、`prev_hash` 与前一行 `curr_hash` 相符（`link_ok=1`），rebuildChain **无法治愈**。

---

## 2. 真因剖析（5 层同根）

| 层 | 主题 | 位置 | 后果 |
|---|---|---|---|
| **L1 协议-写** | seq 由 DB AUTO_INCREMENT 赋值，应用 NEVER 写入 | `AuditLog.java:21` + `2026-09-04-ipd-p0-tables.sql:490,507` | 应用无法控 seq，与 curr_hash 内嵌的 canonical seq 不一致——并发写者静默丢审计事件（重试耗尽即抛异常、REQUIRES_NEW 回滚）而非 seq 冲突 |
| **L2 协议-验** | verifyChain 第三条判据 `!expectSeq.equals(log.getSeq())` 要求严格连续 | `AuditLogService.java:108,112` | 任何空洞/删行/失败 INSERT → 永久 BROKEN，且 rebuildChain 不治缺行 |
| **L3 持久层列型** | json 列被 MySQL 服务端规范化渲染 | `2026-09-04-ipd-p0-tables.sql:497-498` + `AuditHashChain.java:124-130` | 带载荷行 hash 失配，AC-AUD-03 对 6 个 before/afterData 写入点失效 |
| **L4 性能边界** | selectLast `ORDER BY seq DESC LIMIT 1` 全表倒序扫描 | `AuditLogService.java:171-174` | 写事务串行化放大持池时间；QA-05 实测写 P95=30s 雪崩（池满） |
| **L5 死代码** | DEF-4 第3修复「uk_audit_seq 冲突自愈重试」因 L1 + NEVER 注解永不触发 | `AuditLogService.java:74-91` + `AuditLog.java:21` | catch 分支结构性不可达；commit c23fd1f2 的 javadoc 自承「长期方案 = QA-04 泳道 audit_log_chain_heads 原子递增（归属兄弟，本类不引用）」——**8 commits 未落地** |

**同根性**：L1 是 L5 的根因（L1 → uk 永不冲突 → 死代码）；L1 同时是 L2 的根因（L1 → InnoDB AUTO_INCREMENT 不回填 → 空洞）；L3 修复引入新护栏依赖 L4 的写路径 O(1) 化才能保持；L4 是 L5 标注的长期方案（chain_heads）落地后的天然解。四件套必须同卡处理。

---

## 3. 目标态（6 特征）

| # | 特征 | 度量 | 现状差距 |
|---|---|---|---|
| **G1** | **多实例并发写安全** | ≥4 实例并发 append，verifyChain broken=[] | 当前 484 处断裂 / 单实例即坏（NEVER + 并发同 last） |
| **G2** | **json 载荷可索引** | `WHERE JSON_EXTRACT(after_data, '$.key') = ?` 命中 + 应用层 fail-fast | 当前 MySQL 规范化 → 哈希失配；DDL 改 longtext 后无索引 |
| **G3** | **hash 链断点自愈** | 空洞 / 篡改可定位 + 单一修复入口 + rebuildChain 覆盖空洞 | 当前空洞永久 BROKEN，rebuildChain 不治缺行 |
| **G4** | **写路径 O(1)** | append = 1 SQL（CAS 锚表）或 2 SQL（独立 head 事务） | 当前 2~6 SQL / append（selectLast + insert + 最多 3 重试） |
| **G5** | **完整性验证 O(n) 一次扫** | verifyChain 一次升序遍历出两类：HASH_BROKEN + GAP | 当前把 GAP 与 HASH 混同报 broken，无法区分篡改 vs 缺行 |
| **G6** | **防空洞** | 任何失败 INSERT 不留空洞；或空洞单独报 GAP 类 | 当前 InnoDB AUTO_INCREMENT 不回填 + seq 连续性判据 → 必坏 |

---

## 4. 方案矩阵（≥3 套列对比）

### 方案 A：DB 自管 seq（去 AUTO_INCREMENT + 单行原子 chain_heads）+ json 列规范化护栏

| 维度 | 内容 |
|---|---|
| **核心改动** | ① `audit_logs.seq` 列改 `bigint not null` 去掉 AUTO_INCREMENT；② 新增 `audit_log_chain_heads` 表（单行 `key='audit'` 持 `seq` + `curr_hash`）；③ append 改为 `UPDATE chain_heads SET seq=seq+1, curr_hash=? WHERE key='audit'`（受更新行数=1 则成功）→ 再用新 seq 算 prev_hash + insert audit_logs；④ json 列保留（不加 longtext），仅在 append 入口加应用层 JSON 校验护栏（保留 G-02 DB 层 fail-fast）；⑤ verifyChain 三判据拆分为 HASH / GAP 两类返回 |
| **改动文件** | `2026-09-04-ipd-p0-tables.sql`（DDL 三处）、新增 `2026-09-XX-ipd-audit-chain-heads.sql`、`AuditLog.java:21` 去 NEVER、`AuditLogService.java:75-91` 重写为 chain_heads CAS、`AuditLogController.java:73-80` verify 拆两类 |
| **代价** | DDL ALTER 需停写窗口（去 AUTO_INCREMENT 必须重建表）；新表加迁移 + 登记 `tenant.excludes`；verifyChain 行为变更（破坏 P0-9.1 的 L7「seq 零跳号」断言，需同步改测试） |
| **风险** | 去 AUTO_INCREMENT 迁移期如失败 → 需手工回填；chain_heads 单行 = 单点锁（写热点），但原子 UPDATE 等同 SELECT...FOR UPDATE + UPDATE，单行无锁竞争 |
| **G1~G6 覆盖** | G1 ✓ / G2 ✓ / G3 部分（链头原子 → 写不空洞，但历史空洞仍需手动处理）/ G4 ✓ / G5 ✓ / G6 ✓ |
| **冻结协议变更** | 是（verifyChain 返回结构 + seq 来源） |

### 方案 B：应用层 Snowflake ID + crc32 简 hash + 列存 before/after 分表

| 维度 | 内容 |
|---|---|
| **核心改动** | ① seq 改用 Snowflake ID（41 位时间戳 + 10 位机器 + 12 位序列）应用层生成；② curr_hash 改用 crc32（速度优先）替代 SHA-256；③ before_data/after_data 按 entity_type 拆 6 张子表（audit_log_payload_project / _person / _gate 等）；④ append = Snowflake 生成 + crc32(prev + canonical) + insert 主表 + insert 子表（双写）；⑤ verifyChain 仅校验主表链 + 子表数量一致 |
| **改动文件** | 6 张子表 DDL（新增）、`AuditLogService.java:61` 改用 Snowflake 生成器、`AuditHashChain.java:105-117` 换 crc32、所有写审计点（6 处）+ 读侧（listByOperatorIds / export）改双表 join |
| **代价** | 全栈大改（写点 6 处 × 子表路由）；hash 算法变更 → 历史 v1 行**全部失效**，必须一次性全量 rebuildChain；前端读取需 join 子表 |
| **风险** | Snowflake 时钟回拨（NTP 跳变）→ ID 冲突；crc32 碰撞概率 1/2^32 虽极低但**不符合 G-02 防篡改语义**（v1 SHA-256 已合规）；子表双写一致性 |
| **G1~G6 覆盖** | G1 ✓ / G2 ✗（拆表不解决索引）/ G3 ✗ / G4 部分 / G5 ✗ / G6 部分 |
| **冻结协议变更** | **是（破坏性）** + hash 算法换 → 历史链失效 |

### 方案 C：双列存储（hash 专用列 + json payload 独立列）+ chain_heads 锚表（推荐基线）

| 维度 | 内容 |
|---|---|
| **核心改动** | ① 保留 `audit_logs` 主表，去 AUTO_INCREMENT（seq 改 bigint not null DEFAULT 0）；② 新增 `audit_log_chain_heads`（同 A）；③ append 走 chain_heads 原子递增 seq + prev_hash + curr_hash（同 A）；④ **新增 `audit_log_payloads` 表**（id = seq, before_text longtext, after_text longtext, payload_version int, created_at），应用层 hash 算的是 payload 字节（`AuditHashChain.payloadV1` 改为 `nvl(beforeText) + nvl(afterText)`），主表保留 `before_hash char(64)` + `after_hash char(64)` 作指针；⑤ json 列**从主表移除**（G-02 改为应用层 requireJson + `audit_log_payloads.payload_version` 显式版本）；⑥ verifyChain 三判据拆 HASH / GAP 两类 |
| **改动文件** | `2026-09-04-ipd-p0-tables.sql`（去 AUTO_INCREMENT + 新增 payloads 表）、新增 `2026-09-XX-ipd-audit-chain-heads.sql` 与 `2026-09-XX-ipd-audit-payloads.sql`、`AuditLog.java:21` 去 NEVER + 新增 `before_hash/after_hash` 字段、`AuditLogService.java:60-92` 重写（chain_heads CAS + payloads 双写）、`AuditHashChain.java:124-130` payloadV1 改字节级 + 加版本前缀、`AuditEventData.java:45-56` 升级为强校验（必填非空时拒绝非 JSON） |
| **代价** | DDL 改动大（去 AUTO_INCREMENT 必须重建表 + 新增 2 张表）；所有写审计点（6 处）+ 读侧（listByOperatorIds / export / verify）改双表 join；verifyChain 行为变更（破坏 P0-9.1 的 L7 断言） |
| **风险** | DDL 迁移窗口长（必须停写）；payloads 表行数 = 主表行数，存储 2×；chain_heads 仍单点但可接受 |
| **G1~G6 覆盖** | G1 ✓ / G2 ✓（payloads 表可加 `(payload_version, created_at)` 索引 + GIN 模拟）/ G3 ✓（GAP 与 HASH 分列 + payloads 表可独立 rebuild）/ G4 ✓ / G5 ✓ / G6 ✓ |
| **冻结协议变更** | 是（verifyChain 返回结构 + payloads 字节级 hash） |

---

## 5. 推荐方案 + 用户需决策项

### 5.1 推荐：**方案 C**（双列存储 + chain_heads 锚表）

**理由**：
1. **G1~G6 全覆盖**，唯一同时满足「并发安全 + 载荷可索引 + 断点自愈 + O(1) 写 + 一次扫验证 + 防空洞」6 项
2. **结构最干净**：payloads 表独立 → 读路径可选（无需载荷时单表）、历史空洞重建可独立（payloads 行存在但 hash 错 → 仅重算主表 hash）
3. **G-02 合规保留**：json 列从主表移除后，应用层 `AuditEventData.requireJson` 升级为强校验 + `payload_version` 显式版本；DEF-1 fail-fast 护栏迁移到应用层
4. **未来扩展性**：payloads 表天然支持 v2 canonical（`payload_version=2` 行用 `AuditHashChain.canonicalV2` 算），hash 协议演进无需 ALTER 主表
5. **方案 A 的妥协版**：A 方案是 C 的简化（保留主表 json 列），但 json 列被规范化是**已知根因**——再保留无意义

### 5.2 用户需决策项（4 项）

| # | 决策项 | 选项 | 推荐 | 阻塞依赖 |
|---|---|---|---|---|
| **D1** | hash 协议 v1 → v2 是否同时切？ | (a) 沿用 v1 仅拆 payloads 表 / (b) 同时切 v2 加 `payload_version` 列 / (c) 新行 v2 + 历史行 v1 双版本验 | (c) 双版本验（向后兼容，无需历史全量 rebuild） | 无 |
| **D2** | chain_heads 表用单行 vs 分片？ | (a) 单行 `key='audit'`（推荐，IPD 单企业私有部署）/ (b) 按 entity_type 分 26 行 | (a) | D3 |
| **D3** | payloads 表是否加入租户隔离？ | (a) `tenant_id` 列 + 登 tenant.excludes / (b) 单企业租户='000000'，不入隔离链 | (b) 单企业，与主表一致 | 无 |
| **D4** | 写审计 6 处双写一致性策略？ | (a) 主表 + payloads 同事务 / (b) payloads 异步落 + 失败告警 / (c) payloads 落补偿表后重放 | (a) 同事务，原子性最佳；cost 与现状一致 | 无 |

### 5.3 成本 / 风险汇总

- **开发成本**：约 5~7 人天（DDL 3 文件 + service 重写 + 6 写点改造 + 3 测 + 1 verify 升级）
- **测试成本**：≥2 轮（单测覆盖 chain_heads CAS / 契约测覆盖 HASH vs GAP 分列 / 100 并发压测复跑 QA-05-P2）
- **回退成本**：DDL 改动大，回退 = 反向迁移 payloads → json + 回填 + 重建 AUTO_INCREMENT；建议保留 7 天可回退窗口
- **跨 commit 互锁**：见 §6
- **主要风险**：chain_heads 单点 → 引入 Redis 镜像作为读缓存、写仍走 DB（与现状 `SystemConfigService.cache` 模式一致）

---

## 6. 跨 commit 互锁（同批次上线次序）

```
┌─────────────────────────────────────────────────────────────────┐
│ DDL 改动批次（同 PR 同 milestone，必须同发车）                   │
│   DDL-1: 去 audit_logs.seq AUTO_INCREMENT（必须停写窗口）       │
│   DDL-2: 新增 audit_log_chain_heads（单行 CAS 锚表）           │
│   DDL-3: 新增 audit_log_payloads（拆 payload 独立表）           │
│   DDL-4: audit_logs 去 before_data/after_data → before_hash/   │
│          after_hash char(64)（指针列）                          │
│                                                                  │
│ Java 改动批次（同 PR 同 milestone，必须同发车）                  │
│   J-1: AuditLog.java 去掉 NEVER，seq 由应用按 chain_heads 算   │
│   J-2: AuditLogService.append 重写为 chain_heads CAS            │
│   J-3: AuditLogService.verifyChain 拆 HASH/GAP 两类返回         │
│   J-4: AuditLogService.listByOperatorIds / export join payloads │
│   J-5: AuditHashChain.payloadV1 改字节级 hash                   │
│   J-6: AuditEventData.requireJson 升级为强校验（必填非空时拒绝）│
│   J-7: 6 处写审计点（AuditAttemptService / CoefficientChange /  │
│        DeletionRequest / GateElement / LaunchDateChange /       │
│        StageAction / RebuiltChain）改双写                       │
│                                                                  │
│ 测试批次                                                          │
│   T-1: AuditChainSymmetryTest 现有 7/7 + 8/8 必绿               │
│   T-2: 新增 HASH_vs_GAP 分列契约测                              │
│   T-3: 新增 chain_heads CAS 并发契约测                          │
│   T-4: 新增 payloads 双写一致性契约测                           │
│   T-5: QA-05-P2 100 并发复测，验证写 P95 < 3s                  │
└─────────────────────────────────────────────────────────────────┘
```

### 6.1 互锁矩阵（与本批同窗的兄弟流）

| 兄弟流 | 互锁点 | 互锁原因 | 互锁次序 |
|---|---|---|---|
| **DEF-4（已闭环）** | 毫秒归零 + 升序锚定 + uk 重试 | 方案 C 去 NEVER + 去 AUTO_INCREMENT → **DEF-4 第3修复必须废弃**（uk 永不冲突）→ catch (DuplicateKeyException) 整段删除 | 必须同 PR 删除，javadoc 第33行「长期方案」链接到 chain_heads 落地 |
| **DEF-6（建卡待裁）** | json → longtext + 应用层 requireJson | 方案 C 把 json 列彻底从主表移除 → `2026-09-05-ipd-audit-payload-longtext.sql` **被方案 C 的 DDL-3 覆盖**，**不必单独执行**；但 `AuditEventData.requireJson` 必须升级为强校验（必填非空时拒绝） | 同 PR 吸收，DEF-6 卡可标 done with supersede |
| **audit-payload-longtext DDL（untracked）** | before/after → longtext | 同上，被方案 C DDL-3 覆盖 | 同 PR 吸收 |
| **BCrypt cost 升 10（HIGH-1）** | `IpdMockDataInitializer.java:96` | 独立优化，与审计链无强耦合；可同 PR 上线也可分 PR | 建议同 PR 但独立 commit（不改 audit 路径） |
| **Hikari maxPoolSize=80**（P1-3 prod 修复） | `application-prod.yml:74` | 独立优化，与审计链无强耦合；与本批**强推荐同 PR 上线**——chain_heads CAS 解决 G4 O(1) 写，但池容量若仍 20，100 并发排队仍会雪崩 | **强推荐同 PR** |
| **QA-05-P2（todo）** | audit hash 链写串行化 | **本方案 C 即 QA-05-P2 的最终方案**——chain_heads 单行原子递增天然解决 ORDER BY seq DESC 串行化 | 同 PR 闭环 |
| **P0-9.1 L7「seq 零跳号」断言** | 业务链真实验收脚本第 7 检查点 | 方案 C 去 AUTO_INCREMENT → seq 由 chain_heads 原子递增，**自然零跳号**（CAS 受更新行数=1 才成功，否则回滚），P0-9.1 L7 实际更稳 | 需同步修改断言语义说明 |
| **DEF-5（库级 grant）** | G-02 表级只追加收紧 | 方案 C 强化 G-2 语义（payloads 表只追加 + requireJson 强校验）；DEF-5 仍待 owner 决议 | 独立决策，但推荐同 PR 上线前解决（避免 chain_heads 单点锁失效） |
| **P0-9.1 PARTIAL 残留 8 项 FAIL** | 100% 归因 DEF-6 | 方案 C 闭环后预计全部转 PASS | 同 PR 复跑验证 |

### 6.2 严禁的次序（错则不可恢复）

- ❌ **先上 DDL-1（去 AUTO_INCREMENT）后上 J-1（去 NEVER）** → 写路径无 seq 值 → 立即报错或 DB 报错
- ❌ **先上 J-2（chain_heads CAS）后上 DDL-2（建 chain_heads 表）** → `Table not found`
- ❌ **先上 DDL-3（建 payloads 表）后上 J-7（6 写点双写）** → 主表 payload 列被去后单写 → 历史载荷丢失
- ❌ **只上 DDL 不上 J** → 写路径全 500 → 审计停摆
- ❌ **先上 BCrypt cost=10 后上 Hikari maxPoolSize=80** → 反向雪崩（cost 升 4× + 池未升 → 排队更严重）

### 6.3 必须的次序（缺则不闭环）

- ✅ DDL-1+2+3+4 与 J-1+2+3+4+5+6+7 **同 PR 同 commit 段**（commit message 标 `[DEF-9-DEF6-QA05P2]`）
- ✅ T-1 既有契约测 + T-2~4 新增契约测 **同 PR 必绿**（dev profile `@Tag("dev")`）
- ✅ T-5 QA-05-P2 100 并发复测 **必须** 跑在 ipd_perf 隔离库（同 QA-05 报告 §2 模式），不可在 ipd_dev 主库跑
- ✅ 4 实例并发启动验证 chain_heads 锁竞争 < 5ms p95（用 `/tmp/qa05p1-load.py` 改并发=4 实例 vs 16 线程对照）
- ✅ `git status --short` 列出的 37 M / 52 ?? 文件**不动**（主线程 + 兄弟流已声明的工作树边界）

---

## 7. 修复 owner 矩阵

| 卡 | 修复 owner | service 作者 | DDL 文件 | 阻塞依赖 |
|---|---|---|---|---|
| **DEF-9**（seq=AUTO_INCREMENT + 死代码 + 空洞） | IPD owner + AuditLogService 作者 | **当前**：未知（主线程代笔，commit 5b95a9d0 等） | **新增**：`docs/script/sql/update/2026-09-XX-ipd-audit-seq-no-autoinc.sql` + `...-chain-heads.sql` + `...-payloads.sql` | DEF-4 闭环（已满足）、DEF-6 决策（本方案覆盖） |
| **DEF-6**（json 列规范化） | IPD owner + AuditLogService 作者 | 同上 | **被本方案 C 覆盖**，单独立 DDL 不再需要 | 本方案 C 闭环（自动 supersede） |
| **QA-05-P2**（写串行化） | IPD owner + AuditLogService 作者 | 同上 | **被本方案 C 覆盖** | 本方案 C 闭环 |
| **P0-9.1 PARTIAL 残留 8 项** | 第二方 QA 验收 | 第二方 QA 蜂群 | 复用 `P0-9.1-业务链真实验收-20260905.py` | 本方案 C 闭环后复跑 |
| **DEF-4 第3修复废弃** | IPD owner | AuditLogService 作者 | 无 DDL | 本方案 C 闭环（catch 整段删除） |
| **BCrypt cost=10**（HIGH-1） | IPD owner | IpdMockDataInitializer 作者 | 无 | 独立；推荐同 PR |
| **Hikari maxPoolSize=80 prod**（P1-3） | IPD owner + db-migration skill | application-prod.yml 作者 | 无 | 推荐同 PR |
| **DEF-5（库级 grant）** | IPD owner + DBA | root 账号持有者 | 新建 `2026-09-XX-ipd-revoke-and-grant.sql` | 独立决策；推荐同 PR |
| **测试归仓** | Test author | QA-05-P2 / QA-04 / code-reviewer | 新增 `AuditChainGapHashSplitTest` + `AuditChainHeadsCasTest` + `AuditPayloadsDualWriteTest` | 本方案 C J-1~J-7 |

---

## 8. 备注与边界声明

### 8.1 边界（不动项）

- 不触动兄弟流 35+ 在途未提交主源码改动（git status --short 列出的 M/?? 全部跳过）
- 不 commit / 不 push / 不 reset / 不 clean / 不 branch-D / 不 checkout .
- 不抢推卡面状态（设计稿只是报告，挂卡面留给主线程）
- 不读未写未 add 的 SQL（4 个 untracked SQL：launch-date-change-requests / legacy-import / project-cert-items / audit-payload-longtext）

### 8.2 凭据

- 引用方式：「本地配置 `.codex/ipd-dev/config/credentials.json`」与「本地配置 `.codex/ipd-dev/config/<root cnf>`」
- 明文口令不入文（与 QA-06 §4 同款纪律）

### 8.3 关联文档路径

- 设计稿输出：`docs/ipd-系统说明/验收/AUDIT-CHAIN-TOPOLOGY-2026-09-05-DEF9-DEF6-方案设计稿.md`（本文件）
- 证据源：
  - `docs/ipd-系统说明/验收/SEC-AUD-2026-09-05-跨commit安全审查.md`（22 项全局视角）
  - `docs/ipd-系统说明/验收/PERF-AUD-2026-09-05-跨commit性能审查.md`（P0-3 / P1-4 视角）
  - `docs/ipd-系统说明/验收/QA-04-D2-DDL卫生三合一-20260905.md`（DDL 卫生现状）
  - `docs/ipd-系统说明/验收/QA-05-性能基准与容量验收-20260905.md`（实测基线）
  - `docs/ipd-系统说明/验收/2026-09-05-治理轮总账.md`（19 commit 闭环链）
  - `docs/script/sql/update/2026-09-04-ipd-p0-tables.sql`（DDL 基线）
  - `docs/script/sql/update/2026-09-05-ipd-audit-payload-longtext.sql`（DEF-6 方案 A DDL）
  - `docs/ipd-系统说明/开发计划-看板镜像.md`（DEF-9 卡面 description）
  - `ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/service/AuditLogService.java`
  - `ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/service/AuditEventData.java`
  - `ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/util/AuditHashChain.java`
  - `ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/domain/AuditLog.java`
  - `ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/controller/AuditLogController.java`

### 8.4 下一步

- 主线程：阅读本设计稿 → 决策 D1~D4 → 通知 IPD owner + AuditLogService 作者承接 → 在卡面 description 挂本设计稿路径 → 启动方案 C 实施 PR
- 卡面状态：本设计稿不抢推，DEF-9 / DEF-6 / QA-05-P2 三张卡面挂证据段留给主线程统一翻 inreview

---

**报告字数**：约 4500 字（含 7 主节 + 表格 + 互锁矩阵 + owner 矩阵）
**章节数**：7（现状拓扑 / 真因剖析 / 目标态 / 方案矩阵 / 推荐方案 / 跨 commit 互锁 / 修复 owner 矩阵）+ 1 备注边界
