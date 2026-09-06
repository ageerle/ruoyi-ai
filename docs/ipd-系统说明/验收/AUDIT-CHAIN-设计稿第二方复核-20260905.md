# 审计链顶层设计稿 — 第二方只读复核记录

- **被复核对象**：`docs/ipd-系统说明/验收/AUDIT-CHAIN-TOPOLOGY-2026-09-05-DEF9-DEF6-方案设计稿.md`（255 行，作者：审计链顶层架构师 sub-agent，zero-code mode，未跟踪文件）
- **复核人**：第二方治理会话（DEF-6 方案 A 实施者 / DEF-9 立案者 / QA-05-P1 复核者）
- **复核时刻**：2026-09-05 19:32–19:45 PDT（HEAD = `2266fd09` @19:30:14）
- **复核方式**：全程只读（`git show` / `grep` / MySQL `SELECT` / `lsof` / `ps`），未改任何代码、配置、schema、卡面
- **复核结论**：**认同设计稿的问题剖析与同根判定（L1–L5 准确、采纳 DEF-9 定性正确）；但存在 5 处与已落地事实的时序错位、1 处与 U0 风险的直接冲突、2 项设计稿未覆盖的前置缺口，且其断裂归因可被现有数据证伪。方案选型建议由 C 降为 A′（见 §7）**

---

## 1. 认同项（4 项）

| # | 认同内容 | 独立核实方式 |
|---|---|---|
| 1.1 | **L1–L5 五层同根剖析准确**，尤其「L1 是 L5 的根因（NEVER → uk 永不冲突 → 死代码）、L1 同时是 L2 的根因（自增不回填 → 空洞）」 | 与我立案 DEF-9 时的源码三铁证一致（`AuditLog.java:21` 注解 + DDL `auto_increment` + `uk_audit_seq`） |
| 1.2 | **G5「verifyChain 一次遍历出 HASH_BROKEN + GAP 两类」是必需目标** | 该项即我在 `QA-05-P1-第二方复核-20260905.md` §3.2 提出的判据分列建议；本轮 §6 再次用数据证明混同报 broken 会导致归因错误 |
| 1.3 | **§6.2「严禁的次序」清单有工程价值**（如「先上 DDL-1 后上 J-1 → 写路径无 seq 值」） | 与我 DEF-6 实施时「护栏必须在重试循环之外」同类：次序错误会产生静默失效而非显式报错 |
| 1.4 | **§8.1 边界声明克制**（不抢推卡面、不 commit、不动兄弟在途 37 M / 52 ?? 文件） | 与本会话同一并发纪律；本轮我也未抢写镜像卡面 |

---

## 2. ⚠️ 时序错位（5 处）：设计稿以「DEF-6 尚未实施」为前提，实际已落地并提交

设计稿写作时点早于（或未读取）DEF-6 收口事实。**现态核查（19:35 PDT）**：

| 项 | 设计稿表述 | 磁盘/DB 现态 | 证据 |
|---|---|---|---|
| 2.1 | 「DEF-6（**建卡待裁**）」 | DEF-6 已实施完毕、卡面已 `✅ done` | 镜像 L327 状态格 `✅ 方案A收口: ddl+护栏+19测全绿+p091 74/83断=0`（兄弟 R32 已第二方确认） |
| 2.2 | §8.1「不读未写未 add 的 SQL（4 个 untracked SQL：… **audit-payload-longtext**）」 | 该 SQL **已提交**，非 untracked | `git log -1 -- docs/script/sql/update/2026-09-05-ipd-audit-payload-longtext.sql` → `2e50bb71`；`git status --porcelain` 该路径**空输出** |
| 2.3 | §4 方案 A「④ json 列**保留**（不加 longtext）」；方案 C「DDL-4: audit_logs 去 before_data/after_data」 | 两列**已是 `longtext`** | `information_schema.columns` → `before_data longtext YES` / `after_data longtext YES` |
| 2.4 | §6.1「P0-9.1 PARTIAL 残留 **8 项** FAIL \| **100% 归因 DEF-6**」 | 残留 **9 项**，归因**单一空洞**（seq 609→1309），与 DEF-6 无关 | run7（19:07）JSON：带载荷断裂行 **0**；DB `gaps=1`、`rows_in_hole(seq 610..1308)=0` |
| 2.5 | §6.1「方案 C 闭环后**预计全部转 PASS**」 | **不成立** | chain_heads 只保证**未来写入**不产空洞（设计稿方案 A 自承「历史空洞仍需手动处理」）；存量空洞须补齐 seq 或改断言语义，二者设计稿均未列入 J-1~J-7 |

