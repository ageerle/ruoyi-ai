# IPD 改造工作日志（docs/ipd-系统说明/）

> 本目录是 IPD 二开工作手册（drift audit + 改造指南 + 类型映射 + 命名约定 + 外部资源骨架）。
> 变更追踪在 `docs/wiki/wiki/log.md`（karpathy-llm-wiki 工作流）。
> 本文件专注记录 IPD 改造相关变更。

---

## 2026-09-05 — 第六轮：三 Agent 审查 P0 清零（5 卡 TDD 闭环 / 6 commits）

- 3 专业 agent 并行审查（security/performance/code review）产出 9 P0 → 拆 5 张修复卡（PERF-01/02/03 + SEC-AUD-01 + CODE-01），本轮全部 TDD 闭环清零：
  - **PERF-01**（c515b142）：ProjectService.nextCode 加 synchronized + DDL uk_projects_code/uk_projects_product（2026-09-05-ipd-perf01-nextcode-unique.sql）；50 并发取号唯一性测试 GREEN。
  - **SEC-AUD-01 S2**（3f759dc9）：login 4 类失败分支（BAD_CREDENTIALS/RESIGNED/DISABLED/STATUS_ABNORMAL）补 auditFail 写审计，走 AuditLogService REQUIRES_NEW 独立提交；P0-1 changePassword 审计被回滚经核实为伪问题（append 已 REQUIRES_NEW）。
  - **PERF-03**（537c71dc）：StageActionService.instantiate 由循环 selectCount×N+insert×N 改 1 次 selectList（in-memory Set 查重）+ 1 次 insertBatch(200)；138 IO→2 IO。
  - **CODE-01**（4756a9fc）：StageActionService 3 处裸 @Transactional 补 rollbackFor（反射测试防回归）；新增 dto 包 4 record Req（@JsonIgnoreProperties 白名单），3 Controller 4 处 Entity 入参替换，服务端权威字段（code/status/source/gateCode 等）注入面归零。
  - **PERF-02**（d168c04f）：**用户裁决配置变更立即生效（不允许 TTL 窗口）**——SystemConfigService 加 ConcurrentHashMap 只读缓存 + invalidate/invalidateAll 写穿透失效；computeIfAbsent 防击穿 + Optional.empty() 防穿透；单 JVM 定位，多实例需 Redis 广播。
- 附带战场清理：恢复被外部进程反复清理的 auth 链 8 个支撑类（backup/.codex 冻结源，不同血统不可混用）；IpdAuthService 增补 scopeOf+PASSWORD_CHANGE_REQUIRED；P064 Sa-Token 1.44 API 名修复（d064dc05）。
- 本轮共 22 个新测试全绿；未推送；任务状态同步本机看板（5 卡 done）。
- 残留入口：SEC-AUD-01 refresh 审计+freeze 授权（地基已就位）/ SEC-01 operatorId 会话接管 / P0-3.2 参数后台端 / check_ipd.py 从 git 历史恢复 / PERF-01 DuplicateKeyException 异常语义。

## 2026-09-04 — 初始建立

### 创建文件

**主文档（6 个）**：
- `README.md` — 改造工作手册总览
- `drift-audit-report.md` — RuoYi-AI 基线 vs 开发说明书完整漂移审计
- `改造检查清单.md` — 静态检查脚本（路径修正）+ CI 集成 + 阶段验收清单
- `type-mapping.md` — PostgreSQL → MySQL 字段类型映射
- `naming-convention.md` — 20 张 IPD 业务表命名 + 字段命名 + ApiV1Response 规范
- `fork-原与外部资源清单.md` — fork 链 + 14 个注解资源清单 + 外部资源状态

