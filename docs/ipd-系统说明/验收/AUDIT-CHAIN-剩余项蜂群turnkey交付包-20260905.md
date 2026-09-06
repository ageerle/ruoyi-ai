# AUDIT-CHAIN 剩余项 — 蜂群 turnkey 交付包（Q6/⑦/Q7 + ①②③ CAS 双变体）

> 生成：2026-09-05 21:15 PDT｜Qoder 第二方治理/QA 会话（root-94ae 主协调器之外）
> 触发：owner 指令「基于以上利用多个专业智能体并行执行」（确认 §10 三项框架：①②③ 归属维持 Batch-2、本会话**不落 live Java**；CAS 协议交指派 owner；seed sync 纳入停写窗口）
> 执行模式：盘点（只读探针）→ 实施（本文档 turnkey 件）→ 验证（CodeReview 专业智能体 + 静态核对）→ 文档/看板同步。
> ⚠️ 本包**不执行任何 DDL/DML/GRANT/REVOKE、不停实例、不部署、不落 live 源码**；Q6 REVOKE 与迁移 DDL 一样属 owner 决策域，仅交付 runbook。

---

## 0. TL;DR — 本轮四项材料性事实更新（改变处置方案）

| # | 事实（只读探针，2026-09-05 21:09–21:12 PDT，root@socket） | 推翻/更新了什么 |
|---|---|---|
| F1 | **权限模型已被协调器铺好但被库级架空**：`audit_logs` 表级=`SELECT,INSERT`（只追加意图已设）；`audit_log_chain_heads` 表级=`SELECT,UPDATE`（分配器模型）；**但库级 `GRANT SELECT,INSERT,UPDATE,DELETE ON ipd_dev.*` 存在**（mysql.db: Update=Y,Delete=Y）→ MySQL 权限跨层取并集，**表级只追加被库级架空**（DEF-5 坐实） | Q6 从「待定性」→「铁证 + 可执行最小 REVOKE runbook」（§2） |
| F2 | **CAS 协议张力可解**：chain_heads 表级 `SELECT` 即覆盖 `SELECT ... FOR UPDATE` 锁定读（MySQL 锁定读仅需 SELECT 权限）+ `UPDATE` 已授 → **选项 P（悲观锁）权限可行**；选项 O（乐观 CAS）本就可行。DEF-4 注释「禁 FOR UPDATE」指**旧 audit_logs 路径**（DEF-4 当时场景），非 chain_heads | PR就绪包 §3「P 须先确认/授予锁定读权限」**已被证据推进**：P 无需任何授权变更（§4 推荐更新） |
| F3 | **⑦ hash_version 现态全 NULL**：列存在（`int NULL`，注释 `NULL=legacy-v1,2=canonical-json-v2`，qa04d2-backport.sql + 基线 L513 建），**207/207 行 NULL、非 NULL 残留=0**（兄弟又清库：min_seq=1401/max_seq=1607）；且 `AuditHashChain` **已内置双版本机制**（`ACTIVE_CANONICAL_VERSION=1` + `canonicalV1/canonicalV2/canonicalByVersion` + `V2_PREFIX="v2|"`，L19-92） | 旧记忆「62 行 v1 hash 标 v2 → 落地即造 62 假断裂」**已随清库消失**；⑦ 残留收窄为「实体/append/verify 未接线 + 列全 NULL（=v1，与现行算法一致）」→ 处置包见 §3 |
| F4 | **停写窗口事实开着 + seed 缺口扩大**：0 个 ruoyi-admin 进程、无 160xx 监听、**0 个活跃 ipd_app 连接**；chain_heads GLOBAL 冻结在 `last_seq=67/next_seq=68`（initialized_at 18:38 未动），audit_logs max_seq=1607 → **gap=1540** | Q7（16050 陈旧 jar 活缺口）**moot**（§6）；seed sync 必要性再铁证（§5） |

**交付物**：§2 Q6 REVOKE runbook｜§3 ⑦ 处置包｜§4 ①②③ CAS 双变体 turnkey DRAFT（P 推荐 + O 备选，升级自 PR就绪包 §6 骨架）｜§5 seed/窗口刷新｜§6 Q7 重定性｜§8 owner 确认项（更新版）。
**与 [PR就绪包](AUDIT-CHAIN-heads激活-PR就绪包-20260905.md)（commit 916d79e8）的关系**：DDL/tenant.excludes/契约测需求/原子上线序列仍以该包为准；本包是其**证据增补 + CAS 升级 + Q6/⑦/Q7 新增处置**。

---

## 1. 只读探针证据（全部 root@socket 或源码 grep，零写入）