**对方案 C 的具体影响**：DDL-4 的迁移起点是 `longtext` 而非 `json`，故其「json 列被规范化是已知根因 → 再保留无意义」（§5.1 理由 5）的论证对象已不存在；我已执行的 longtext ALTER 成为方案 C 的**既有中间态**（不是浪费：它当场止住了带载荷行断裂，run5/6/7 带载荷断裂均为 0），但 owner 需知晓方案 C 会**再迁一次**（longtext → 指针列 + payloads 表）。

---

## 3. ⚠️ 与 U0 风险直接冲突：设计稿「Hikari maxPoolSize=80 强推荐同 PR 上线」

设计稿 §6.1 把 `Hikari maxPoolSize=80`（P1-3 prod 修复）列为**「与本批强推荐同 PR 上线」**，§6.2 更把「先上 BCrypt cost=10 后上 maxPoolSize=80」列为严禁次序，理由是「池未升 → 排队更严重」。

**该建议只考虑了池侧，未考虑 DB 侧硬上限，与我的 U0 复核结论直接冲突**：

```
现态（19:33 PDT 实测）
  SHOW VARIABLES LIKE 'max_connections'        → 151
  processlist GROUP BY user                    → ipd_app 40 | root 1 | event_scheduler 1
  lsof -nP -iTCP -sTCP:LISTEN | grep java      → 4 个实例（16039/16044/16045/16050）
  锚点等式：4 × HikariCP 默认 10 = 40 ≡ 实测 ipd_app 40（且全为 Sleep 空闲态）
            → 证明常驻连接数由池预留决定、与业务负载无关
  HEAD 基座配置：application.yml:127 maxPoolSize: 40（d9c24227 引入，R8-P0-1~4 未处置）

外推
  基座 40 × 4 实例 = 160 > 151  → ERROR 1040 Too many connections（尚未兑现，因 4 实例全为旧 jar）
  若按设计稿再上 80 × 4 实例 = 320 > 151 → 超限 169 个，且 ipd_app 非 SUPER 拿不到保留连接
```

`Too many connections` 比池排队雪崩**更严重**：新实例根本起不来、运行中实例无法取连接、只有 root 运维通道可用。**故「把新配置推到全部实例」这一必然动作，必须先在 P1-1 三选项中决策**（基座降 20 / 先提 max_connections ≥250 并评估内存 / ipd-local 显式配池使基座只兜底），设计稿的 80 建议应**挂起到该决策之后**。

---

## 4. ⚠️ 新发现 A（设计稿未覆盖）：基线 DDL 未回写 → 新环境建库会完整复发 DEF-6

```
docs/script/sql/update/2026-09-04-ipd-p0-tables.sql（基线 DDL，建库唯一入口）
  L490  seq           bigint      not null auto_increment
  L496  before_data   json        null          ← 仍为 json，未回写
  L497  after_data    json        null          ← 仍为 json，未回写
  L507  unique key uk_audit_seq (seq)

ipd_dev 库现态：before_data / after_data = longtext（2e50bb71 的迁移 SQL 已 apply）
```

**危害**：迁移 SQL 只对**已存在的库**有效。任何新环境（CI、他人本地、生产首次部署）从基线 DDL 建库都会得到 `json` 列 → **DEF-6 完整复发**，且因新库不需要跑 `update/` 目录下的迁移脚本，无人会察觉。这使 DEF-6 的 `done` 状态**只在当前活库成立**。

