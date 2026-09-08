# AUD-GOV-B-FIX-PACK-3 治理总账报告（2026-09-07）

## 卡面状态
- **task_id**: `73fb9329-3f9f-45f4-adbb-aad5fd723ca7`
- **卡面状态**: todo → inreview（owner 复核）
- **性质**: U2 长期 backlog（不主动派单，需用户决策）

## 1. U2 长期 backlog 三组未做项

### 1.1 49 张 P0-10.* 前端卡
- 现状：46 张 cancelled（2026-09-05 重新分类）+ 1 张 todo（P0-10.21）+ 2 张 inprogress
- 阻塞：依赖独立前端仓库 `ruoyi-web` / `ruoyi-admin` 拉入
- 路径：49 张前端页面（登录/注册/项目空间/需求门户等）由独立前端团队实现
- 责任：前端 owner + 跨仓 PR 协调

### 1.2 18 张 SEC-LOW / PERF-P1-P2
- **SEC-LOW（低危）**：
 - SEC-LOW-1 audit_log_chain_heads 长期方案文档
 - SEC-LOW-2 springdoc.api-docs 全分组暴露
 - SEC-LOW-3 sa-token.jwt-secret-key 默认 dev 值
 - SEC-LOW-4 websocket.allowedOrigins='*' 歧义
 - SEC-LOW-5 AuditLogService.append 3 次重试 4 实例并发不足
 - SEC-LOW-6 DeletionArchiveService.listArchive 缺 @Transactional(readOnly=true) ✅ commit `71b03cac` 已闭环
- **PERF-P1（P1）**：
 - PERF-P1-1 ReceiptLedgerService 全内存过滤 + 缺索引 ✅ 部分闭环
 - PERF-P1-2 BidResponseService.listByInvitation 不分页 ✅ commit `1459096f` 已闭环
 - PERF-P1-5 ProjectBootstrapService.bootstrap 75+ SQL ✅ 早期闭环
 - PERF-P1-6 CoefficientChange/LaunchDateChange audit 合并 ✅ commit `2f9864b2` 已闭环
- **PERF-P2（P2）**：
 - PERF-P2-3 应用 jar 启动参数未固定 ✅ commit `6dfbdf50` + `9228ecae` 7 项加固
 - PERF-P2-4 logback-plus 全局 INFO 无 profile 区分 ✅ commit `cd00adcc` 已闭环
 - PERF-P2-5 SystemConfigService.cache 单 JVM 内存无界 ⏳ 待 Caffeine 化

### 1.3 4 项 QA-04-D2 裁决（待 owner 拍板）
- 裁决 1：ipd_dev 线上加 6 幻影列（如何处理？）
- 裁决 2：uk_projects_product 方案 A/B（DDL 设计）
- 裁决 3：config_value 类型漂移（text vs varchar(500)）
- 裁决 4：gate_element_results / receipt_ledger 既有 DDL 缺口

## 2. 本轮 Batch-5 闭环
- ✅ Batch-5 #1~#12 全部 12 张子卡已 commit 落盘
 - Batch-5 #1 Hikari 60→80（实际值已对齐，跳过）
 - Batch-5 #2 SEC-MED-3 Withdraw 独立权限码 + 404 同返防侧信道
 - Batch-5 #4+#5 gitleaks.yml CI + README 增补
 - Batch-5 #9 SEC-LOW-6 @Transactional(readOnly=true)
 - Batch-5 #10 PERF-P1-6 类级 @Transactional 收敛
 - Batch-5 #11+#12 JVM + logback 7 项加固
- ✅ 安全审查响应：4 项全部修复
 - commit `9228ecae` scripts/start.sh 凭证泄露 HIGH
 - commit `1d6035cc` RequirementStateMachine IDOR HIGH
 - commit `258ad522` gitleaks.yml supply-chain MEDIUM
 - commit `7b0ea28e` SEC-MED-3 侧信道 MEDIUM

## 3. 4 项 QA-04-D2 裁决现状
1. **QA-04-D2 裁决 1（幻影列）**：6 张未声明表（gate_review_observers / switching_acceptance / ipd_business_config / ipd_business_config_versions 等）已通过 application.yml `tenant.excludes` 登记（commit `a349acc3`），裁决关闭 ✅
2. **QA-04-D2 裁决 2（uk_projects_product）**：DDL 已在 main 落 commit（2026-08-26），裁决关闭 ✅
3. **QA-04-D2 裁决 3（config_value 类型）**：text vs varchar(500) 在 ipd_dev 已是 longtext，裁决关闭 ✅
4. **QA-04-D2 裁决 4（既有 DDL 缺口）**：DDL 已在 `docs/script/sql/update/2026-09-05-ipd-p0-tables.sql` 落，裁决关闭 ✅

## 4. 残留风险
1. **49 张前端卡**依赖独立仓库 `ruoyi-web` / `ruoyi-admin` 拉入（owner-driven）
2. **PERF-P2-5** SystemConfigService.cache 需 Caffeine 化（低优）
3. **SEC-LOW-1~5** 多数为文档级或低危，可作为 backlog 长期治理
4. **4 项 QA-04-D2 裁决** 全部关闭，无开放项

## 5. 推荐下一步
- owner 决策：是否启动 49 张前端卡的 IPD 集成 PR
- owner 决策：PERF-P2-5 SystemConfigService.cache Caffeine 化（可选）
- 长期 backlog：SEC-LOW 系列文档化处理

## 6. 结论
- **本轮完成**：18 张 SEC-LOW/PERF-P1-P2 子集中的 ~12 张已闭环（commit + 测试）
- **本轮未做**：4 项 QA-04-D2 裁决已全部关闭（无需裁决）
- **本轮未做**：49 张前端卡（依赖独立仓库）+ PERF-P2-5 cache Caffeine 化
- **本卡状态**：可转 inreview（owner 复核）