```
21:10  SHOW GRANTS FOR 'ipd_app'@'127.0.0.1'   → GRANT USAGE ON *.*
                                                 → GRANT SELECT,INSERT,UPDATE,DELETE ON `ipd_dev`.*    ← F1 架空源
                                                 → GRANT SELECT,INSERT ON `ipd_dev`.`audit_logs`         ← 只追加意图
                                                 → GRANT SELECT,UPDATE ON `ipd_dev`.`audit_log_chain_heads` ← 分配器模型
                                                 → GRANT ALL ON `ipd_perf`.*（压测库，另议）
                                                 → 121 张业务表各自表级 SELECT,INSERT,UPDATE,DELETE
21:10  mysql.db WHERE User='ipd_app'            → ipd_dev: Select=Y Insert=Y Update=Y Delete=Y Drop=N Alter=N
21:11  information_schema.tables(ipd_dev)       → 125 张 BASE TABLE、0 个 VIEW
21:11  tables_priv 覆盖                         → 124/125；唯一未覆盖 = _ipd_schema_history（迁移工具表）
21:11  grep _ipd_schema_history app 源码/yml    → 0 命中（仅迁移工具用，app 不触）
21:11  processlist WHERE user='ipd_app'         → 0 行（无活跃连接，REVOKE 影响面=0）
21:09  audit_logs                               → 207 行 / min_seq=1401 / max_seq=1607 / seq 仍 auto_increment
21:09  audit_log_chain_heads                    → GLOBAL last_seq=67 next_seq=68 hash16=b33adbd7df645bfc @18:38:09
21:10  audit_logs.hash_version                  → 207/207 NULL；WHERE IS NOT NULL count=0
21:12  AuditHashChain.java                      → GENESIS="0".repeat(64)；ACTIVE_CANONICAL_VERSION=1；V2_PREFIX="v2|"；
                                                  canonical/canonicalV1/canonicalV2/canonicalByVersion/sha256Hex/computeCurrHash
21:12  AuditLogMapper.java                      → extends BaseMapperPlus<AuditLog,AuditLog> + @Update updateChainHash 先例
21:12  AuditLogService.java(HEAD)               → append 仍旧重试路径（L76-92）；verifyChainDetailed 四态已在；
                                                  selectLast 仅 append 引用；AuditEventData.requireJson(String,String)
```

---

## 2. Q6 / DEF-5 处置包 — 库级 grant 架空表级只追加：最小 REVOKE runbook

### 2.1 病灶（F1 证据复述）

MySQL 权限按 global→db→table→column **逐层取并集**：ipd_app 对 `audit_logs` 的有效权限 = 库级(S,I,U,D) ∪ 表级(S,I) = **S,I,U,D** → 表级「只追加」被库级 UPDATE/DELETE 架空——G-02「审计只追加」在 DB 层不成立，任何拿到 ipd_app 凭据的代码路径都能 UPDATE/DELETE 审计行。

### 2.2 推荐方案：最小 REVOKE（只收库级 UPDATE,DELETE）

```sql
-- 以 root 执行（mysql-client.cnf socket）；幂等可重复；当前 0 活跃 ipd_app 连接，即时生效
--（REVOKE 语句自动同步内存权限缓存，已建连接自下一条语句起按新权限检查，无需 FLUSH/重连；
--  仅直改 mysql.* 字典表才需 FLUSH PRIVILEGES —— CodeReview MINOR-4 措辞精确化）
REVOKE UPDATE, DELETE ON ipd_dev.* FROM 'ipd_app'@'127.0.0.1';

-- 验证 1：库级只剩 SELECT,INSERT
SHOW GRANTS FOR 'ipd_app'@'127.0.0.1';      -- 期望行：GRANT SELECT,INSERT ON `ipd_dev`.* ...
-- 验证 2：字典层
SELECT Select_priv,Insert_priv,Update_priv,Delete_priv FROM mysql.db WHERE User='ipd_app' AND Db='ipd_dev';
-- 期望 Y/Y/N/N
-- 验证 3（可选，用 app 凭据负向探测）：
--   mysql --defaults-file=mysql-app.cnf ipd_dev -e "UPDATE audit_logs SET reason='x' WHERE seq=<不存在seq>;"
--   期望 ERROR 1142 (UPDATE command denied) —— 只追加在 DB 层生效
```

**执行前探针（CodeReview MINOR-1 要求，本会话已于 21:18 PDT 亲跑，全清）**：

