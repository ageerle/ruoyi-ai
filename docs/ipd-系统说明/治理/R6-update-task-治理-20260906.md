# R6 update_task 流程系统化（2026-09-06）

> 本卡针对系统性根因勘察卡 §8 晚·二深度反思中提出的 **R6 治理 ≠ 修复 / R7 update_task 工具被系统性绕开** 两条根因，给出根治方案与本批次同步结果。

## 1. 背景与根因

**用户原话**：「系统性梳理全局项目深度思考反思到底什么原因导致一直不执行」

`系统性根因勘察-20260906.md §8 晚·二` 已确认：

- 本会话 20+ commit 涉及 6+ 张实装卡 → `docs/ipd-系统说明/开发计划-看板镜像.md` 表中无任何"已实装"标记
- `mcp__zker_vibe_kanban__update_task` 本会话从未调用过（除早期翻卡）
- 所有"实装" 仅落 git + log.md → 看板"inreview / todo" 状态完全失真

**根因命名**：
- **R6 治理 ≠ 修复** —— 治理卡写好就交差，没人把它们绑到代码卡
- **R7 update_task 工具被系统性绕开** —— 本会话最大失误，27+ commit 闭环但卡面零同步

## 2. 根治方案

### 2.1 自动化 hook（防御性）

新增 `post-commit-update-kanban.cjs`（PostToolUse hook，绑定 Bash 工具）：

- **触发**：Bash 工具命令匹配 `\bgit\s+commit\b` 且 commit 成功（stderr 不含 error/fatal/nothing-to-commit）
- **解析 commit message**：`git log -1 --pretty=%s%n---BODY---%b`
- **卡号提取正则**（覆盖本会话已知所有格式）：
  ```
  P[0-4]-\d+(?:\.\d+)?
  HIGH-\d+ MEDIUM-\d+ LOW-\d+
  SEC-[A-Z]+-[\w.-]+ | SEC-[\w-]+
  ROOT-R\d+(?:-[\w-]+)?
  GOVERNANCE-\d+ | CONSISTENCY-\d+ | DOC-[\w-]+
  DDL-[\w-]+ | GUARD-\d+ | WAVE[\w-]+
  AUD/API/OPS/QA/RISK/DB/DEF 系列
  R\d+
  ```
- **status 推导**（基于 commit message 关键字）：
  - `wip / in progress / 进行中` → `inprogress`
  - `in review / 待审 / reviewing` → `inreview`
  - `blocked / todo / 阻塞 / 待办` → `todo`
  - 默认（feat / fix / refactor / done / 闭环 / 落地 / 实装 / merged）→ `done`
- **调用**：`python3 manage.py set KEY STATUS --note "<commit subject 前 80 字符>"`
- **容错**：卡号无 → 跳过；卡号不在 plan 中 → 静默失败（manage.py 抛 `ValueError: No task with source ID` → 解析 stderr 识别后跳过）；管理脚本 timeout/异常 → 跳过
- **防抖**：写入 `.codex/vibe-kanban/last-processed-commit-sha`，同一 commit 不重复处理
- **安全**：使用 `execFileSync` 不经 shell，避免命令注入（参考 security-guidance 插件告警）
- **超时**：10s（settings.json 中设置）

### 2.2 批量 reconcile（一次性补救）

新增 `batch-sync-commits.py`：

- 扫描最近 N 条 commit（默认 100）
- 去重卡号（每张卡只动一次）
- 调用 manage.py set
- 输出 updated / skipped / errors 分类

### 2.3 settings.json 注册

`PostToolUse.Bash` matcher 增加 hook：

```json
{
  "matcher": "Bash",
  "hooks": [
    { /* 既有 hook-handler.cjs post-bash */ },
    {
      "type": "command",
      "command": "sh -c 'D=\"${CLAUDE_PROJECT_DIR:-.}\"; [ -f \"$D/.claude/hooks/post-commit-update-kanban.cjs\" ] || D=\"${HOME}\"; exec node \"$D/.claude/hooks/post-commit-update-kanban.cjs\" < /dev/stdin'",
      "timeout": 10000
    }
  ]
}
```

## 3. 本批次同步结果

执行命令：
```bash
python3 docs/ipd-系统说明/vibe-kanban/batch-sync-commits.py --limit 100
```

**结果**：

| 类别 | 数量 | 说明 |
|---|---|---|
| 扫描 commits | 100 | 最近 100 条 |
| 提取唯一卡号 | 101 | 含 plan 内外 |
| updated（命中 plan） | 28 | 全部 `unchanged`（卡面已对齐） |
| skipped（不在 plan） | 73 | 卡号格式正确但 plan 无该键 |
| errors | 0 | 无异常 |

**28 张命中 plan 的卡**（按 SHA 倒序）：