**建议**：①回写基线 DDL 两列为 `longtext`（最小改动、与 QA-04-D2「漂移列回写」既有实践一致——该 commit `a52035aa` 已回写过其他漂移列，本次遗漏）；或 ②在基线 DDL 该两行加注释指向迁移脚本。若采方案 C，则基线 DDL 应与 DDL-3/4 同 PR 一次性写到位。**此项应作为 DEF-6 的收口补充条件，否则 DEF-6 不宜视为完全闭环。**

---

## 5. ⚠️ 新发现 B（设计稿 D1 的前置缺口）：`hash_version` 列已写入 62 行 v2，但应用层完全不读

```
库现态：SELECT hash_version, COUNT(*), MIN(seq), MAX(seq) FROM audit_logs GROUP BY hash_version
        → NULL(legacy-v1) 601 行 (seq 2..1363) | 2(canonical-json-v2) 62 行 (seq 6..67)
        列注释：'NULL=legacy-v1,2=canonical-json-v2（QA-04-D2 回写线上形态）'

代码现态：AuditHashChain 已具备双版本能力
        L22 ACTIVE_CANONICAL_VERSION = 1
        L74 canonicalV2(...)
        L92 canonicalByVersion(int version, ...) → switch 1/2，default 抛 IllegalArgumentException
        但 grep -rn "hashVersion\|hash_version" ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/ → 零命中
        → AuditLog 实体无该字段、Mapper 不映射、verifyChain 与 rebuildChain 均不读版本
```

**推论（演绎，无需新实验）**：`verifyChain` 恒用 v1 重算，而这 62 行**不在 broken 列表**（broken 仅 `[1309]`）→ **它们的 `curr_hash` 实际是 v1 形态，`hash_version=2` 是误标**（QA-04-D2「回写线上形态」写了列值但未重算 hash）。

**对设计稿 D1 的直接影响**：D1 推荐 **(c)「新行 v2 + 历史行 v1 双版本验」**。但一旦按 (c) 实现「读 `hash_version` 分派校验」，这 62 行会**在落地瞬间全部判断裂**（标记 v2 → 用 `canonicalV2` 重算 → 与库中 v1 hash 不符），制造 62 行假断裂，比当前 1 行空洞严重得多。

**故 D1 的前置条件应为**：①先修正这 62 行的 `hash_version`（改回 NULL/1，或按 v2 重算其 `curr_hash`）；②`AuditLog` 实体与 Mapper 补 `hashVersion` 字段（当前应用层根本读不到该列）；③补一条「版本标记与实际 hash 协议一致性」的契约测。**三项未做前不应启动 (c)。**

---

## 6. ⚠️ 证伪：兄弟 R32 对 `broken=[1309]` 的归因不成立（该项影响 DEF-9 优先级判断）

`log.md` 未提交段 R32（2026-09-06 02:25 CST）称：

> 「verify broken=[1309] … seq 1309.prev_hash=295eca2f37e5=seq 609.curr_hash（完美衔接）。**仅 curr_hash 由兄弟实例 16039 p191.jar（09:35 build）旧算法生成**，与本仓 def6.jar 不一致。带载荷审计行断裂=0，属跨实例并发 race 瞬时产物，**非 DEF-6 payload 缺陷**。」

「非 DEF-6 缺陷」「带载荷断裂=0」两点**与我一致且正确**；但「仅 curr_hash 旧算法生成」的归因**可被其自身数据证伪**：

| 证据 | 实测 | 含义 |
|---|---|---|
| 1309 链接自洽 | `prev_hash = 295eca2f37e5`，与 seq 609 的 `curr_hash` **相等**（`link_ok=1`）、`no_payload=1` | 判据②成立；无载荷 → 判据①的输入不含 payload |
| **R32 自己跑了 rebuild×2** | `fixed=22`、`fixed=19` | rebuild **已重算 curr_hash**；若归因于 curr_hash 算法，重算后应转 OK |
| rebuild 后仍断裂 | `broken=[1309]` 未变 | → **curr_hash 不是原因**，归因被证伪 |
| 空洞确凿 | `rows_in_hole(seq 610..1308) = 0`；`gaps=1` | 609 之后 expectSeq=610，实际下一行 seq=1309 |
| 演绎 | `!expectSeq.equals(log.getSeq())` = `!610.equals(1309)` = **true** | **第三判据必然触发**，与 curr_hash 取何值无关 |