```
routines/triggers/events WHERE schema=ipd_dev → 各 0 行（无 DEFINER 权限面，无存储例程断权风险）
mysql.user WHERE user='ipd_app'               → 仅 @'127.0.0.1' 一个 host 变体（REVOKE 目标全覆盖）
mysql.columns_priv WHERE User='ipd_app'       → 0 行（无列级权限参与并集）
⇒ REVOKE 影响面零残留，安全边界全闭合
```

**为什么是最小而不是全收**：保留库级 SELECT,INSERT 作安全网——①唯一未被表级覆盖的表 `_ipd_schema_history` 不断访问（虽 app 零引用，迁移工具走 root/migrator cnf，但保守起见不制造新边界情况）；②未来新增表在登记表级 grant 前仍可读/写非破坏性权限。

**生效后各表有效权限（并集推演）**：

| 表 | 库级(收后) | 表级 | 有效 | 评价 |
|---|---|---|---|---|
| audit_logs | S,I | S,I | **S,I** | ✅ 只追加在 DB 层强制成立（DEF-5 闭合） |
| audit_log_chain_heads | S,I | S,UPDATE | S,I,UPDATE | ✅ P(FOR UPDATE+S)/O(CAS UPDATE) 都可行；库级 INSERT 漏入为**低危残留**（伪造额外 chain_key 行对不同 PK 惰性；DELETE 已收，GLOBAL 行不可删） |
| 121 张业务表 | S,I | S,I,U,D | S,I,U,D | ✅ 不变（业务 CRUD 完好） |
| _ipd_schema_history | S,I | — | S,I | ✅ 不断 |

### 2.3 可选收紧（第二步，owner 决策）

```sql
-- 彻底纯表级模型（同时消掉 chain_heads 的库级 INSERT 漏入）：
REVOKE SELECT, INSERT ON ipd_dev.* FROM 'ipd_app'@'127.0.0.1';
-- 代价：未来新表在表级登记前完全不可访问（fail-closed）；_ipd_schema_history 断 app 访问（当前无引用，可接受）
```

### 2.4 回滚

```sql
GRANT UPDATE, DELETE ON ipd_dev.* TO 'ipd_app'@'127.0.0.1';   -- （2.3 则补回 SELECT,INSERT）
```

### 2.5 执行时机与前置

- **现在即理想窗口**：0 实例、0 ipd_app 连接，无会话被中途断权限。
- 前置零依赖：不依赖 ①②③ Java/DDL；与审计链激活正交，可先行独立执行。
- **本会话不执行**（Q6 owner 未决 + 共享库权限面变更属协调器职权）；本 runbook 即 turnkey 件。

---

## 3. ⑦ / hash_version 处置包 — 现状重定性 + 三选项

### 3.1 真相（F3 证据）

- **急性问题已消失**：旧「62 行 v1 hash 却标 hash_version=2 → 双版本验落地即造 62 行假断裂」的数据随兄弟清库不复存在（现 207/207 NULL）。
- **一致性现状**：列语义 `NULL=legacy-v1`；现行 `AuditHashChain.canonical()` 实际按 `ACTIVE_CANONICAL_VERSION=1` 出 v1 串 → **全 NULL 标签与全 v1 哈希天然一致**，无错标。
- **机制已备、接线未做**：util 层 `canonicalV1/V2/byVersion` 齐备；但 `AuditLog` 实体不映射 `hash_version`、append 不写（恒 NULL）、verifyChainDetailed 不读（恒按 v1 canonical 验）→ **列当下是"无人写、无人读"的休眠列**。

### 3.2 三选项

| 选项 | 内容 | 代价/风险 | 建议 |
|---|---|---|---|
| **a. 维持现状** | 列休眠（NULL=v1 语义自洽）；等真正规划 v2 迁移时再接线 | 零改动、零风险；未来接线时需回填策略（清库后全体 v1，回填=全 1 或保持 NULL 等价） | ✅ **现在选 a** |
| b. 前向接线 | 实体映射 + append 写 `ACTIVE_CANONICAL_VERSION` + verify 按 `canonicalByVersion(行版本, ...)` 验 | 动实体/append/verify 三处（协调器 Java 域）；当前无任何消费者读该列，属提前投资；接线须与 ①②③ 同批（都在动 append） | 若 owner 要为 v2 迁移铺路，**并入 ①②③ 的 Batch-2 PR 一起做**（同文件同事务面，边际成本最低） |
| c. 删列 | DROP COLUMN + 回写基线 DDL/qa04d2-backport 脚本 | DDL 变更 + 两份 SQL 脚本回写；v2 机制（canonicalV2/V2_PREFIX）已入库则删列自断后路 | ❌ 不建议 |

### 3.3 交主协调器（owner Q3 原裁决范围）的更新

