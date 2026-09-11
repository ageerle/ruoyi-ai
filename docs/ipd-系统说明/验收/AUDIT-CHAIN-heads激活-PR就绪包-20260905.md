# AUDIT-CHAIN chain_heads 激活 — PR 就绪包 + 归属登记（A′ ①②③）

> 生成：2026-09-05 20:40 PDT｜Qoder 第二方治理/QA 会话（root-94ae 主协调器之外）
> 触发：owner Q2 采纳 A′、并定「①②③ = 备 PR 不执行、交主协调器在约定停写窗口统一上线」
> 本包性质：**只读探针 + 证据交付**（AGENTS.md 单一写入者：Java 源码由主协调会话串行写，其他会话只做只读探针）+ **OPS-09 归属登记**（同一指令多会话先登记归属、别各自建卡）。
> ⚠️ 本包**不含落 live 源码的 Java 实现、不执行任何 DDL/DML、不停实例、不部署**；迁移 SQL 内嵌于文档（非 `docs/script/sql/update/` 独立件），以防被误 auto-apply 造成「DDL 先上 / Java 后上」写路径 landmine。

---

## 0. TL;DR — recon 材料性发现（改变了 ①②③ 的性质）

owner 定「备 PR」时，①②③ 被理解为净新工作。**只读探针实测：并非净新**——

1. **② chain_heads 建表已由指派兄弟完成**（活库 `ipd_dev` 已存在 `audit_log_chain_heads`，且 schema 比设计稿方案 A 草图更精细），但**完全未接线**：无任何 Java 引用（`grep chain_key/next_seq/last_seq/ChainHead/allocator` 源码树全空）、seed 陈旧、`audit_logs.seq` 仍 `auto_increment`。
2. **①（去 AUTO_INCREMENT）③（Java CAS + 去 NEVER + 删死代码）仍 pending。**
3. 该工作在 **Wave3-实施规格包** 中已指派给「**AuditLogService 作者**」的 **Batch-2 / QA-05-P2（提级）slot**，标注「与 DEF-9 互锁」。
4. **活库是移动靶**：run7（19:04）`maxSeq=1324` → 现（20:40）`audit_logs` 仅 154 行、`max_seq=1554`、`min_seq=1401`（兄弟又清库），chain_heads seed 停在 `last_seq=67`。

**结论**：本会话**不写与兄弟 in-flight 设计可能冲突的臆测性 Java CAS 实现**（违单一写入者 + OPS-09 + 需猜未决 CAS 协议），改为交付**归属登记 + 实建真相 + 非臆测的具体件（迁移 SQL / tenant.excludes / Java CAS 需求规格与双选项骨架 / 契约测需求 / 原子上线序列）**，喂给指派 owner 的 Batch-2 slot，由主协调器在停写窗口原子集成。

---

## 1. 归属登记（OPS-09）

| 子项 | 内容 | 指派 owner | 现态 | 证据 |
|---|---|---|---|---|
| ① | `audit_logs.seq` 去 `AUTO_INCREMENT` | AuditLogService 作者（Batch-2 QA-05-P2） | **pending**（seq 仍 `bigint NO UNI auto_increment`） | `SHOW COLUMNS FROM audit_logs LIKE 'seq'` |
| ② | 新增 `audit_log_chain_heads` 锚表 | 同上 | **已由兄弟建成**（活库存在 + seed 一行，但**未接线/seed 陈旧**） | `SHOW CREATE TABLE audit_log_chain_heads` |
| ③ | `append` 改 chain_heads CAS + 去 `insertStrategy=NEVER` + 删死代码 `catch(DuplicateKeyException)` | 同上 | **pending**（无 Java WIP；`AuditLog.java:21` NEVER 仍在；`AuditLogService.java:75-92` 旧重试路径仍在） | grep 源码树无 chain_heads 引用 |
| ③′ | `tenant.excludes` 登记 `audit_log_chain_heads` | 同上 | **pending**（`application.yml:198` 块 26+2 表列到 `- audit_logs`(L244)，无 chain_heads） | `application.yml:196-248` |