**结论**：1309 断裂的唯一原因是 **verifyChain 第三判据（seq 严格连续）遇上存量空洞**，即 DEF-9 第③面，与「旧算法 jar」「跨实例 race」均无关。这也解释了为什么 rebuild 治不好它（rebuild 只写 prev/curr 两列、不改 seq）——与我的 A7 定论一致。

**附带更正 R32 两处表述**：①「rebuild-chain ×2 **幂等**：fixed=22, fixed=19」——22 ≠ 19，不满足幂等（幂等应第二次 `fixed=0`）；可能是期间有活跃写入（16045 于 19:05:58 重启后有 LOGIN 行）产生新的不一致，但用「幂等」描述不准确。②「19 测全绿：… `AuditChainSymmetryTest` **8/8**」——我上轮实测该测试类为 7 测，请核对是否兄弟新增了用例（若是，属正常演进，建议注明）。

---

## 7. 方案选型第二方意见：建议由 **C 降为 A′**（G2 缺乏业务需求支撑）

设计稿推荐方案 C（5~7 人天、破坏性协议变更、存储 2×、DDL 迁移窗口长）。其相对方案 A 的**增量收益集中在 G2「json 载荷可索引」**（`WHERE JSON_EXTRACT(after_data,'$.key')=?` 命中），因为 G1/G3/G5/G6 由 **chain_heads 锚表 + verifyChain 判据分列**即可达成，与是否拆 payloads 表无关（设计稿方案 A 自评已覆盖 G1✓/G2✓/G3部分/G4✓/G5✓/G6✓）。

**质询**：IPD 当前是否有「按审计载荷内容检索」的验收标准或产品需求？我在 AC-AUD 系列中看到的是**链自洽与只追加**（AC-AUD-01/03），未见载荷内容检索需求。若无，则 G2 属**目标态自设**，为它付出「主表去载荷列 + 新增 payloads 表 + 6 处写点双写 + 读侧全部改 join + 存储 2×」的代价不成立。

**建议方案 A′**（= 设计稿方案 A 的 ①②③⑤ + 我已落地的 longtext 与 requireJson 护栏）：

| 项 | 内容 | 状态 |
|---|---|---|
| ① | `audit_logs.seq` 去 `AUTO_INCREMENT` | 待做（需停写窗口，与 ②③ 同 PR） |
| ② | 新增 `audit_log_chain_heads` 单行锚表 + 登记 `tenant.excludes` | 待做（需停写窗口，与 ①③ 同 PR） |
| ③ | `append` 改为 chain_heads 原子递增（CAS）→ 去 `insertStrategy=NEVER` → 删除死代码 `catch (DuplicateKeyException)` 整段 | 待做（需停写窗口，与 ①② 同 PR） |
| ④′ | 载荷列 **保持 longtext**（已 apply）+ `AuditEventData.requireJson` 护栏（已提交） | **已完成** |
| ⑤ | `verifyChain` 拆 HASH_BROKEN / GAP 两类返回 | **已完成**（owner Q2 采纳 A′ 后落地：Service+record `0596c957`/`75fa56fb`、Controller 四态 `ba5c329d`、契约测 `AuditChainGapHashSplitTest` 5 例 `6d3eccf9`；绿门 24/24 Skipped=0，详见下方落地追记） |
| ⑥ | **基线 DDL 回写 longtext**（§4） | **已完成**（`dd361ef3`，基线两列 json→longtext + 8 行规范化注释） |
| ⑦ | **`hash_version` 62 行修正 + 实体补字段 + 一致性契约测**（§5） | 待做（owner Q3：交主协调器，涉 UPDATE append-only 历史行） |
| ⑧ | **存量空洞 seq 610..1308 处置**（补齐或改断言语义），否则 P0-9.1 仍 9 项 FAIL | **已实现「改断言语义」**（owner Q5 采纳「改断言不补行」：P0-9.1 脚本 3 处 GAP 硬门降级为告警——chain 门 / 断裂数门 / seq 零跳号门；静态验证 ALL PASS，详见下方 Q5 落地追记。**live re-run 待协调窗**，未伪称收 done） |