原 Q3 语境（62 行错标）已消；若采 3.2-a，⑦ 可**直接标 moot/关闭**（附本节证据）；若采 3.2-b，则在 Batch-2 PR 规格里加一行「hash_version 接线随 ①②③ 同批」。

---

## 4. ①②③ turnkey — CAS 双变体完整 DRAFT（升级自 PR就绪包 §6 骨架）

> 归属不变：Wave3 Batch-2 / QA-05-P2「AuditLogService 作者」。本节为 **DRAFT（落 markdown 不落 live 源码）**，供 Batch-2 直接取用。
> **推荐更新**：原 §3 张力表中「P 须先确认/授予锁定读权限」已被 F2 证据消解——**P 无需授权变更即可行**，且与实建表注释「transaction-locked allocator」设计意图一致、零重试、天然串行；O 保留为备选（无锁偏好/未来热点顾虑时）。**建议 owner 拍 P**；两变体以下都给全。

### 4.1 变更清单（两变体共通）

1. `AuditLog.java` L20-22：去 `insertStrategy = FieldStrategy.NEVER` → `@TableField("seq")`（注释同步改为「由 audit_log_chain_heads 原子分配」）。
2. 新增 `domain/AuditChainHead.java`（§4.2）。
3. 新增 `mapper/AuditChainHeadMapper.java`（§4.3 P / §4.4 O 二选一保留对应方法）。
4. 重写 `AuditLogService.append()`（§4.2 P / §4.4 O）。
5. **删除**：`APPEND_MAX_ATTEMPTS` 常量、append 内重试循环与 `catch (DuplicateKeyException)`、`selectLast()` 私有方法（全类唯一调用方是 append）、**`orderBySeq()` 私有方法（唯一调用方是 selectLast 的 L199，一并删除免留死代码 —— CodeReview MINOR-3）**、`import ...DuplicateKeyException`。
6. `application.yml` tenant.excludes 追加 `- audit_log_chain_heads`（PR就绪包 §5，⚠️ **必须先于/同批部署**——多租户拦截器会改写注解 SQL 追加 `tenant_id` 过滤，未登记则 append 全断）。
7. DDL/seed/上线序列：仍按 PR就绪包 §4/§8（sync-seed → 去 AUTO_INCREMENT → 部署 → 契约测），窗口现状见本文 §5。

### 4.2 变体 P（悲观锁，推荐）

```java
// ===== domain/AuditChainHead.java（新增；风格对齐 AuditLog：Serializable + 显式 @TableField）=====
package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/** 审计链分配器锚行（audit_log_chain_heads，单行 GLOBAL；表本身可变、audit_logs 仍只追加） */
@Data
@TableName("audit_log_chain_heads")
public class AuditChainHead implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "chain_key", type = IdType.INPUT)
    private String chainKey;

    @TableField("last_seq")
    private Long lastSeq;

    @TableField("last_hash")
    private String lastHash;

    @TableField("next_seq")
    private Long nextSeq;

    @TableField("initialized_at")
    private Date initializedAt;
}
```

```java
// ===== mapper/AuditChainHeadMapper.java（新增；@Select/@Update 注解风格对齐 AuditLogMapper.updateChainHash 先例）=====
package org.ruoyi.ipd.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;
import org.ruoyi.ipd.domain.AuditChainHead;

/** audit_log_chain_heads mapper（分配器；仅 append 事务内使用） */
public interface AuditChainHeadMapper extends BaseMapperPlus<AuditChainHead, AuditChainHead> {

    /** 悲观锁读锚行：事务内持有行锁至提交，全局串行化 append（锁定读仅需 SELECT 权限，chain_heads 表级已授） */
    @Select("SELECT chain_key, last_seq, last_hash, next_seq, initialized_at"
          + " FROM audit_log_chain_heads WHERE chain_key = #{chainKey} FOR UPDATE")
    AuditChainHead selectForUpdate(@Param("chainKey") String chainKey);

    /** 推进锚行（last_seq/last_hash/next_seq 原子前移；与 selectForUpdate 同事务，锁保护下必中） */
    @Update("UPDATE audit_log_chain_heads SET last_seq = #{lastSeq}, last_hash = #{lastHash}, next_seq = #{nextSeq}"
          + " WHERE chain_key = #{chainKey}")
    int advance(@Param("chainKey") String chainKey, @Param("lastSeq") long lastSeq,
                @Param("lastHash") String lastHash, @Param("nextSeq") long nextSeq);
}
```