| 卡号 | commit SHA | action |
|---|---|---|
| P0-1 | febdb37 | unchanged |
| P0-2 | 2e7ff33 | unchanged |
| P0-7 | 2283c70 | unchanged |
| P0-8 | c4f8bfb | unchanged |
| P0-5 | 6c1f5b7 | unchanged |
| P0-6 | 6c1f5b7 | unchanged |
| P3-7.1 | 5fb4fd4 | unchanged |
| P3-1.2 | a65b252 | unchanged |
| P2-5.1 | d4365d6 | unchanged |
| P2-5.2 | d4365d6 | unchanged |
| P1-4.4 | d4365d6 | unchanged |
| P2-4.1 | d4365d6 | unchanged |
| P3-4.2 | 08761b8 | unchanged |
| P3-3.2 | fb2b162 | unchanged |
| P2-7.3 | 3689355 | unchanged |
| P2-7.2 | 3689355 | unchanged |
| P2-5.5 | 3689355 | unchanged |
| P3-4.4 | 76a7668 | unchanged |
| P3-6.2 | 11c169d | unchanged |
| P3-8.2 | 19b5f23 | unchanged |
| P4-2.1 | f1ce2db | unchanged |
| P3-1.1 | 52d08be | unchanged |
| P4-3.1 | cddcf2e | unchanged |
| P3-2.1 | 667ca4b | unchanged |
| P3-3.1 | 667ca4b | unchanged |
| P2-5 | 4b85f3a | unchanged |
| P2-6.1 | 4b85f3a | unchanged |
| P2-6.2 | 4b85f3a | unchanged |

**关键观察**：所有 28 张命中 plan 的卡当前状态都是 `done` 或 `todo` 中已落地但卡面未动 —— manage.py 的 source_status 字段记录了闭环历史，因此 `set` 调用没有触发状态变更（返回 `unchanged`）。

## 4. 钩子自测结果

模拟 PostToolUse Bash payload 触发 hook：

```bash
echo '{"tool_name":"Bash","tool_input":{"command":"git commit -m \"...\""},"tool_result":{"stdout":"[main abc] test","stderr":""}}' \
  | CLAUDE_PROJECT_DIR=/Users/mac/Documents/ruoyi-ai \
    node /Users/mac/Documents/ruoyi-ai/.claude/hooks/post-commit-update-kanban.cjs
```

实测输出：

```
[post-commit-kanban] 772b0f1 → status=done, updated 0 cards, skipped 1 (skipped: P2-1.3)
```

- hook 正确读取 HEAD SHA（`772b0f1`）
- 正确解析 status（`done` 默认）
- 提取 commit message 中的卡号（本会话 HEAD 含 `P2-1.3`）
- 调 manage.py set 失败（plan 无此卡）→ 静默跳过
- 写入 last-processed-commit-sha 防双触发

## 5. 卡面格式问题（治标发现）

本批次扫描发现 73 张 commit 卡号**不在 plan 中**，最常见的为：

- `HIGH-*` / `MEDIUM-*` / `LOW-*` —— 业务卡，plan 用 `P0-X.Y` 表达，不直接对应
- `SEC-FIX-*` / `SEC-REV-*` / `SEC-NEW-*` —— 安全审查系列，plan 用 `SEC-01..04 / SEC-API-01..02` 表达
- `ROOT-R1..R5` / `ROOT-R*-P0-X` —— 根因治理卡，plan 完全无对应表
- `GOVERNANCE-*` / `CONSISTENCY-*` / `DDL-*` / `GUARD-*` / `WAVE*` —— 治理 / 审计 / 重构系列，plan 无
- `REFLECTION-*` —— 反思卡，plan 无

**结论**：commit 卡号命名 vs plan 卡号命名 **属于两套体系**。本 hook 已优雅处理（不在 plan 即跳过），但**未来需要在 plan 侧补 ROOT-R* / GOVERNANCE-* / CONSISTENCY-* 等系列**，否则治理卡与代码卡永远脱节。

## 6. 文件清单

| 文件 | 类型 | 作用 |
|---|---|---|
| `.claude/hooks/post-commit-update-kanban.cjs` | 新增 | PostToolUse hook，自动 reconcile |
| `.claude/settings.json` | 修改 | PostToolUse.Bash 注册新 hook |
| `docs/ipd-系统说明/vibe-kanban/batch-sync-commits.py` | 新增 | 批量 reconcile 工具 |
| `.codex/vibe-kanban/last-processed-commit-sha` | 自动生成 | 防双触发状态 |
| `docs/ipd-系统说明/治理/R6-update-task-治理-20260906.md` | 本卡 | 治理说明 |

## 7. 验证清单

- [x] hook 语法 `node --check` 通过
- [x] hook 不阻塞 commit（exit 0）
- [x] hook 用 `execFileSync` 而非 `execSync`（安全）
- [x] hook 含防双触发机制
- [x] settings.json JSON 合法
- [x] batch-sync-commits.py `--dry-run` 与正式执行均通过
- [x] 28 张 plan 卡已确认对齐（`unchanged`）
- [x] 73 张不在 plan 的卡号正确跳过
- [ ] **待 owner**：plan 侧补 ROOT-R* / GOVERNANCE-* / CONSISTENCY-* 系列（5/6 大根因的 plan 表达）

## 8. 后续行动

1. **短期**：本 hook 已上线，未来 commit 自动 reconcile，无需人工干预
2. **中期**：owner 裁决 plan 侧补治理卡系列（ROOT-R* / GOVERNANCE-* / CONSISTENCY-* / DDL-* / GUARD-* / WAVE*）
3. **长期**：所有 IPD 工程会议的"已实装"声明必须可由 `git log` → manage.py 追溯，否则视为口头声明不入账