A′ 覆盖 G1/G3/G5/G6 + G4，**不含 G2**；G2 建议拆为独立卡，待确认有载荷检索需求后再排期（届时 payloads 拆表可作为 A′ 的增量演进，不需回退 A′）。

### ⑤ 落地追记（2026-09-05 20:00–20:05 PDT，owner Q2 采纳 A′ 后）

- **零破坏双出口**：`verifyChain()` 语义完全不变（返回合并去重升序 `List<Long>`），新增 `verifyChainDetailed()` 返回 `AuditChainVerifyResult(hashBroken, gaps, total)` record，`verdict()` 四态 `OK`/`HASH_BROKEN`/`GAP`/`BROKEN`。`AuditChainSymmetryTest` 6 处 `verifyChain()` 断言**一行未改仍全绿**（8/8）= 双出口零破坏验证点。
- **Controller 四态化**：`GET /api/v1/audit-logs/verify` 保留 `broken`（=mergedBroken，兼容 P0-9.1）+ 新增 `hashBroken`/`gaps`/`total`，`chain` 由二值升四态。**硬门不放宽**：有 GAP 时 `chain` 仍非 `OK`，断言 `chain==OK` 的脚本仍 FAIL，分列只让 FAIL 可归因。
- **契约测 `AuditChainGapHashSplitTest`（5 例，@Tag("dev")）**：核心场景②精确复刻 seq 1309 形态（链接自洽 `prev`=前一实际行 `curr`、但 seq 跳跃）→ `verdict()=GAP`、`hashBroken` 空，锁死「rebuild 治不了的 GAP 不再被误归因为哈希问题」；场景⑤同一 seq 落两桶验证 `mergedBroken` 的 `distinct()` 不可省。
- **绿门**：`mvn -o -pl ruoyi-modules/ruoyi-ipd`（错峰+单模块+无 `-am` 无 `clean`）两轮 24/24 Skipped=0（GapHashSplit 5 + Symmetry 8 + AuditPayloadJsonGuard 7 + GateElementAuditJson 4）；`javap` 字节码确认 `AuditLogController.class` 含四态字段（消除首轮 `Nothing to compile` 盲点）。
- **归属**：本轮代码在工作树未提交期间被兄弟会话 `git add -A` 裹挟进 R8X-CONT 系列（Service `0596c957`、record `75fa56fb`、Controller `ba5c329d`、新测 `6d3eccf9`），各 commit message 均未提 verify 四态语义 → 本追记补登归属供溯源。
- **边界**：Controller 为透传映射，四态逻辑由 Service 层 `verdict()` 断言覆盖；真 HTTP 契约留待部署后 P0-9.1 重跑——**单测绿 ≠ HTTP 闭环**，不伪称完整验收。

### Q5 落地追记（2026-09-05 20:28 PDT，owner Q5 采纳「改断言语义、GAP 降级告警」后）