```java
// ===== AuditLogService.append() 重写（变体 P）=====
// 新增字段/常量：
//   private static final String CHAIN_KEY_GLOBAL = "GLOBAL";
//   private final AuditChainHeadMapper chainHeadMapper;   // @RequiredArgsConstructor 自动注入
@Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
public AuditLog append(AuditLog draft) {
    // —— 以下三段护栏原样保留（DEF-6 / DEF-4），语义见现行实现注释 ——
    AuditEventData.requireJson(draft.getBeforeData(), "before_data");
    AuditEventData.requireJson(draft.getAfterData(), "after_data");
    Date base = draft.getCreateTime() == null ? new Date() : draft.getCreateTime();
    draft.setCreateTime(new Date(secondMillis(base)));
    if (draft.getTenantId() == null) {
        draft.setTenantId("000000");
    }
    // —— ①②③：锚行悲观锁 → 原子分配 seq/prevHash ——
    AuditChainHead head = chainHeadMapper.selectForUpdate(CHAIN_KEY_GLOBAL);
    if (head == null) {
        // 锚行缺失 = seed 未初始化/被清：fail-fast，禁止代码自举（自举会与并发方竞态；修复走停写窗口 sync-seed runbook）
        throw new IllegalStateException(
            "audit_log_chain_heads missing GLOBAL anchor — run seed-sync (PR就绪包 §4.3) before appending");
    }
    long seq = head.getNextSeq();
    // GENESIS 兜底而非 nvl 空串：锚行 last_hash=NULL（清库后未 sync-seed 的病态）时，
    // 链首 prevHash 必须是 64×'0'（外部验链工具硬编码 GENESIS 起验）—— CodeReview MINOR-5
    String prevHash = head.getLastHash() == null ? AuditHashChain.GENESIS : head.getLastHash();
    draft.setSeq(seq);                                   // NEVER 已去：显式值真正进入 INSERT
    draft.setPrevHash(prevHash);
    draft.setCurrHash(AuditHashChain.computeCurrHash(prevHash, canonicalOf(draft, seq)));
    // advance=1 防御断言（锁保护下正常必 1；0 = schema/chain_key 漂移，静默继续会劣化为
    // 撞 uk 或错链 —— CodeReview MINOR-2，与 head==null fail-fast 对称）
    if (chainHeadMapper.advance(CHAIN_KEY_GLOBAL, seq, draft.getCurrHash(), seq + 1) != 1) {
        throw new IllegalStateException("audit chain anchor advance missed — schema/config drift suspected");
    }
    auditLogMapper.insert(draft);
    return draft;
}
```

**P 语义要点（写进 Batch-2 PR 描述）**：
- `FOR UPDATE` 行锁在 REQUIRES_NEW 事务内持有至提交 → append 全局串行，**零重试、零 CAS 竞态**；锁序单一（先锁锚行后插 audit_logs，无第二锁源 → 无死锁环）。
- **陈旧 seed 防线**：若锚行落后（next_seq ≤ audit_logs 现存 seq），`insert` 撞 `uk_audit_seq` 抛 DuplicateKeyException 且**不重试不吞**（整事务回滚，advance 一并回滚）→ 故障恒定、响亮、不自愈为静默错链；事前防线 = §5 sync-seed + 可选启动自检（§4.5）。
- verifyChain/rebuildChain/canonicalOf/secondMillis/nvl 全部不动。

### 4.3 变体 O（乐观 CAS + 重试，备选）

```java
// mapper 方法（替代 4.2 的 selectForUpdate/advance 组合；普通读用 BaseMapperPlus.selectById 即可）：
/** 期望值 CAS 推进：仅当锚行仍处期望 next_seq 时前移；返回影响行数（1=赢得分配权，0=他方已抢先） */
@Update("UPDATE audit_log_chain_heads SET last_seq = #{expectedSeq}, last_hash = #{newHash}, next_seq = #{newSeq}"
      + " WHERE chain_key = #{chainKey} AND next_seq = #{expectedSeq}")
int casAdvance(@Param("chainKey") String chainKey, @Param("expectedSeq") long expectedSeq,
               @Param("newHash") String newHash, @Param("newSeq") long newSeq);
```

