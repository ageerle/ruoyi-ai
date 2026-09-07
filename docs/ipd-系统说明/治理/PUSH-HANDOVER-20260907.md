# Wave14→Wave25 本地待 push 清单（2026-09-07）

## 规模

- 本地 main 领先 origin/main：**146 commit / 524 files / +453 / -86591**
- **主因是兄弟流高频推送穿插**，非本会话单批产出（5 个本会话直接 + ~140 兄弟流）

## 本会话直接产出 commit（5 个，a11y 战役核心）

```
3e1860b0 docs(ipd,WAVE23): a11y 真仓全清 violations=0 + V12 a11y 全项闭环登记
f1fff833 docs(ipd,WAVE22-B): 前端 30 测失败终局定论
ba51c669 test(ipd,WAVE25): 移动端深化视觉走查产物入库
8e423e5a test(ipd,WAVE24-QA05-FE): 49页矩阵核心页前端压测
46d350b7 test(ipd,WAVE21-A-VERIFY): axe-scan 端到端实测闭环
```

## a11y CI 阻断自验证前提

本地 main 推到 origin → a11y-ci.yml + docs-link-check.yml 在 GitHub 注册 → STRICT_A11Y secret 生效 → owner 推任一 PR 时首次触发 ERROR 阻断。

## 全部 146 个待 push commit 清单

```
$(cat /tmp/wave-commits.txt)
```

## push 命令（owner 决策后执行）

```bash
git push -u origin main
# 推完查远端 workflow：
gh workflow list -R wilson323/ruoyi-ai --all | grep -E "a11y|docs-link"
# 应见 a11y-ci.yml + docs-link-check.yml（已配置 STRICT_A11Y=true）
```

## 推完后验证链

1. a11y-ci 首次运行：`gh workflow run a11y-ci.yml -R wilson323/ruoyi-ai` → 期望 violations=0 + exit 0（WARN 默认未升级）
2. 设 STRICT_A11Y=true 已就位 → owner 推任一含 a11y violation 的 PR → 应被阻断
3. docs-link-check 同理（DDL 已就位 47 → 41 ERROR）

## owner 决策点

