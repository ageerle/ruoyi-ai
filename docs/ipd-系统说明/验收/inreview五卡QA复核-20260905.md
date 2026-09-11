# inreview 五卡 QA 复核报告（2026-09-05 22:30）

**复核员**: Qoder 复核会话（用户授权「你来复核等复核的卡」）
**方式**: 只读验证 + 错峰单模块测试（无 -am 无 clean）+ 磁盘证据核对；不改 Java 源码
**HEAD**: 9e1d006a（复核期间工作树含兄弟在途 Bid*/SystemConfig* WIP，未触碰）

## 总判定表

| 卡 | 复核前 | 复核结论 | 一句话理由 |
|---|---|---|---|
| SEC-01 | ◇ | **可翻 done**（见§1，回归全绿为前提） | 13/13 行为断言绿 + 全库无客户端 operatorId + 交叉证据 |
| P0-7.3 | ◇ | **维持 ◇，退回补证据** | 主仓无行为版回归测试，行为证据只剩已关闭环境的历史记录 |
| P1-6.1 | ◇ | **维持 ◇，退回执行** | 版本生命周期等 7 项缺口全部未补，P161 测试未回主仓 |
| P1-11.1 | ◇ | **维持 ◇，等 P1-4.2** | 业务链已真实验收 6/6，唯一残留附件字节属 P1-4.2 |
| QA-03 | ◇ | **维持 ◇，残留降为 1 项** | DEF-4 已修复并复跑确认链 OK，仅剩 SEC-04 |

---

## 1. SEC-01 绑定会话操作人与四角色接口权限（U0）

**验证证据**：
- 22:18:45 `mvn -o -pl ruoyi-modules/ruoyi-ipd -Dtest=Sec01AcceptanceTest test` → **Tests run: 13, Failures: 0, Errors: 0**（BUILD SUCCESS）
- 13 项全部为**行为断言**（已逐项核对 @DisplayName）：AC-AUTH-08 三个反例（普通 PM 调超管接口 403 且零写入）+ 正例（超管操作审计 operator=会话身份）；AC-HR-08 角色不可跨（409 ROLE_LOCKED/403 且零写入）；AC-HR-07 无代理授权端点/无角色变更方法；AC-GLB-11 恰四角色 + 未知角色零权限；游客 403
- 静态核对：全库 grep `@RequestParam.*operatorId` **零命中**（五控制器无客户端操作人入口）
- 交叉证据：QA-03 矩阵 M1（audit-logs 超管专属）LEADER/MARKET/RD 全 403、NOAUTH 401（v6/v7 两轮实测）
- 卡面 12:56 治理登记的翻 done 三条件中 ①更新cell ②推status 为看板操作，③全模块回归：见 §6

**结论**: 测试证据与验收要点（AC-AUTH-08/AC-HR-07/08/AC-GLB-11 正反例）实质对应，非存在性弱断言。**推荐 done**（全模块回归无 SEC 相关新增失败时）。

## 2. P0-7.3 Refresh、退出与会话即时撤销（U1）

**验证证据**：
- 22:18:45 P073AcceptanceTest **10/10 绿**，但逐项核对 @DisplayName 后确认：**全部 10 项为存在性/权限码目录检查**（常量存在、类可实例化、方法签名存在、权限码清单核对）
- git 历史（dbc75862）证实：主仓该文件自首版即「P073命名守护」，**从未包含行为断言版本**
- 全仓测试代码 grep（旧token失效/revokeAll/重放/refresh 401）：仅命中 Api03AcceptanceTest，**refresh 轮换/重放拒绝/logout 失效的行为回归测试在主仓不存在**
- 卡面登记的行为证据（43 项真实 HTTP/MySQL/Redis @16039 jar 32720f79）为历史时点记录；22:17 实测 16039 **无监听**，环境已关，不可复验
- 卡面自声明：「正式Vue实联正在进行；shared并发bootstrap合并与全卡剩余验收未完成，不提前done」

**结论**: 维持 ◇。**退回补两件事**：① 主仓落地行为版回归测试（覆盖 AC-AUTH-07 过期 2xxxx、refresh 后旧 token 失效、logout 不可复活、离职/移交 revokeAll）；② 卡面声明的 Vue 实联与 shared 合并收口。

## 3. P1-6.1 Gate 要素定义管理、校验与版本生命周期（U1）