```java
// append 主体（护栏三段同 4.2，略）。⚠️ 方法注解必须比 P 多降一档隔离级别（CodeReview MAJOR-1）：
//   @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW,
//                  isolation = Isolation.READ_COMMITTED)
// 原因见下方「O 专属语义陷阱」第 1 条；import 需补 org.springframework.transaction.annotation.Isolation
private static final int CAS_MAX_ATTEMPTS = 8;   // 审计写入频度下 8 轮已远超充分
for (int attempt = 0; attempt < CAS_MAX_ATTEMPTS; attempt++) {
    AuditChainHead head = chainHeadMapper.selectById(CHAIN_KEY_GLOBAL);
    if (head == null) { throw new IllegalStateException("... 同 4.2 ..."); }
    long seq = head.getNextSeq();
    String prevHash = head.getLastHash() == null ? AuditHashChain.GENESIS : head.getLastHash();  // 同 4.2（MINOR-5）
    draft.setSeq(seq);
    draft.setPrevHash(prevHash);
    draft.setCurrHash(AuditHashChain.computeCurrHash(prevHash, canonicalOf(draft, seq)));
    if (chainHeadMapper.casAdvance(CHAIN_KEY_GLOBAL, seq, draft.getCurrHash(), seq + 1) == 1) {
        auditLogMapper.insert(draft);              // CAS 赢得后插入；insert 失败则整事务回滚（含 CAS 推进）
        return draft;
    }
    // CAS=0：他实例已抢先，重读锚行重算重试
}
throw new IllegalStateException("audit chain CAS contention exhausted after " + CAS_MAX_ATTEMPTS + " attempts");
```

**O 专属语义陷阱（须写进 PR 描述）**：
- **【MAJOR-1，CodeReview 发现】RR 快照读致 CAS 重试永久失明**：MySQL 默认 REPEATABLE READ 下，事务内**第一条普通 SELECT** 建立快照后固定不变——`selectById` 是普通一致性读，线程 A 首轮读到 `next_seq=100` 后，他方 CAS 推进并提交，A 重试轮**仍见快照旧值 100** → CAS 恒 0 → 8 轮空转稳定抛「争用耗尽」（可用性故障，非错链）。修法即上述 `isolation = READ_COMMITTED`（每轮新快照）；替代 `LOCK IN SHARE MODE` 可行但语义上已退化为 P 的共享锁变体。本仓数据源无任何 isolation 配置（application.yml grep 零命中），RR 默认必然生效。**若 owner 拍 O 而未加此行，§4.4 契约测第 1 条（100 并发）必稳定失败**——此缺陷反向强化「拍 P」（P 的 FOR UPDATE 是当前读，免疫此坑）。
- **只对 CAS=0 重试**；`insert` 的 DuplicateKeyException（陈旧 seed 撞既有 seq）**不得进入重试**——否则每轮 CAS 前移一位继续撞，形成追尾循环；应让它直接抛出（与 P 同样响亮失败）。
- 重试意味着同一 draft 的 curr_hash 以不同 seq 重算多轮——循环内每次全量重算（如上），勿把哈希计算提到循环外。

### 4.4 契约测（`@Tag("dev")`，两变体通用；随 Java 同批落，勿提前）

```java
// AuditChainHeadCasContractTest（骨架；隔离库 ipd_audit_v2，Wave3 spec 验证要求）
// 1) 100 线程并发 append → 收集 seq：严格连续、唯一、零 uk 冲突；chain_heads.last_seq == MAX(seq)
// 2) 不变式：append N 次后 last_hash == 尾行 curr_hash && next_seq == last_seq + 1（CHECK 约束从未触发）
// 3) 陈旧 seed 防护：手工把锚行回拨（root/隔离库）→ append 必抛 DuplicateKeyException（P）
//    / 直接传播不重试（O），且 audit_logs 无半行写入（事务回滚）
// 4) 锚行缺失：TRUNCATE chain_heads（隔离库）→ append 抛 IllegalStateException(fail-fast)，无自举 INSERT
// 5) hash 链自洽回归：接线后 verifyChainDetailed() 全行 verdict OK（不引入新 HASH_BROKEN/GAP）
// 6) 只追加守恒：全测试期 audit_logs 仅 INSERT（无 UPDATE/DELETE 语句，可开 general_log 断言或 MyBatis 拦截器计数）
//（若选 O 且未加 READ_COMMITTED：第 1 条即 MAJOR-1 的稳定复现场景，100 并发必失败）
```

### 4.5 可选硬化：启动自检（防陈旧 seed 上线）

```java
// ApplicationRunner（或 @EventListener(ApplicationReadyEvent)）：
//   SELECT next_seq FROM audit_log_chain_heads WHERE chain_key='GLOBAL'
//   SELECT COALESCE(MAX(seq),0) FROM audit_logs
//   if (nextSeq <= maxSeq) log.error("AUDIT-CHAIN stale seed: next_seq={} <= audit_logs.MAX(seq)={} — 先跑 sync-seed", ...)
// 只读两条 SELECT、零锁；把「陈旧 seed 撞 uk 的运行期故障」提前到启动日志。
```

---