- **改点（3 处 GAP 硬门降级，`P0-9.1-业务链真实验收-20260905.py`）**：① `verify_state` 增读四态 `hashBroken`/`gaps` 分列；② 新增 `chain_gate` 判据助手——四态 Controller 为权威（`chain∈{OK,GAP}`→PASS），旧二态 jar（如 run7 的 `def6i.jar`，`chain="BROKEN"`、无分列键）回退脚本侧 DB 归因（`wj+rest`=真哈希断裂、`gaphead`=历史空洞）；③ `chain_checks`/L7 的 `chain=OK`+`断裂数=0` 两硬门改为「无哈希断裂」+「哈希断裂数=0」，GAP 降级为 `WARN`（不计 FAIL）；④ L7 `seq 零跳号` 按**前驱是否越基线**切分：前驱 `p<=seq0`=历史空洞（清库致 AUTO_INCREMENT 计数器跳变，如 609→1309）降级告警，前驱 `p>seq0`=本轮新漏行仍 FAIL。
- **防假绿（核心，回应 AGENTS.md「绿的是契约不是既有实现」）**：真哈希断裂（DEF-6 载荷 `wj`、未归因 `rest`、四态 `HASH_BROKEN`/`BROKEN`）与**本轮新增漏行**（`gap_new`）仍判 FAIL；仅**已知历史空洞**降级告警。不伪造补行，尊重 AC-AUD-01 只追加语义。
- **静态验证 ALL PASS**（`data/coding-harness/artifacts/q5_static_verify.py`，gitignore 本地件，不触共享库/不跑 HTTP）：① `py_compile` 语法门 PASS；② `ast` 抽取真 `chain_gate` 源跑 7 例判据矩阵全 PASS（run7 旧 jar 单空洞→PASS、DEF-6 载荷/未归因/四态 HASH_BROKEN/BROKEN→FAIL、四态 OK/GAP→PASS）；③ sqlite 仿真 LAG 跳号切分：run7 形态 `gap_hist=1 gap_new=0`（历史空洞降级）、注入删 1315 后 `gap_hist=1 gap_new=1`（真漏行仍被捕获）。
- **对 run7 证据的推演**：run7 的 9 项 FAIL 全为单一 seq 1309 历史空洞驱动（`带载荷=0 空洞后首行=1 未归因=0`）→ Q5 改后 3 处门均降级为 PASS/WARN，同库态 re-run 应得 **83/83 ALL PASS**（check 条目数守恒，未增删）。
- **边界（不伪称完成）**：脚本改断言为**代码层**；真 HTTP re-run 需 live 实例 + 会 mutate 共享库（改密/删除请求），按单写者 + 热窗纪律**留待主协调器协调窗执行**，本会话不擅自跑、不翻 P0-9.1 板卡（兄弟 `f9442b62` 已在镜像标「业务腿全绿」，本改使该标记获得断言层支撑）。**静态绿 ≠ HTTP 闭环**。

---

## 8. 交 owner 的决策清单（按时序依赖排序）

| # | 决策项 | 阻塞关系 | 我的建议 |
|---|---|---|---|
| **Q1** | **P1-1（U0）连接预算**：基座 `maxPoolSize` 40 → 20？还是先提 `max_connections` ≥250？ | **阻塞「把新配置推到 4 实例」与设计稿的 80 建议** | 基座降 20（与 prod 现值一致，4×20=80，53% 水位）；dev 若需 40 保留 dev 独有覆盖 |
| **Q2** | **方案选型**：C（设计稿推荐）还是 A′（§7）？ | 决定 DEF-9/DEF-6/QA-05-P2 三卡的实施范围与人天 | A′；并把 G2 拆独立卡待需求确认 |
| **Q3** | **D1 hash 协议**：若选 A′，是否仍切 v2？ | 依赖 §5 三项前置 | 先修 62 行误标 + 补实体字段，**再**谈双版本验；否则 (c) 落地即制造 62 行假断裂 |
| **Q4** | **DEF-6 收口补充**：基线 DDL 回写（§4） | 决定 DEF-6 能否视为完全闭环 | 回写基线两列为 longtext，与 QA-04-D2「漂移列回写」同实践 |
| **Q5** | **存量空洞处置**：补齐 seq 610..1308 还是改 P0-9.1 断言语义？ | 决定 P0-9.1 能否从 ◐ 收 done | **owner 已采纳「改断言语义」且已落地**（GAP 降级告警、不补行；脚本改 + 静态验证 ALL PASS，live re-run 待协调窗，详见 §7 Q5 落地追记） |
| **Q6** | **DEF-5**（库级 grant 架空表级只追加） | 独立，但设计稿建议同 PR 前解决 | 仍待处置，本轮未动 |
| **Q7** | **16050 实例**：`ruoyi-admin-def6.jar`（18:48:20 启动，不含护栏且 `ruoyi-ipd`/`ruoyi-system` 双双陈旧） | 对已改 longtext 的库是**活的 DEF-1 fail-fast 缺口** | 停掉或换含护栏的 def6i.jar；根因是部署链从陈旧 ~/.m2 解析模块 |