**外部资源骨架（10 个）**：
- `IPD系统_AI开发主Prompt_v3.md` ✅ **已填充（从 ZK-IPD 复制 181 行原文）**
- `IPD系统_六阶段标准动作清单_v3.md` ⚠️ 骨架（69 动作待填充）
- `IPD系统_五大Gate评审要素_v1.md` ⚠️ 骨架（33 项要素 + 14 否决项待填充）
- `IPD系统_验收清单.md` ⚠️ 骨架（237 条 AC 待填充）
- `IPD系统_开发执行规则_AI必读.md` ⚠️ 骨架（11 条硬约束已嵌入 v3 Prompt）
- `IPD系统_冲突裁决与最终待确认清单.md` ⚠️ 骨架
- `IPD系统_待确认决策表_v2.md` ⚠️ 骨架
- `assets_公共规范-通用.md` ⚠️ 骨架
- `design-specs_后台-RuoYi-AI.md` ⚠️ 骨架
- `mock-data.js` ⚠️ 骨架

### 关键发现

**v3 Prompt 原文已找到**（2026-09-04）：
- 来源：`/Users/mac/Documents/ZK-IPD/产品流程细化管理工具 2/`
- 文件名：`IPD产品经理管理系统·最终完整版AI开发Prompt（全规则闭环无遗留疑问）.md`
- 大小：11514 bytes / 181 行
- 重要性：是开发说明书 §3 G-01~G-11 的事实源

**v3 Prompt 与开发说明书.md的对应关系**：
- 一、全局系统定义 + 七大硬规则 → §3 G-01~G-11
- 二、IPD 六阶段 + Gate → §5.4 BR-IPD + §5.6 BR-GATE
- 三、双 PM 考核 + 奖金池 → §5.11 BR-INC + §5.12 BR-KPI + §8 涉钱参数
- 四、招投标组队 → §5.5 BR-TEAM
- 五、免登录游客需求 → §5.7 BR-REQ
- 六、项目一键移交 → §5.13 BR-HAND
- 七、AI 辅助生成 → §5.8 BR-AI
- 八、角色权限 + 删除审核 → §5.1 BR-ORG + §5.9 BR-DEL
- 九、超管移交 → §5.14 BR-ADM
- 十、审计日志 → §5.10 BR-AUD
- 十一、全局闭环要求 → §12 审计与合规要求

### 仍缺失的 V3 资源（ZK-IPD 未发现）

6 个 V3 资源骨架等待填充：
1. 六阶段标准动作清单（69 动作）
2. 五大 Gate 评审要素（33 项 + 14 否决项）
3. 验收清单（237 条 AC）
4. 冲突裁决与最终待确认清单
5. 待确认决策表 v2（33 项决策）
6. mock-data.js（演示数据）

ZK-IPD 下只有「IPD业务闭环核查清单-V1.0.md」（业务核查，非 AC 验收清单）和 .docx 系统设计说明书（Word 格式不可直接读）。需要继续找其他来源或 Gavin 提供。


### 全部 7 个 V3 资源已找到 + 填充（之前漏了 `产品流程细化管理工具/` 目录）

2026-09-04 第二次探索 ZK-IPD，发现真正包含所有 V3 资源的目录是：
- **`/Users/mac/Documents/ZK-IPD/产品流程细化管理工具/`**（**没有 2**）

之前第一次搜索只看了 `产品流程细化管理工具 2/`，只找到 1 个 v3 Prompt（简化版，11.5 KB / 181 行）。

这次复制了 7 个核心 V3 资源（替换之前简版）+ 5 个额外资源（v2 历史 / P0 任务清单 / 熵基特有 / 最终版 txt）：

**核心 7 个（替换骨架）**：
1. IPD系统_AI开发主Prompt_v3.md（99 KB / 1368 行）✅ 完整版
2. IPD系统_六阶段标准动作清单_v3.md（24 KB / 298 行）✅ 69 动作
3. IPD系统_五大Gate评审要素_v1.md（17 KB / 229 行）✅ 33 项 + 14 否决
4. IPD系统_验收清单.md（36 KB / 411 行）✅ 235-237 条 AC
5. IPD系统_开发执行规则_AI必读.md（11 KB / 191 行）✅ 11 条硬约束 + 派发模板
6. IPD系统_冲突裁决与最终待确认清单.md（12 KB / 209 行）✅ 4 原则 + 6 裁定
7. IPD系统_待确认决策表_v2.md（30 KB / 172 行）✅ 33 项决策