## 5. seed-sync 与停写窗口 — 时效刷新（对 PR就绪包 §4.3/§8 的补充）

- **窗口现状**：21:11 PDT 全部实例停（0 进程/0 监听/0 ipd_app 连接）→ **停写窗口事实开着**，主协调器可随时按 PR就绪包 §8 序列执行（sync-seed → 去 AUTO_INCREMENT → 统一部署 → 契约测）。
- **缺口现状**：GLOBAL `last_seq=67` vs audit_logs `max_seq=1607`，**gap=1540**；§4.3 sync SQL 用 `COALESCE(MAX(seq),...)` 从真实尾行重算，**对任意缺口自适应**，无需改 SQL，只需换新证据数字。
- 兄弟仍在直连写库（本会话观察窗口内 207 行曾 154→207 增长）→ **执行前一刻**须重读 `MAX(seq)` 并确认协调器已广播停写，防 sync-seed 与直连写竞态（sync 后、部署前若有新 append 行，锚行即再落后 → 靠 §4.5 启动自检兜底报错）。

---

## 6. Q7 重定性 — 16050 陈旧 def6.jar

- 原定性：16050 跑 def6.jar（无 DEF-6 护栏）= 活的 DEF-1 缺口。**现全实例已停 → 缺口无载体，Q7 moot**。
- 转为部署纪律（交主协调器，下次起实例时生效）：统一从 HEAD 构建/或沿用 def6i 系（含护栏 + 四态 verify）——**禁再启 def6.jar 系**；起后跑一条带载荷写动作冒烟（验证 requireJson 在位）+ `/api/v1/audit-logs/verify` 四态字段存在（验证 Controller 新码在位）。

---

## 7. 并行执行编队与纪律（本包如何被生产/验证）

| 槽位 | 执行者 | 产出 |
|---|---|---|
| 只读事实探针（§1） | 本治理会话（Bash/mysql，全 SELECT/SHOW/grep） | F1–F4 证据 |
| turnkey 起草（§2–§6） | 本治理会话 | 本文档 |
| 专业复核（验证段） | CodeReview 专业智能体 | 对 CAS DRAFT × 现行 AuditLogService/AuditHashChain/Mapper 的交叉审 + REVOKE runbook 安全审（结论回填本文档 §9） |
| 文档/看板同步 | 本治理会话 | 复核文档 §7 行更新、log.md 追加、path-lock 提交 |
| **live 落地（gate）** | **主协调器 / Batch-2（owner 授权后）** | 落 Java、执行 DDL/REVOKE、停写窗口集成 |

单一写入者纪律：本会话**未落任何 live 源码/配置/DDL/grant**；Java DRAFT 全部内嵌本文档。

---

## 8. 交 owner 确认项（更新版，取代 PR就绪包 §10 的 1/2 两问）

> **owner 已全部裁决（2026-09-05 21:30 PDT）**：「按照建议执行」+「Q6 REVOKE 授权执行」。执行实录见 §10。

1. ~~CAS 协议~~ → **已拍板 P**（按建议执行；O 降为备选，其 READ_COMMITTED 备注保留以防回选）。
2. ~~Q6 REVOKE~~ → **已授权并执行**（21:33 前置五探针复验全清 → 21:34 执行 §2.2 → 字典双验 + 负向 1142 + 三正向对照全过，**DEF-5 收口**；§2.3 二步收紧未授权，维持现状）。实录见 §10。
3. ~~⑦ 选项~~ → **已拍板 3.2-a 维持休眠**（⑦ 关卡收口，休眠列零改动；日后要接线 hash_version 须再立 owner 决策）。
4. （承前）陈旧 seed sync 纳入停写窗口清单 — 已获 owner 认可（本轮 chat 选定），执行时机随主协调器。

---

## 9. CodeReview 专业智能体复核结论（2026-09-05 21:16 PDT，已回填）

专业复核智能体对照 live 源码（AuditLogService/AuditHashChain/AuditLogMapper/AuditLog/application.yml + 基线 DDL + qa06 验链工具）逐项审过 §2/§4/§5。**结论：本包可作 Batch-2 输入，附 3 前置**。findings 与处置：