---

## 9. 可复现命令清单（本轮复核全部只读）

```bash
cd /Users/mac/Documents/ruoyi-ai
M=.codex/ipd-dev/software/mysql-8.0.46-macos15-arm64/bin/mysql
C=.codex/ipd-dev/config/mysql-client.cnf        # root, SOCKET

# §2 时序错位
git log --oneline -1 -- docs/script/sql/update/2026-09-05-ipd-audit-payload-longtext.sql
git status --porcelain -- docs/script/sql/update/2026-09-05-ipd-audit-payload-longtext.sql
sed -n '488,510p' docs/script/sql/update/2026-09-04-ipd-p0-tables.sql          # 基线仍 json
grep -n "maxPoolSize\|connectionTimeout" ruoyi-admin/src/main/resources/application.yml

# §3 U0 连接预算
$M --defaults-file=$C -N -e "SHOW VARIABLES LIKE 'max_connections';"
$M --defaults-file=$C -N -e "SELECT user,COUNT(*) FROM information_schema.processlist GROUP BY user;"
lsof -nP -iTCP -sTCP:LISTEN | grep -i java
for p in <PID...>; do ps -o lstart=,command= -p $p | tr ' ' '\n' | grep -i '\.jar'; done

# §4/§5 两项新发现
$M --defaults-file=$C -e "SELECT COLUMN_NAME,DATA_TYPE FROM information_schema.columns
  WHERE table_schema='ipd_dev' AND table_name='audit_logs'
    AND COLUMN_NAME IN ('before_data','after_data','seq');"
$M --defaults-file=$C -e "SELECT hash_version,COUNT(*),MIN(seq),MAX(seq)
  FROM ipd_dev.audit_logs GROUP BY hash_version;"
grep -rn "hashVersion\|hash_version" ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/   # 零命中
grep -n "canonicalV2\|canonicalByVersion\|ACTIVE_CANONICAL_VERSION" \
  ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/util/AuditHashChain.java

# §6 证伪归因
$M --defaults-file=$C -e "SELECT a.seq,LEFT(a.prev_hash,12),LEFT(b.curr_hash,12),
  (a.prev_hash=b.curr_hash) link_ok,(a.before_data IS NULL AND a.after_data IS NULL) no_payload
  FROM ipd_dev.audit_logs a JOIN ipd_dev.audit_logs b ON b.seq=609 WHERE a.seq=1309;"
$M --defaults-file=$C -N -e "SELECT COUNT(*) FROM ipd_dev.audit_logs WHERE seq BETWEEN 610 AND 1308;"   # 0

# DEF-6 疗效探针（带空格载荷 = json 时代规范化产物）
$M --defaults-file=$C -e "SELECT CASE WHEN seq<=609 THEN 'A_历史区' WHEN seq<1309 THEN 'B_空洞区'
  ELSE 'C_空洞后' END bucket, COUNT(*) n, MIN(seq), MAX(seq), MIN(create_time), MAX(create_time)
  FROM ipd_dev.audit_logs WHERE after_data LIKE '%, \"%' OR after_data LIKE '%\": %' GROUP BY bucket;"
# → 73 行全在 A 区（seq 23..502），create_time 最晚 2026-09-06 07:51:52 CST = 16:51:52 PDT，
#   均早于 ALTER apply 时点；C 区零命中 → 改列型后无新毒行
```

---

## 10. 边界声明

- 本轮**未修改**任何代码 / 配置 / schema / 卡面 / 镜像；**未**起停实例；**未**执行 rebuild；**未** commit。
- 未抢写镜像卡面：设计稿 §8.4 明确「卡面留给主线程统一翻」，且 AGENTS.md「并发写单一写入者」要求镜像由主协调会话串行写；故本文档以**证据交付**形式提交，§4/§5 两项新发现的建卡素材已备齐（现象/根因/证据/修复方向/验收判据），可由主协调器直接挂卡。
- 唯一 DB 交互为 `SELECT` / `SHOW`；无写入副作用（与上轮 QA-05-P1 复核不同，本轮未调用任何 HTTP 端点）。