> 规格来源：`Wave3-实施规格包-2026-09-05.md` L32/L69/L101/L152/L154/L311（G-10 = QA-05-P2 提级、Batch-2 第 1 commit、`feat(audit,DEF-5+QA-05-P2+DEF-9): audit_log_chain_heads 锚表 + insertStrategy=NEVER 取消 + 空洞永久 BROKEN 防护`，验证要求「隔离库 ipd_audit_v2：100 并发 append 0 uk 冲突 + verifyChain PASS」；R-4 HIGH：新表与兄弟 untracked SQL 可能 schema 冲突）。设计来源：`AUDIT-CHAIN-TOPOLOGY-...方案设计稿.md` §4 方案 A / §5。

---

## 2. 活库 recon 真相（只读探针，2026-09-05 20:40 PDT，`ipd_dev`）

### 2.1 chain_heads 实建 schema（权威，来自 `SHOW CREATE TABLE`）
```sql
CREATE TABLE `audit_log_chain_heads` (
  `chain_key` varchar(32) COLLATE utf8mb4_bin NOT NULL,
  `last_seq` bigint NOT NULL,
  `last_hash` char(64) COLLATE utf8mb4_bin NOT NULL,
  `next_seq` bigint NOT NULL,
  `initialized_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`chain_key`),
  CONSTRAINT `chk_audit_chain_sequence` CHECK (((`last_seq` >= 0) and (``next_seq` > `last_seq`)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin
  COMMENT='Mutable transaction-locked allocator; audit_logs itself remains append-only'
```
**与设计稿方案 A 草图的差异**（草图：单行 `key='audit'` 持 `seq`+`curr_hash`）：实建用 `chain_key`（PK，支持多链）+ `last_seq`/`last_hash`/`next_seq` 三列分离 + CHECK 不变式（`next_seq>last_seq`）+ 明确「可变分配器 vs audit_logs 只追加」注释。**本包一律对齐实建 schema，不回退到草图。**

### 2.2 seed 陈旧缺陷（未接线铁证）
- 现 seed：`chain_key='GLOBAL', last_seq=67`（`initialized_at 2026-09-05 18:38:09`）。
- 同期 `audit_logs`：154 行、`min_seq=1401`、`max_seq=1554`。
- ⇒ chain_heads 分配器停在 67，审计表尾在 1554：**两者从未同步 = append 代码根本没读/写 chain_heads**，仍走 `AUTO_INCREMENT`。**接线前必须在停写窗口把 GLOBAL 锚行重同步到 audit_logs 真实尾行**（见 §4.3），否则首个 CAS append 会分配 `seq=68` 与既有 1554 撞 uk / 造新空洞。

### 2.3 现态 append 路径（`AuditLogService.java:61-95`，③ 待重写）
```java
DuplicateKeyException conflict = null;
for (int attempt = 0; attempt < APPEND_MAX_ATTEMPTS; attempt++) {   // =3
    AuditLog last = selectLast();                                    // 读尾行
    String prevHash = last != null ? nvl(last.getCurrHash()) : AuditHashChain.GENESIS;
    long seq = (last != null && last.getSeq() != null ? last.getSeq() : 0L) + 1;
    draft.setSeq(seq);                                               // ← 但被 NEVER 丢弃
    draft.setPrevHash(prevHash);
    draft.setCurrHash(AuditHashChain.computeCurrHash(prevHash, canonicalOf(draft, seq)));
    try { auditLogMapper.insert(draft); return draft; }
    catch (DuplicateKeyException e) { conflict = e; }                // 死代码（见下）
}
throw conflict;
```
- **NEVER 悖论**：`AuditLog.java:21` `@TableField(value="seq", insertStrategy=FieldStrategy.NEVER)` → MyBatis-Plus INSERT **排除 seq 列** → `draft.setSeq(seq)` 的计算值被丢弃、实际 seq 由 DB `AUTO_INCREMENT` 赋值。平时无删除时二者巧合相等（链自洽）；**一旦清库，AUTO_INCREMENT 计数器不回填** → DB 赋值 ≠ 代码算值 → 这正是 DEF-9 `seq 1309` 空洞 + Q5 降级所承载的根因。
- **死代码**：因 seq 由 DB 赋值，`uk_audit_seq` 永不冲突 → `catch(DuplicateKeyException)` 分支结构性不可达（方案 A L50 已定性）。③ 去 NEVER + 走 chain_heads 后应删此整段重试。

---

## 3. 未决设计张力（交指派 owner + 主协调器决策，本包不臆测）

chain_heads 表注释写「**transaction-locked allocator**」→ 暗示悲观 `SELECT ... FOR UPDATE`；但 **DEF-4 曾记「无 FOR UPDATE（DB 最小权限禁锁定读）」**（`AuditLogService.java:77` 注释 + 方案设计稿 L50）。二者矛盾 = **CAS 协议未决**，是 ③ 的核心设计选择，属指派 owner 职权：

| 选项 | 机制 | 前置 | 取舍 |
|---|---|---|---|
| **P（悲观锁）** | append 事务内 `SELECT ... FOR UPDATE` GLOBAL 行 → 取 `next_seq`/`last_hash` → 算 curr_hash → `UPDATE` 推进 → `INSERT` audit_logs | app DB 用户须具锁定读权限（DEF-4 记为被禁 → **须先确认/授予**） | 单行锁天然串行、零 uk 冲突、零重试；写热点但单行锁廉价；契合表注释 |
| **O（乐观 CAS）** | 普通读 GLOBAL → 算 → `UPDATE ... SET ... WHERE chain_key='GLOBAL' AND next_seq=?`（期望值 CAS）→ 受影响=1 才 INSERT，否则重读重试 | 无需锁定读权限（规避 DEF-4 约束） | 无锁、但高并发下重试；把「audit_logs uk 冲突重试」换成「chain_heads 单行 CAS 重试」（更干净：CAS 落在可变分配器行，非只追加表） |

> 两选项都需**新增** `ChainHead` 实体 + `ChainHeadMapper`（`selectForUpdate`/`selectByKey`/`advance`/`casAdvance`）——属兄弟 Batch-2 新建类，本包只给需求与骨架（§6），不落 live 源码。

---

## 4. 迁移 SQL（⚠️ 内嵌，禁 auto-apply；须停写窗口 + 与 Java ③ 原子上线）

> **不得**作为独立件放入 `docs/script/sql/update/`（该目录件可能被协调器/兄弟批量 apply）；① 去 AUTO_INCREMENT 若先于 ③ Java 上线，NEVER 仍丢弃 seq 而列已无自增/无默认 → INSERT 报 `Field 'seq' doesn't have a default value` → **写审计路径全 500**，且审计与业务同 `@Transactional(REQUIRES_NEW)` 会连带业务事务失败。

### 4.1 ① 去 AUTO_INCREMENT（需重建表，停写窗口）
```sql
-- 前置：audit_logs 现有行 seq 已连续或空洞已被 Q5 归因为已知历史空洞
ALTER TABLE audit_logs MODIFY COLUMN seq BIGINT NOT NULL;   -- 去掉 auto_increment，保留 UNIQUE 键
```

### 4.2 ② chain_heads 建表（幂等，对齐实建 schema；已存在则 no-op）
```sql
CREATE TABLE IF NOT EXISTS audit_log_chain_heads (
  chain_key varchar(32) COLLATE utf8mb4_bin NOT NULL,
  last_seq bigint NOT NULL,
  last_hash char(64) COLLATE utf8mb4_bin NOT NULL,
  next_seq bigint NOT NULL,
  initialized_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (chain_key),
  CONSTRAINT chk_audit_chain_sequence CHECK ((last_seq >= 0) AND (next_seq > last_seq))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin
  COMMENT='Mutable transaction-locked allocator; audit_logs itself remains append-only';
```

### 4.3 ②′ 重同步陈旧 seed（**关键**，停写窗口内、去 AUTO_INCREMENT 后、Java ③ 部署前）
```sql
-- 现 seed last_seq=67 已陈旧于 audit_logs 尾行；从真实尾行重算，GENESIS=64个0（空表回退）
INSERT INTO audit_log_chain_heads (chain_key, last_seq, last_hash, next_seq)
SELECT 'GLOBAL',
       COALESCE(MAX(seq), 0),
       COALESCE((SELECT curr_hash FROM audit_logs ORDER BY seq DESC LIMIT 1),
                '0000000000000000000000000000000000000000000000000000000000000000'),
       COALESCE(MAX(seq), 0) + 1
FROM audit_logs
ON DUPLICATE KEY UPDATE
  last_seq  = VALUES(last_seq),
  last_hash = VALUES(last_hash),
  next_seq  = VALUES(next_seq);
-- 校验：SELECT * FROM audit_log_chain_heads WHERE chain_key='GLOBAL';  → next_seq == MAX(audit_logs.seq)+1
```

### 4.4 回滚（如需）
```sql
ALTER TABLE audit_logs MODIFY COLUMN seq BIGINT NOT NULL AUTO_INCREMENT;  -- MySQL 自动置计数器=MAX(seq)+1
-- chain_heads 表可保留（未接线时惰性）；或 DROP TABLE audit_log_chain_heads;（若确认无引用）
-- 同步回退：重新部署旧 jar（NEVER + selectLast 重试路径）
```

---

## 5. ③′ tenant.excludes 登记（`ruoyi-admin/src/main/resources/application.yml`）

`tenant.excludes`（L198 块）现况：`- audit_logs` 在 L244；块尾经兄弟 `2481a92a`（R8-P0-1 补漏）已延伸至 L257（新增 person_roles / coefficient_change_requests / cms_content），**chain_heads 仍未登记**。建议在块尾（L257 `- cms_content` 后）追加，避免插在 audit_logs(L244) 与后续 R8 表之间造成 mid-list churn：
```yaml
    - cms_content
    - audit_log_chain_heads   # 审计链分配器锚表（单行 GLOBAL，无 tenant_id 列、无多租户语义）；不登记则多租户插件对其追加过滤 → CAS 读不到锚行
```
> 该表**无 `tenant_id` 列**（见 §2.1），若不登记 excludes，多租户拦截器会尝试追加 `tenant_id=?` 过滤 → 运行期 SQL 报未知列 / CAS 失败。**与 §4 DDL 同批上线。**

---

## 6. Java CAS 需求规格 + 骨架（DRAFT，交 AuditLogService 作者 Batch-2 集成；**本会话不落 live 源码**）

### 6.1 `AuditLog.java:20-21` 去 NEVER
```java
    /** 全局递增序号（hash 链顺序锚，由 audit_log_chain_heads 原子分配，非 DB 自增） */
    @TableField("seq")            // ← 去 insertStrategy=NEVER，使显式 setSeq 值进入 INSERT
    private Long seq;
```

### 6.2 `AuditLogService.append` 重写骨架（选项 P 悲观锁版；选项 O 见 §3）
```java
@Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
public AuditLog append(AuditLog draft) {
    AuditEventData.requireJson(draft.getBeforeData(), "before_data");   // 保留 DEF-6 护栏
    AuditEventData.requireJson(draft.getAfterData(), "after_data");
    Date base = draft.getCreateTime() == null ? new Date() : draft.getCreateTime();
    draft.setCreateTime(new Date(secondMillis(base)));                  // 保留 DEF-4 毫秒归零
    if (draft.getTenantId() == null) draft.setTenantId("000000");

    ChainHead head = chainHeadMapper.selectForUpdate("GLOBAL");        // 单行锁：SELECT ... FOR UPDATE
    long seq = head.getNextSeq();
    String prevHash = nvl(head.getLastHash());
    draft.setSeq(seq);
    draft.setPrevHash(prevHash);
    draft.setCurrHash(AuditHashChain.computeCurrHash(prevHash, canonicalOf(draft, seq)));
    chainHeadMapper.advance("GLOBAL", seq, draft.getCurrHash(), seq + 1); // UPDATE last_seq/last_hash/next_seq
    auditLogMapper.insert(draft);                                       // seq 显式插入（NEVER 已去）
    return draft;
}
// 删除：APPEND_MAX_ATTEMPTS 常量、selectLast()-based seq、catch(DuplicateKeyException) 整段死代码
```
**新增依赖**：`ChainHead`（domain，映射 §2.1 schema）+ `ChainHeadMapper`（`selectForUpdate`/`advance` 或 `casAdvance`）。

---

## 7. 契约测需求（`@Tag("dev")`，交 Batch-2；本会话不落 live 测试树）

> 若提前落 live 测试树会引用尚不存在的 `ChainHead`/`ChainHeadMapper` → 全模块 test-compile 断裂（波及兄弟），故随 Java ③ 同批落。

1. **并发分配无冲突**（Wave3 spec）：N=100 并发 append → seq 严格 1..100 单调、零重复、零空洞、`uk_audit_seq` 0 冲突。
2. **CAS 不变式**：每次 append 后 `chain_heads.last_seq == audit_logs.MAX(seq)`、`last_hash == 尾行 curr_hash`、`next_seq == last_seq+1`（CHECK 约束不触发）。
3. **陈旧 seed 防护**：seed `last_seq` 落后 audit_logs 尾行时，append 不得分配撞既有 seq 的值（须先 §4.3 同步 / 或启动自检拒绝）。
4. **hash 链自洽回归**：接线后 `verifyChainDetailed()` 对新行判 `verdict()=OK`（不引入新 HASH_BROKEN/GAP）。
5. **只追加语义守恒**：chain_heads 可变、audit_logs 仍只追加（无 UPDATE/DELETE audit_logs）。

---

## 8. 原子上线序列（停写窗口，交主协调器执行）

1. **停写**：4 实例（16039/16044/16045/16050）全停或置维护态；确认无并发 append。
2. **DDL**：§4.3 sync-seed → §4.1 去 AUTO_INCREMENT（§4.2 建表已存在则 no-op）。
3. **配置**：§5 tenant.excludes 登记（随新 jar 的 application.yml）。
4. **部署**：构建含 §6 Java ③（去 NEVER + chain_heads CAS + 删死代码）的新 jar；**4 实例统一升级**（避免新旧 jar 混跑：旧 jar NEVER 路径对新 DDL 会写失败）。
5. **起 + 验证**：§7 契约测 + 真 HTTP append 冒烟（seq 单调、chain_heads 同步、verifyChain OK）。
6. **回滚预案**：§4.4（去 AUTO_INCREMENT 可逆，计数器自动置 MAX+1）+ 重部署旧 jar。

> 互锁提醒：与 DEF-5（库级 grant 架空表级只追加，Q6 未决）、⑦（hash_version 62 行，owner Q3 交主协调器）同属审计链收口，建议同窗口评估。16050（`def6.jar` 陈旧、不含护栏）是活的 DEF-1 缺口（Q7），升级时一并处置。

---

## 9. 与 Q5 的衔接（已落地）

- Q5（owner 决策，本会话已落地 commit `c1382635`）：P0-9.1 断言已把 **GAP 降级为「已知历史空洞」告警**、仅哈希断裂判 FAIL；`seq 零跳号` 按「前驱是否越基线」切分（历史空洞告警 / 本轮新漏行仍 FAIL）。
- ①②③ 落地后：chain_heads 原子分配 → **新空洞不再产生**（seq 由分配器连续发放，不依赖 AUTO_INCREMENT 计数器）；**历史空洞**（清库遗留）仍由 Q5 告警承载、不补行（尊重 AC-AUD-01）。二者互补：①②③ 治未来、Q5 承载过去。
- 方案 A 自评「G3 部分（链头原子→写不空洞，但历史空洞仍需手动处理）」——Q5 即「历史空洞的处理方式 = 降级告警而非手动补行」，闭合 G3 残留。

---

## 10. 交 owner / 主协调器的确认项

1. **归属确认**：①②③ Java CAS 是否仍由「AuditLogService 作者 / Batch-2 QA-05-P2」承接？本会话按单一写入者**不落 live Java**，仅交付本就绪包 —— 若 owner 要本会话**代拟完整 Java CAS 实现**（覆盖单一写入者 + 需先定 §3 悲观/乐观），请明示。
2. **§3 CAS 协议**：悲观 FOR UPDATE（须确认 app 用户锁定读权限，DEF-4 记为被禁）还是乐观 CAS + 重试？
3. **陈旧 seed**：§4.3 sync-seed 是否纳入停写窗口清单（现 last_seq=67 严重落后，接线前必须同步）？