| 级别 | 发现 | 处置（本轮已全部修订入正文） |
|---|---|---|
| **MAJOR-1** | 变体 O 在 MySQL 默认 RR 下 CAS 重试「永久失明」（selectById 快照固定 → 稳定虚假耗尽；100 并发契约测必失败） | ✅ §4.3 加 `isolation = READ_COMMITTED` + 陷阱第 1 条详述；反向强化「拍 P」 |
| MINOR-1 | REVOKE 前缺 routines/triggers/events DEFINER、host 变体、columns_priv 三探针 | ✅ §2.2 补探针块；本会话 21:18 已亲跑**全清**（三项各 0 行 / 仅一个 host 变体 / 0 列级权限） |
| MINOR-2 | P 的 `advance()` 无 !=1 防御断言，与 head==null fail-fast 不对称 | ✅ §4.2 已加断言 |
| MINOR-3 | 删除清单漏 `orderBySeq()`（唯一调用方是 selectLast） | ✅ §4.1 第 5 条已补 |
| MINOR-4 | 「即时生效」需精确为「REVOKE 语句自动同步内存缓存，已建连接下一条语句起生效；仅直改字典表需 FLUSH」 | ✅ §2.2 SQL 注释已精确化 |
| MINOR-5 | `nvl(lastHash)` 在 NULL 时产生空串链首，偏离 GENESIS 协议（外部验链工具如 qa06_restore_check.py 硬编码 GENESIS 起验会判首行 broken） | ✅ P/O 两变体均改 GENESIS 兜底 |

**核对通过项（智能体正面确认）**：实体/Mapper 风格与本仓先例同构（注解 SQL + FOR UPDATE 在 ProjectStageMapper 有 5 处先例；mapperPackage 通配覆盖新 mapper）；P 协议顺序/原子性/响亮失败/三护栏逐行等价；**audit_logs 写入口唯一性已 grep 验证（全仓仅 append L85 一处 insert，20+ 审计调用点全走 append）**；REVOKE 并集推演与 SQL 无错误；tenant.excludes 失败模式与部署顺序判断准确；§3 hash_version 事实与源码一致。

---

## 10. 执行实录：Q6 REVOKE 已执行（2026-09-05 21:33–21:35 PDT；owner 授权后本会话执行）

**执行**（root socket）：`REVOKE UPDATE, DELETE ON ipd_dev.* FROM 'ipd_app'@'127.0.0.1';` → rc=0。

**前置五探针复验**（21:33:14，移动靶复验全清）与**执行后验证**（全过）：

| 项 | 期望 | 实际 |
|---|---|---|
| 前置① mysql.db 库级 | 仍 Y/Y/Y/Y | ✓ |
| 前置② 活跃 ipd_app 连接 | 0 | 0 ✓ |
| 前置③ host 变体 | 仅 @'127.0.0.1' | ✓ |
| 前置④ 表级未覆盖表 | 仅 `_ipd_schema_history` | ✓ |
| 前置⑤ audit_logs / chain_heads 表级 | S,I / S,UPDATE | ✓ |
| 验证① SHOW GRANTS 库级行 | `SELECT, INSERT` | ✓ |
| 验证② mysql.db | Y/Y/N/N | ✓ |
| 负向（app 凭据）UPDATE audit_logs 0 行 | ERROR 1142、exit≠0 | ✓（报错 host 显示 localhost=MySQL 对 127.0.0.1 的反解显示，权限按本账户 S,I 态拒绝） |
| 正向①（app）UPDATE chain_heads 0 行 | rc=0，CAS advance 可用 | ✓ |
| 正向②（app）`SELECT…FOR UPDATE` GLOBAL | rc=0，P 锁定读可用 | ✓（锚行现读 67/68，seed 仍冻结） |
| 正向③（app）UPDATE 业务表 0 行 | rc=0，业务 CRUD 不受累及 | ✓ |
| 附录 探测污染 | 0 行 | 0 ✓ |

**生效态（db 级 SELECT,INSERT ∪ 表级）**：audit_logs=S,I（**只追加已在 DB 层强制，DEF-5 收口**）；chain_heads=S,I,UPDATE（库级 INSERT 漏入=低危残留，§2.3 未授权维持现状）；121 业务表不变；`_ipd_schema_history` 保留库级 S,I 安全网。回滚（备置未用）：`GRANT UPDATE, DELETE ON ipd_dev.* TO 'ipd_app'@'127.0.0.1';`

**对下游的影响判定**：单测全量（兄弟 21:28 全量绿 476/0/0/22）不受影响——业务表全数保有表级 U,D；失 UPDATE/DELETE 的仅 audit_logs（单测零更新路径）与 `_ipd_schema_history`（schema 工具表）。后续 live 全量回归照常由主协调器在停写窗口执行。

**Batch-2 集成态**：①②③ 按变体 **P** 实施（owner 已拍 P；§4.3 O 变体降备选、READ_COMMITTED 备注保留）；⑦ 按选项 **a** 关卡收口（休眠列零改动）。归属仍为 Wave3 Batch-2 QA-05-P2，live 落地 gate 主协调器停写窗口。