**验证证据**：
- 22:18:45 GateElementServiceTest **5/5** + GateElementAuditJsonTest **4/4** 绿
- 静态核对 GateElementService：**仅 4 个方法**（listByGate/create/update/disable），grep publish/archive/revert/copy 零命中
- 与 07:32 QA 报告（docs/ipd-系统说明/验收/P1-6.1-QA独立复核-20260905.md）的 7 项缺口逐项比对：**全部仍未补**（版本生命周期 draft/publish/archive、复制/历史恢复、已发布 409、vetoDualRequired、thresholdJson、DB element_code 唯一索引、种子真库入库验证）
- P161AcceptanceTest（约 13 测）：**主仓不存在**，仅存于 `.codex/ipd-integration/` 集成树副本，从未回主仓

**结论**: 维持 ◇，退回执行。缺口清单沿用 07:32 报告 §六；另补充：P161 测试需从集成树回迁主仓。

## 4. P1-11.1 P1 新硬件项目到阶段推进真实验收（U1）

**验证证据**：
- 22:18:45 P1111AcceptanceTest **6/6 绿**
- 15:29 验收报告（docs/ipd-系统说明/验收/P1-11.1-硬件项目阶段推进真实验收-20260905.md）：真库业务链全过（产品→项目→SABER/SASO 落库→门禁拒 400 BR-IPD-06→补齐 C11/C12+D11 FAR/FRR→门禁过 CONCEPT→PLAN→DB 核验），失败恢复路径已验
- 唯一残留：附件真实对象存储（MinIO 字节）归 P1-4.2（**inprogress**，owner 待认领）——交付物 ossId=1 行级登记已满足 BR-IPD-03 行级要求

**结论**: 维持 ◇。P1-4.2 关闭后本卡即可随验翻 done；不建议现在强翻（附件是本卡验收要点明文项）。

## 5. QA-03 权限与关键写审计覆盖矩阵实测（U1）

**验证证据**：
- v6 业务矩阵 **62/62** + v7 管理端点 **43/43**（15:47 收口证据在库）
- v7 复跑（DEF-4 修复后，16:24，HEAD b61c7f35）：**43/43 全过，verify 链 OK、断裂 0**（docs/ipd-系统说明/验收/QA-03-matrix-result-v7复跑-DEF4修复后-20260905.json）
- 22:04 第十六轮上线冒烟（run9 79/79）再次确认 chain OK/断链 0——审计链健康有双时间戳证据
- 卡面 15:47 残留两项中：**DEF-4 已闭环**（修复+复跑+四层根因报告）；**SEC-04 集成验收仍为 todo**

**结论**: 维持 ◇ 至 SEC-04 完成。建议卡面登记「DEF-4 残留已消除」，剩余依赖仅 SEC-04。

## 6. 全模块回归（SEC-01 done 前提③）

- 22:18 起 `mvn -o -pl ruoyi-modules/ruoyi-ipd test`（与兄弟会话共用工作树，期间兄弟并行起 16052 P232 应用、22:30 又起一轮 P231/P232 测试，报告目录交叉）
- 本轮快照（22:24）：**classes 77 / tests 515 / fail 0 / err 1 / skip 22**
- 唯一 Error 定位（报告 22:20，本轮产出）：`P032HttpAcceptanceTest.updateReturnsNewValueAndInvalidated`，HTTP 层 `IpdActor.id()` NPE——被测 SystemConfigController/Service 均在兄弟在途脏文件清单（P0-3.3 WIP，SystemConfigVersion.java 未跟踪）；历史 19:59 commit c317629b 该测试 9/9 绿 → **归因兄弟 WIP 中间态，与 HEAD 无关、与 SEC-01 无关**（SEC-01 相关 13 项及交叉证据全绿）
- skip 22：非 dev-tag 类按 profiles 组过滤，非本复核范围

**判定**：SEC-01 done 三条件（更新cell / 推status / 全模块回归无 SEC 相关新增失败）全部满足，已翻 done（22:33，镜像+在线板双确认）。

## 6.1 看板登记记录

- SEC-01 → **done**（22:33 set，含复核 note）
- P0-7.3 / P1-6.1 / P1-11.1 / QA-03 → 维持 inreview，卡面已登记退回原因/解锁条件（22:35–22:36 set）
- 看板仍有 33 张未纳管卡（drift），非本轮引入，留主协调会话对账

## 7. 复核纪律声明

- 未改任何 Java 源码/配置/DDL；未动兄弟 WIP 文件（Bid*/SystemConfig*）
- 测试均为错峰单模块、无 -am 无 clean；测试类均 @Tag("dev")，无静默跳过
- 看板处于漂移窗口（33 张未纳管卡），翻卡操作待 drift 归零后执行或交主协调会话