**额外 5 个（放外部资源/额外资源/）**：
1. IPD系统_P0任务清单.md（15 KB / 360 行）—— P0 阶段具体任务
2. IPD系统_动作清单_熵基特有环节补漏.md（12 KB / 116 行）—— 熵基科技特有
3. IPD系统_AI开发主Prompt_v2.md（62 KB / 1019 行）—— v2 历史
4. IPD系统_六阶段标准动作清单_v2.md（23 KB / 232 行）—— v2 历史
5. IPD产品经理管理系统_最终版.txt（19 KB / 159 行）—— 早期 v1 之前版本

**重要发现**：
- ZK-IPD 下有「产品流程细化管理工具 2/」是 monorepo 演示工程（pnpm workspace / 38210 文件）
- 「产品流程细化管理工具/」才是产品规格文档库（20 个 .md / .txt 文件）
- v3 Prompt 真实版本 99 KB / 1368 行（之前简版 11.5 KB / 181 行是「产品流程细化管理工具 2/」里的 IPD v3.0 完整闭环版）

---

## 2026-09-04（第二轮全局一致性审计 + 历史件清理）

- **反转修正**：Q2 销售额口径 = **回款 `RECEIPT`**（以 v3:1135 参数表为准；mock-data.js:281 的 `SHIPMENT` + 「出库」注释为旧裁定残留，已改）。一致性报告 §B.6/§C 同步改写。
- **数字对齐**：CLAUDE.md 与 README-IPD-OVERRIDE「12 条硬约束」→ **11 条**（G-01~G-11），删 OVERRIDE 伪条目 G-12；spec/_导航地图 P4「11 页」→ **12 页**；AC 总数实测 237（第一轮「235」为误计；mock-data:2/:6、CLAUDE.md、fork:62 统一回滚 237）；改造检查清单 P3 算例缩进 + 档位标注对齐开发说明书 §8.4。
- **噪音清理**：删除 `外部资源/额外资源/` 5 个已吸收历史件（AI开发主Prompt_v2 / 六阶段清单_v2 / 最终版.txt / P0任务清单 / 熵基特有补漏）。吸收证据：v3 清单含熵基环节 5 处；决策表:13 全部回填 v3；P0 执行以开发说明书 §11 + 改造检查清单为准。git 历史可恢复。
- **表述同步**：README 目录树「10 个骨架」→「已填充」；fork清单 §三/§四改终局状态；drift-audit 顶部加状态注记；AGENTS.md G-04 更新为 owner 授权勘误通道（勘误须登记本 log）。
- **登记**：本轮属于 owner 授权的 docs/开发说明 勘误级更新（导航地图 P4 页数 1 处）。验证门禁：/tmp/doc_consistency_gate.mjs 全断言 PASS。

---

## 2026-09-04（第三轮：废弃件与过时段落清理）

- **删除** `drift-audit-report.md`（第一轮审计报告：Q2/AC 两结论被 §六 反转、功能被开发文档一致性报告取代），git 历史可查；同步清理引用 8 处（ipd-README 树/表/阅读路径/流程表、README-IPD-OVERRIDE 目录树/第5步、改造检查清单错误码项改指 §B.3、agents/domain.md——含「12 条硬约束」→11 条与「骨架待填充」节终局化）。
- **一致性报告**：B.1/B.5（AC 数量）改写为终局 237；§D/E/F 第一轮过程叙事折叠为历史短节（移除已过时的「不动 docs/开发说明」原则表述）；§6.2 行 8 更新为终态；尾部改为「二轮审计 + 三轮清理完成」。
- **门禁**：扩展 A19-A22 后重跑全绿（drift 文件不存在 / 无悬挂引用 / 过时原则已删 / B.1 终局化）。