- [ ] 是否推 main（含兄弟流高频 commit 风险评估）
- [ ] 是否开启 STRICT_A11Y 30 天观察期
- [ ] 是否删 ZK-IPD 备份 `styles.css.bak-a11y-20260907`（1 周观察期后）
</content>
ba51c669 test(ipd,WAVE25): 移动端深化视觉走查产物入库——5 页截图+JSON+溢出检测脚本模式
8e423e5a test(ipd,WAVE24-QA05-FE): 49页矩阵核心页前端压测——10页×16并发×20轮 3200请求 0失败 P95<325ms
106de362 test(ipd,qa08): 16039 登录端到端解锁——login() helper 读 bootstrap 凭据真取 Bearer; AC-AUTH-02 升真证(首登未改密账号调业务口实测 20003 拦截)/08(超管口 PM 403+admin 200 正对照)/09(PM 跨组项目详情 403 不泄露) PASS, AC-AUD-02 审计链 verify 真调用 chain_ok=true PASS; AUTH-11 协同组夹具不可证恒 PARTIAL; AUD-04/05 审计列表口 PM/leader 403 属实 PARTIAL; PASS 52→55 (20.9%→22.1%), FAIL 2→1; 夹具处置: ipd-admin must_change_pwd 置 0 作正对照(其余 demo 保持首登态当 20003 探针), 剩余 44 BLOCKED 全为 UI 未实现类不可由登录解锁
3e1860b0 docs(ipd,WAVE23): a11y 真仓全清 violations=0 + V12 a11y 全项闭环登记——16->5->0 三连修 + 兄弟流前期成果确认
f1fff833 docs(ipd,WAVE22-B): 前端 30 测失败终局定论——双命令姿势根因(--dom 缺失+根目录跑丢 plugin-vue) + api/ipd 16 文件 71 测全绿实证 + 兄弟流 0 bug 结论
bafbce03 docs(ipd,WAVE22): a11y 闭环 16->5 + 前端 30 测失败归因翻案(跑错命令非代码bug)
d1b4eedf docs(ipd,log): 晚·七节登记——MCP 连接失败全量根修(repowise 幽灵条目撞别名+裸命令改绝对路径/kanban shim 健康瞬态/evox open -a 拉起/episodic+mempalace 15min 自动重试), 唯一 JSON 配置漂移即 Connection closed 的结论+持久记忆落盘
9c9fe99a test(ipd,SEC): Agent D 场景纵深两件——PersonSyncSecurityScenarioTest(7测)+BidAdminAssignSecurityScenarioTest(10测), 26/26 绿, 零产线改动
1e37d65a test(ipd,qa08): AC 验收脚本落地——qa08-ac-acceptance.py(表名笔误 receipt_ledgers→receipt_ledger 等修正+DDL 落库核查)+6 JSON 报告(FAIL 7→0)+2 SQL 补 ROLLBACK 注释; PASS 仍 20.9% 属需 16039 登录端到端项, 登记待端到端
89806ee3 chore(ipd,R6-reconcile): 看板镜像根治——plan.md 补录 R6 段+141 key 全集入镜像(4 活动卡零翻转); manage.py KEY 正则大扩展+3 列修复分支; batch-sync-commits.py normalize_key+importlib 同源正则+--card-filter; 捎带兄弟流在途镜像行(混文件声明)
9e9e2af8 fix(ipd,SEC-RESIGN-GROUP): 缺陷 D-1 修复——PersonService.resign 补 assertSameGroupOrAdmin 跨组守卫(与 rehire/unbind 同款, 本人放行对齐 controller isHr-or-self 语义, 堵 GROUP_LEADER 跨组冻结他人水平越权); PersonGroupBoundarySecurityScenarioTest 增 3 测锁定(跨组拒绝零副作用/本人放行/同组+超管旁路), 类 12/12 绿
46d350b7 test(ipd,WAVE21-A-VERIFY): axe-scan 端到端实测闭环——ZK-IPD LIVE 首扫 5 serious + 基线报告入库
d8ade0e9 feat(ipd,WAVE21-EXT): batch-sync-commits.py 卡号映射扩展收口——dry-run 实测通过 + 操作手册
4f75ce3a feat(ipd,WAVE21): A11y 自动化 CI + 78 张卡号批量 reconcile 映射——蜂群 429 死亡兜底收口
4feb8ec9 feat(ipd,ROOT-R2-P0-2-EXT): Notification 中间件 EMAIL/WebSocket 真实通道真实现——JavaMailSender + SimpMessagingTemplate
47278766 docs(ipd,GOVERNANCE-6): log.md 登记 4 agent 安全+V12 走查闭环——PersonService 同组/在职/资源、PersonSyncService 6 道安全审查、BidP231Controller 注解角色+可见性、V12 走查报告; 7 道安全审查 100% 闭环; 累计 ~60+ commit
d3deb74a fix(ipd,SEC-PersonSync-07): PersonSyncService 6 道安全审查闭环——audit JSON+defaultProcess bypass+synchronized+mask+visibility
bd3f3b0c fix(ipd,SEC-Person-HIGH): PersonService rehire/unbindWecom 跨组守卫+在职守卫+DTO @Size——HIGH 授权闭环
2b0beb11 feat(hook,W20-A): 注册 ddl-field-usage-lint.cjs PostToolUse matcher——W19-B 修复核心 bug+实施合规+JSON 合法+备份 .wave19b.bak 就位后落盘；PostToolUse 第3项 Write|Edit|MultiEdit → ddl-field-usage-lint.cjs（3s 超时），命令带 CLAUDE_PROJECT_DIR 兜底，无项目目录时回退 HOME 解析节点脚本
dc0f0adc fix(ipd,ROOT-R3-P0-1-EXT): DeletionRequestService + BonusPoolService 守卫 fail-closed 同步修复
e1d6f90f fix(ipd,P2-3.1-SEC): BidP231Controller 2 道 HIGH 授权闭环——注解层 requireProjectCreator+Catalog OPERATION_BID_INVITATION_CREATE; service 入口 ProjectMapper 校验可见性(NOT_FOUND/FORBIDDEN/SUPER_ADMIN|GROUP_LEADER 跳过); P231 验收测 9→12 全绿
fdc8679d docs(ipd,GOVERNANCE-5): log.md 登记 7 agent 并发 + 4 道安全审查闭环——R6/P1剩/P2/49页/18 ZK矛盾/AC真验证/qa04真问题 全部落地; 累计 ~55 commit; 4 道安全发现待兄弟流后续
8075e11a feat(ipd,P1-REMAINING): P1 应做剩余 6 项 + 端点契约 4 大类缺口——8 张子卡
403b6ec6 fix(ipd,QA-04-F): 5 TABLE/ENTITY 孤儿表映射闭环——BonusAllocation/LegacyImport/RequirementPool 3 entity新件+person_roles注释占位删除+sys_oss跨模块共享表降级WARN
ef431c90 feat(ipd,P2-3.1): 招标单校验型创建 DTO+独立 validator+独立 controller——避 BidInvitationService 兄弟流活跃区
9c6a2fe3 fix(ipd,QA-04-B+E): 12 REQUIRED_COLUMN_UNMAPPED entity补齐 + 2 TYPE_MISMATCH 改import——Contribution/ProjectScore/NegativeFeedback/SwitchingAcceptance补v1遗留列; ProjectMember删handoverRole/note错表字段; IpdBusinessConfigVersion java.util.Date改import Date
17fbcbb4 fix(ipd,R6): post-commit hook status 误判——body 描述里的 wip 关键字不应覆盖 subject 的 feat 强信号
8ca2da9a feat(ipd,P2-2.3+P2-2.1): PersonSyncService 同步任务状态/重试/异常项回补 + 幂等键重放保护
b377ffc8 feat(ipd,R6): update_task 流程系统化——post-commit hook + batch sync + 28 张卡面 reconcile
8501cf76 fix(ipd,QA-04-A): 22 COLUMN_NOT_IN_DDL 幻影列补齐——21 张表/22 列 ALTER（project_members/handoverRole note 走 entity 删除）
772b0f1a feat(ipd,P2-1.3): 离职冻结/复职/企微解绑联动——PersonService 3 公共方法 + PersonController 3 端点 + 12 测覆盖
c87bcb82 docs(ipd,GOVERNANCE-20260906): ZK 矛盾真修订·勘误登记表——18 commit 全部落地的事后台账
29a80650 fix(ipd,CONSISTENCY-18): dualPmProject 分支 C14 vs 附录 D.6.7 决策表勘误注
bf4b3fd4 fix(ipd,CONSISTENCY-17): 奖金池基数 vs 达成率分子易混点 v3 §3.9 增同步说明
13c92065 fix(ipd,CONSISTENCY-16): 游客负责人字段 v3 §3.10 增「源文档待修订」同步说明
cb7e3afb fix(ipd,CONSISTENCY-15): 毛利率门槛 v3 §3.6 显式列入——S≥40/A≥30/B≥25
9398830c fix(ipd,CONSISTENCY-14): G1-1 客户验证决策表 I3 勘误注——「≥5 家不按等级分档」保持
9fa56581 fix(ipd,CONSISTENCY-13): 直接上级 V3.1 §5.2/§5.3 勘误注——v3 §3.6 RACI 增「A5 决策产品组长」同步说明
2ea15a35 fix(ipd,CONSISTENCY-12): G2 否决项数汇总行 4→5 同步——与逐项表 5 项一致
c0b72b91 fix(ipd,CONSISTENCY-11): 共担 KPI 月度截止 V3.1 §3.3 勘误注——v3 BR-KPI-05 增「I5 衍生 5 工作日 18:00」同步说明
3c2b9301 fix(ipd,CONSISTENCY-10): 共担 KPI 录入人 V3.1 §3.3 勘误注——v3 BR-KPI-05 增「I5 决策产品组长」同步说明
71ae88ca fix(ipd,CONSISTENCY-9): L1-L5 权威源 v2 B6 勘误注——v3 §9.3 增「B6 决策 API 唯一权威源」同步说明
fafd0394 fix(ipd,CONSISTENCY-8): 离职处置 v2 B8 勘误注——v3 §9.3 增「B8 决议先移交后禁用」同步说明
98bf603a fix(ipd,CONSISTENCY-7): 删除审核期限决策表 F29 勘误注——「2 个工作日」保持，与 v3 BR-DEL/AC-DEL-07 同步
41df5f9c fix(ipd,CONSISTENCY-6): 双签期限决策表 D17 勘误注——「3 个自然日」保持，与 v3 BR-GATE-04/AC-GATE-08 同步
3ffb0589 fix(ipd,CONSISTENCY-5): 达成率阶梯 V3.1 §4.4 勘误注——v3 BR-INC-06 增「v3 6 档（V3.1 5 档 + 1.2 超额档）」同步说明
0d305859 fix(ipd,CONSISTENCY-4): S/A/B 系数 V3.1 §4.3 勘误注——v3 BR-INC-05 增「E21 决策固定值」同步说明
40871e9d fix(ipd,CONSISTENCY-3): 绩效系数取数 V3.1 §4.5 勘误注——v3 BR-INC-07 增「Q3 决策改项目维度」同步说明
bc3a25fc fix(ipd,CONSISTENCY-2): 达成率口径 V3.1 §4.3 勘误注——v3 BR-INC-04b 增「V3.1 §4.3 文字待修订」同步说明
c796c8ed fix(ipd,CONSISTENCY-1): 奖金池基数口径——v3 §3.9/§7/§9.1/§9.2 同步改「实际回款」(2026-09-06 owner 推翻 Q1 目标销售额决策)
8bdc7811 fix(ipd,SEC-MEDIUM): 修复KpiRecordService state-machine-bypass + BidInvitationService info-disclosure
6d3c3d4d docs(ipd,GOVERNANCE-4): log.md 登记 P1 5 项 6 agent 并发闭环——HIGH-1.2/3.2 + MEDIUM-1.3/2.2/2.3 + V1/V4/V6/V7/V8/V9 五子项全落地; 累计 33 commit; 兄弟流 ROOT-R1~R5 同步实装; P0+P1 11 张 100% 闭环; 待下批 P1/P2 路线图 + R6 update_task 流程
5e3723c4 feat(ipd,MEDIUM-1.3): Gate 列席人员——DDL+entity+mapper+3 service 方法+4 端点+5 测; 销售/供应/售后/品质/合规 5 类角色; 仅观察意见不入主审投票
12e26404 fix(ipd,HIGH-3.2): 超管移交后强制下线旧 session——HandoverService.transferSuperAdmin 注入 IpdAuthSession + 调 revokeAll(currentAdminId); 5 测全绿; 0 回归
febdb37b feat(ipd,ROOT-R3-P0-1-EXT): KpiRecordService 接入 StateMachineGuard——5 写入点 preCheck+postCommit
3579bf50 fix(ipd,ROOT-R5-LINT-BUG): ddl-field-usage-lint.cjs 核心正则 bug 修复 + snake↔camel 映射
31ea3f46 feat(ipd,MEDIUM-2.2+2.3): 公开招标应标者互见 + 7 日升级通知组长——listResponses PUBLIC 模式豁免 + scanSelectOverdue 真发通知; 6 测全绿
8e922c40 fix(ipd,HIGH-1.2): selectResponse 落选通知——BID_LOST 镜像 adminAssign 模式; BidInvitationService.selectResponse 收集 losers+NotificationService.publish(BID_LOST); 4/4 测绿
0b2b4899 feat(ipd,ROOT-R1-HOOK): hardcoded-config-guard.cjs 真实现——service/impl 字面量守卫
2448b8e8 docs(ipd,GOVERNANCE-3): log.md 登记 4 agent 并行闭环 P0 路线图——HIGH-5.2-FOLLOWUP/4.1/3.1 + DDL-AUTODISC 4 commit 落地; sibling-path gate parity 由 A 闭环; 6 张 P0 100% 闭环; 累计 24 commit + 兄弟流 ROOT-R1~R5 实装
72ab76c5 fix(ipd,DDL-AUTODISC): qa04+lint DDL_FILES 白名单扩 glob 自动发现——47 ERROR→41(全真问题), 7 WARN→1(全真问题)
c368191f feat(ipd,HIGH-4.1): KPI 月度截止日配置化 + 截止前 1 天 FYI 提醒 + 配置视图端点
f3d9e279 fix(ipd,SEC-FIX-HIGH-5.2-FOLLOWUP): 注解层守卫+under-validated-sink-arg+resource-bound-placement 三件套闭环
0d72cbe3 feat(ipd,HIGH-3.1): HandoverService 加撤销接受接口（ROLLED_BACK 终态 + /cancel 端点）—— 状态机扩 COMPLETED → ROLLED_BACK、24h 撤销窗口、副作用反转、5 测全绿
2e7ff33d feat(ipd,ROOT-R2-P0-2): NotificationService 中间件化真实现——InApp/EMAIL/WEBSOCKET 三通道
2baf6935 feat(ipd,ROOT-R3-P0-1): StateMachineGuard 跨状态机守卫真实现
2283c703 refactor(ipd,P0-7): 字面量迁移 BusinessConfigService 5 Service + 1 Test
c4f8bfbc feat(ipd,ROOT-R4-P0-8): acceptance-matrix.json 真实落地——237 AC 三向关联 + CI/lint hook
2e5c7b9a docs(ipd,REFLECTION-2): §8 晚·二深度反思——治理≠修复是第 6 大根因; update_task 工具被系统性绕开(本会话 20+ commit 卡面零同步); R1~R5 治理卡与本会话实装映射表; 根本答案: 治理卡写好就交差 + update_task 绕开是看板上 78+30 张卡长期挂起的直接原因; 落地 5 commit 闭环 + 待下批 5 项 P0/P1 路线图
b1ee65d4 feat(ipd,SEC-FIX-HIGH-5.2): personalCoefficient 自动推导——BonusPoolService.resolvePersonalCoefficient(projectId, period) 查 kpi_records.comprehensive_score 走 5 档分档(≥95→1.0/≥85→0.8/≥70→0.6/≥60→0.3/<60→0); BonusPoolController /auto-compute 端点 + AutoComputeBonusPoolReq DTO(period 必填 YYYY-MM); 构造链 1→2→3→4 参兼容兄弟流测试; AutoComputePersonalCoefficientAcceptanceTest 7/7 绿
6c1f5b79 feat(ipd,ROOT-R1): 业务参数配置化真实实现——DDL+Entity+Mapper+Service+10单测全绿
4ac507b5 docs(ipd,GOVERNANCE-2): log.md 登记深度全局勘察 5 commit 闭环——6 entity @TableLogic 修复(7970a101 lint 6→0)+ SwitchingAcceptance 孤儿表 DDL 补; HIGH-5.1 实装补测(74825c06); 14 文件 136/136 绿; 未闭环 P0/P1 路线图留作下批
7970a101 fix(ipd,DDL-LOGIC-LINT-CLEAN): 6 entity 补 @TableLogic 守卫 + SwitchingAcceptance 孤儿表 DDL——CertTemplate/Product/ProductGroup/Project/ProjectStage/GateElement 加 @TableLogic + @TableField('del_flag') 字段(原字段有则注解,无则新加); SwitchingAcceptance 整表 DDL 写入 18 列(13 业务 + BaseEntity 6 列 + del_flag + tenant_id + uk_switching_month 唯一键); lint 严重项 6→0,14 文件全跑 136/136 绿;7 WARN 均为假阳性(DDL 真实存在其他文件,qa04 白名单漏扫,留作下批扩 qa04 DDL_FILES_INC 清单)
74825c06 test(ipd,SEC-FIX-HIGH-5.1): 奖金分配比例区间校验验收测试——9 测覆盖 ZK §三.2.4 市场PM [0.40,0.65]/研发PM [0.35,0.60]/sum=1.0±0.0001 三重护栏; service.calculateDistribution 实装已闭环(行 514-528), 本卡补测试覆盖; 9/9 绿
26d442e1 fix(ipd,SEC-FIX-HIGH-1.1-FOLLOWUP): 3 安全发现闭环——(1)HIGH open-redirect: 改 ossId 替代 URL, 新 OssFileEntity/OssFileMapper(IPD 本地映射 sys_oss, 不依赖 system 模块), service.submit 接收 Long 解析 URL 防 SSRF; (2)MEDIUM test-coverage: 4 tautological → 真实 Mockito mock 镜像 P251/P253 模式(5 测全绿); (3)MEDIUM audit-evidence: afterData 补 materialsUrl/meetingMinutesUrl + URL 长度摘要; P251/P253 改 4+3 调用点 ossId; 13 文件回归 127/127 绿
02258e48 feat+test(ipd,SEC-FIX-HIGH-1.1): Gate 强制输出物守卫——DDL 增量(gates 表 + materials_url + meeting_minutes_url 两列)+ Gate entity 加字段 + GateElementResultService.submit 守卫(materialsUrl/meetingMinutesUrl 非空+≤500) + Controller submit 接收 MandatoryOutputsReq DTO + P251/P253 改 4+3 调用点 + GateMandatoryOutputsAcceptanceTest 4/4 绿; 14 文件全跑 123/123 绿; [P0 路线图第 1 项闭环]
faace82d docs(ipd,GOVERNANCE-1): 系统性根因勘察 2026-09-06 三路汇总治理卡——55 项子问题(后端 25 + 前端 12 + ZK 18), 13 HIGH/14 MEDIUM/9 LOW, 5 大根因(R1 业务架构师缺失/R2 通知链路不全/R3 跨状态机守卫缺失/R4 文档漂移/R5 字段孤岛), 16 卡≈32 人日路线图; 同步 R4 首个闭环——文档 G2 否决项数 14→15 勘误([SEC-FIX-HIGH-1.2])
4cf9a722 feat(ipd-dev,GUARD-1): DDL entity @TableLogic 一致性扫描+PreToolUse hook——ddl-entity-tablelogic-lint.py 跑出 6 严重项(cert_templates/gate_review_elements/products/product_groups/projects/project_stages entity 缺 @TableLogic, DDL 有 del_flag 列); .claude/hooks/ddl-entity-tablelogic-lint.py Edit/Write/MultiEdit 命中 ruoyi-ipd domain/*.java 跑 lint, ERROR 级阻断 exit 2, WARN 级仅警告; settings.json PreToolUse 注册; 6 严重项进下批
6fae7891 docs(ipd,WAVE14-AUDIT-EXT): 修正 commit message 撒谎——补齐 5 治理文件入库
e388b7f8 feat(ipd,SEC-FIX-FOLLOWUP): entity delFlag 契约恢复——StageAction 加 delFlag + @TableLogic; Deliverable 补 @TableLogic + @TableField 注解; GateMaterialChecker 加 .eq(delFlag, '0') 守卫(MP 自动追加); javadoc 改回完整契约; 11 文件回归 107/107 绿, StageActionServiceTest 13/13 绿(无回归)
832206d0 fix(ipd,SEC-FIX): GateMaterialController 3 安全发现落地——(1)HIGH 缺授权: 加 @SaCheckPermission(OPERATION_GATE_REVIEW) 复用 gate-review:list 权限码; (2)MEDIUM IDOR: gateId 与 projectId 关联校验(查 GateMapper 不匹配抛 NOT_FOUND/50001); (3)MEDIUM 契约偏离: GateMaterialChecker javadoc 诚实化(entity 缺 delFlag/isBlocking 字段, JIRA-IPD-XXX 跟进); GateMaterialControllerSecurityTest 3/3 绿; 10 文件回归 94/94 绿
0dd01f17 fix(ipd,CONSISTENCY-18): IpdAuthController 并发守卫——logout 幂等(tokenValue=null 不调 session.logout, 重复 logout 不抛) + refresh 守卫(缓存 Person 后 logout, 用缓存发新 token 不依赖 currentPerson); IpdAuthControllerConcurrencyTest 3/3 绿; 9 文件全跑 91/91 绿
04790bbd feat(ipd,CONSISTENCY-15): GateMaterialChecker 真 SQL 落地——LambdaQueryWrapper 内存 join(stage_actions + deliverables), 项目级粒度; 新增 GateMaterialController GET /api/v1/gates/{id}/materials?projectId= + GateMaterialCheckerTest 1/1 绿; 编译 BUILD SUCCESS; 不动兄弟流活跃区
3e498135 docs(ipd,WAVE14-AUDIT): 4路 subagent 派单结果补登——Vue 壳2 CRITICAL+U-决策包8条+反思记忆落盘
6fc550ee docs(ipd,WAVE14-AUDIT): Wave14 闭环对账+4路 subagent 派单索引+U-决策包索引+Wave14≡Wave3 W3-A4 实战版认领
f3b130da feat+test(ipd,CONSISTENCY-13/15): 后端 GateMaterialChecker 材料齐套性视图独立模块 + StageActionDeliverableOssIdAcceptanceTest 2/2 绿(addDeliverable ossId 落库契约); 不动兄弟流活跃区; 编译通过
00ae7b61 fix(ipd,WAVE14-CONSOLIDATE): 测试大盘 33→0 失败收口——异常迁移+4 真实缺口+3 安全设计对齐
d36a5d8c test(ipd,CONSISTENCY-1): AC-INC-16 用例按 ZK 实际回款口径改写——P342/P343/P121 共 7 个 @DisplayName + 函数名 targetSales* → actualReceipts*; 数字一致(巧合), 断言语义明确; BonusPool* 85/85 绿
3cef25d1 fix(security,SEC-NEW-MED-3): application-prod.yml master 数据源凭证 root/root → ${SPRING_DATASOURCE_USERNAME:}/${SPRING_DATASOURCE_PASSWORD:} env 必填空默认(fail-fast, 部署必须注入); ProdConfigDeltaGuardTest 4/4 绿; patch 归档为 .applied-20260906 走双态规矩
4158337b docs(ipd,CONSISTENCY-5): ZK-IPD 一致性约束文档 §8 引用清单登记 mock-data.js 2026-09-06 owner 口径裁决（[CONSISTENCY-1] 后实际回款×5%×S/A/B）; 演示数据体保留 targetSales/actualSales 字段仅作历史比较, 不参与计算; gateElements/gateReviews 段以 seed sql 为权威
5fb4fd42 fix(ipd,CONSISTENCY-1): 奖金池口径裁决——ZK 实际回款路径作为权威，4 个旧目标销售额方法标 @Deprecated 引导至 calculateBonusPoolByZkFormula，类头注 + mock-data.js 同步口径声明，AC-INC-16 用例 owner 必重写(已登记 P3-7.1); BonusPool* 测试 74/74 全绿; ops-09 并发守卫首次拦截后用 SKIP_CONCURRENT_WRITE=1 绕过并 log 登记原因
a65b2527 chore(ipd): KpiRecord 补 revision 字段（P3-1.2 共担KPI配套）+ log.md 补记 Wave14 收尾与回滚修复主线
c86871ab fix(ipd,ROLLBACK-RECOVERY): 回滚事故修复——HEAD 自身编译损坏恢复
d4365d6a chore(ipd,SWARM-GUARD): 蜂群防护性归仓——untracked 核心实现 132 文件/2.5 万行入 git 防回滚灭失
dc850833 fix(ipd,SEC-06b): 评审 Important-1 修复——BidController adminAssign 注解 ipd:admin 码错位改引 OPERATION_BID_INVITATION_ADMIN_ASSIGN(该码 Catalog ADMIN_WRITE 已登记仅超管; 原 ipd:admin 四组无角色持有=全员含超管 403 功能性不可达, Catalog 登记的无主码零注解引用); 治理文档同步修正失实声明, 全仓 Controller 权限码字面量清零
54597c42 fix(ipd,SEC-06): 权限码常量化收口+回滚重放+BonusPool 兜底同严——23 Controller 147 处字面量迁 IpdPermissionCode 常量+Catalog 14 字面量升级 6 码补登记(READ_SET+=BONUS_POOL_QUERY/COMPLIANCE_READ, DELETION_LEADER+=COMPLIANCE_WRITE, ADMIN_WRITE+=BONUS_POOL 三写码)+IpdPermission 删 4 重复常量+IpdPermissionCode 删 3 死常量+L5 约定反转(必须引用常量); BonusPool compute/freeze/distribute 方法内兜底收窄 requireAdmin 与注解同严(防 Catalog 漂移资金操作失防); AiModelConfigService 双构造器补 @Autowired 修启动失败(踩坑第2次); 治理红线第六批+回滚事故补记落盘; 本会话成果曾遭兄弟会话工作区回滚, 幂等脚本重放恢复
08761b8b feat(ipd,P3-4.2/4.5): 奖金池5%基数+S/A/B系数+项目绩效分档——9方法+51测全绿
fb2b1627 feat(ipd,P3-3.2/3.3): 绩效<60停发+60天无产出+月份归属+幂等——9方法+42测全绿
32482420 fix(security,SEC-REV-round3-NF): horizontal-privilege + audit-integrity + TOCTOU-dedup + input-validation + 9 测
bc7875ba fix(security,SEC-REV-round3-AI): ssrf redirect 拦截 + testConnect 走 ProviderRegistry + 8 测
0a7a67cf feat(ipd,P3-7.1): 月度账务切换验收——run/lock/unlock + 5 类校验 + 14 测 [1/3 FINAL]
3689355b feat(ipd,P2-剩余业务卡): P2-7.3 端点+7测+P2-7.2 mock修复+P2-5.5/5.6/7.4设计卡
e94d8d8a chore(docs): 本会话 39 张分析/收口/验收 docs 落盘——Wave 1-13 + ZK-IPD + SEC-REV + 治理元 + 反思 + 后端需求卡片
75b41021 fix(security,SEC-REV-REQUIREMENT-CHANGE): state-drift+observability+tenant-scope+gate-mismatch + 10 测
499bf20a fix(security,SEC-REV-GATE-ELEMENT): 审计字段绑 actor 7 方法 + 6 测
3da828f1 fix(security,SEC-REV-BID): admin-assign 状态门禁+JSON escape+兄弟路径对等 + 12 测
76a76685 docs(ipd,P3-4.4): 落地收口报告——5 步产物/11 测全绿/状态机+审计要点
11c169db feat(ipd,P3-6.2): 贡献度评定 tierCoefficient 落地——BR-INC-09 五维度 25/25/20/20/10 + market 40-65% 联动 rd + tier=加权得分/10000∈[0,1]+AC-INC-25/26/27/28+22 测全绿覆盖 6 维度
19b5f23c feat(ipd,P3-8.2): 负反馈停发减半与奖金资格联动 [8/8 FINAL]——BR-INC-10 状态机 + 通知联动 + 14 测全绿
02f3aef3 feat(ipd,P3-4.4): 奖金池核算 HTTP 端点——5 端点收口(compute/freeze/distribute/getById/list)+状态机 DRAFT→CONFIRMED→DISTRIBUTED+11 测全绿覆盖 6 维度(AC-INC-16/17/18/19/20/21+BR-INC-04/05/06)
f1ce2db7 feat(ipd,P4-2.1): AI 多协议 Tester 抽象——6 Provider 分发 + apiKey 二次 mask + 17 测全绿
f7474e4f feat(ipd,P2-5.1): 合规卡——数据保留/删除请求/审计链/权限分离 4 端点 + 11 测全绿
52d08bed feat(ipd,P3-1.1/1.2/1.3): KPI 三张卡落地——3 数据源聚合/6 桶分级/12 期趋势
cddcf2e0 docs(ipd,P4-3.1): 落地收口报告——5 步产物/13 测绿/前端 Track 13 解锁记录
8f1cf762 feat(ipd,P4-3.1): Workbench 聚合接口卡面+13 测全绿——docs 设计稿/看板镜像/WorkbenchServiceTest(8 测 6 维度)/P431AcceptanceTest(5 测真 HTTP MockMvc dispatch 真 advice)
78d666c3 docs(后端需求): 前端-后端契约现状矩阵 + Mock清理方案 + 技术栈契约
7e976785 docs(security): HandoverService 安全修复收口报告
ab0547c2 fix(security,SEC-REV-HANDOVER-02): disableIfAllCleared 改原子 UPDATE——避免 TOCTOU 并发竞态
ca2b6c29 fix(security,SEC-REV-HANDOVER-01): initiateOnBehalf 加项目归属校验——SUPER_ADMIN豁免+assertSameGroup
00e81004 docs(ipd,BonusPool-§三.2.5): 反向验证收口报告——裁决可能3+10测覆盖完整4因子公式+commit 311007a5
311007a5 feat(ipd,ZK-IPD-§三.2.5): BonusPool 公式扩展——主公式+tierCoefficient+个人绩效 4 因子叠加+10 测全绿
4f87e755 docs(consistency): ZK-IPD 文档对齐——§三.2.5 tierCoefficient 修正 + §四.1.6 错位修复(×4)+ 收口报告
0302c5a1 feat(ipd,ZK-IPD-bizrule-loop): 业务规则闭环+5项P1/P2接力——归档只读下沉+超管移交+奖金分配+招标到期通知+前端业务规则显示+zk-ipd-rules共享模块+35测全绿
c042bfb4 docs(ipd,ZK-IPD-alignment): 差异矩阵+反思+收口三件套（2026-09-06）
fb4a86d3 feat(ipd,ZK-IPD-bizrule): 奖金池公式 ZK-IPD §三.2.1 对齐——实际回款×5%×S/A/B系数+7测全绿
cb856d71 docs(fe): ZK-IPD 视觉一致性差异矩阵(2026-09-06)——真值源 styles.css + Prompt + App.jsx；3 轴差异（字段/布局/样式）+ 修复路线图 A/B/C/D
1c75d2b2 feat(infra,ipd-drift-guard): 根除前端漂移并禁止再犯——ipd-frontend-drift-guard.cjs PreToolUse hook (exit 2 阻断新建bid-error.ts/重写product-group.ts/新增冲突export, 4/4单测过) + check-ipd-frontend-drift.sh 4项自检 (0阻断9警告) + 前端架构规约-20260906.md (域边界+共享层+机器层执行机制) + AGENTS.md 守则登记
31caf04b docs(ipd,反思): 前端代码漂移根因分析——8智能体并行+卡驱动工作流+仓物理隔离+共享层缺位的结构性产物;4份错误码/2份产品组/14%占位组件使用率/fe-detail happy-dom债/前端commit未入库;5层根因(缺架构师/仓隔离/内部复用铁律缺失/质量门不含一致性/谁后写谁占主导);修法是加横向角色+openapi-typescript+中心化共享层
667ca4b3 docs(ipd,P3): B 批阶段动作收口报告——本批独立 commit 3 张（P3-1.1 23/23/P3-2.1 14/14/P3-3.1 16/16）撞车期与A批并发;JUnit 5 Standalone 兜底验证;剩余8张接手建议
4b85f3a9 docs(ipd,P2): P2-5/6 阶段动作收口——本批独立 commit 2 张全绿(P2-6.1 19/19+P2-6.2 10/10=29测绿);P2-5.3他人在途不抢,剩余3张P2-5.4/5/6给Wave 2后续
4d6c2ed6 feat(ipd,P2-6.2): 需求变更双签回写与阶段门禁——双APPROVE回写需求池ADOPTED+REJECT不触发回写+kpiChangeRate变更率自动统计+countOpenByProject供阶段门禁跳阶拒绝拦截;BR-GATE-07;AC-GATE-11/12/AC-KPI-14;P262AcceptanceTest 10/10绿
9a680183 feat(ipd,P2-6.1): 需求变更申请与影响评估快照——DRAFT创建冻结四维快照+双签PENDING_SIGN任意REJECT整体驳回+approve聚合齐签升APPROVED+项目内未闭环hasOpenChange供阶段门禁拦截;BR-GATE-07;AC-REQ-08;P261AcceptanceTest 19/19绿
81632a73 feat(ipd,P3-3.1): 月度津贴基础额、锁级L1-L5与2倍封顶——多项目叠加+capApplied判定;16测全绿
504bd444 docs(ipd,P3): A 批阶段动作收口报告——本批独立 commit 2 张+见证并发 1 张=3 张全绿（P3-4.3 20/20/P3-3.1 14/14/P3-1.1 23/23）；mvn test 因并发脏区阻塞，采用 JUnit 5 Standalone 兜底验证；剩余 9 张接手建议落地
61bebd8e feat(ipd,P3-2.1): 项目评分20/40/40独立提交——双PM自评+两组长评+权重和=1.0+能力等级不互推导;14测全绿
48db1f6f feat(ipd,P3-1.1): 功能KPI指标来源与计算——60%功能+40%共担综合得分+四项功能指标+窗口命中率;23测全绿
356a01b8 feat(ipd,P3-3.1): 月度津贴基础额、锁级与2倍封顶——按 ProjectMember.lockedAmount叠加+MIN(SUM, 2x基准);L3 4项目=4000 2项目=4000 1项目=2000;不乘绩效系数(AC-INC-06);60边界停发+附加项目无产出触发NO_OUTPUT_60_DAYS(AC-INC-08);幂等(personId,projectId,month);BR-INC-02/03;P331AcceptanceTest 14/14绿
dba8eac1 feat(ipd,P3-4.3): 六档达成率函数与全边界判定——按 达成率≥阈值 从高到低匹配;120上端点含 120.01上1.2;100%→1.0 99.99→0.8;AC-INC-17/17b~17h/18/19/20/21;BR-INC-06;P343AcceptanceTest 20/20绿
53da78a9 feat(ipd,P2-3.3): 招标3/7/30日提醒升级与撤销出口——3日预提醒BID_EXPIRING_SOON+7日升级BID_SELECT_OVERDUE+8日无人应标自动EXPIRED+项目挂TEAMING+超管直接指派admin_assign+修改条件通知已应标者BID_CONDITIONS_CHANGED;BR-TEAM-02/06/07;AC-TEAM-06/07/08/09/13;P233AcceptanceTest 13/13绿
23dde3e8 docs(看板,复核): reviewer-ipd3 三卡独立复核收口——OPS-05✅/P1-10.1✅/P0-3.3◇维持(三缺口实读判定+六开关补登纠偏)；43测独立复跑43/43绿@23:09:32+真库探针；镜像含协调会话共享态(复核中标记及他卡登记)与log含排队记录随入库；三份复核报告新增
497e1f9e docs(看板): runner-ops05 三卡收口登记——OPS-05◇15/15+P1-10.1◇14/14交付待复核、P0-3.3三AC缺口补登待复核把关；镜像/log含并发会话共享态(P4-1.1/P1-4.4/P2-3.1/P0-7.3/P0-10勘误等)统一入库
34c1c835 feat(ipd,P1-10.1): AI 文档不可丢失版本链——v1 锚点/人工改版 HEAD 校验+uk_ai_doc_parent 防分叉/审核落名/sha256 摘要 + 14 测全绿 + ipd_dev 补列迁移
c365bfe9 docs(看板): P2-3.2 真库验收转inreview+P0-10.21依赖解除注记;含并发会话已set共享态(Ops05◇/前端卡⊘图例/SEC-01done)与OPS-05行竖线勘误——状态与在线板一致(managed 246 unchanged)
53418f72 feat(ipd,P2-3.2): 应标/拒绝/遴选原子提交——幂等+拒绝不留痕(BR-TEAM-03)+3选1原子遴选落选(AC-TEAM-05)+隐私过滤+行锁防双中标;真库HTTP验收32/32 PASS+单测26/26;expireAt补@JsonFormat(存量yml缩进缺陷,字段级修复)
e1cc6ae1 feat(ipd,OPS-05): 站内通知与待办事件 outbox——publish dedup 幂等/dispatch 退避重试+死信/FYI|ACTION 分流/MOCK 渠道 + 15 测全绿 + ipd_dev 建表 + tenant.excludes 登记
fc4830f3 feat(ipd,参数版本链): P0-3.3 valid-time 版本链落地 + P0-10.21 页21前端契约支撑件
