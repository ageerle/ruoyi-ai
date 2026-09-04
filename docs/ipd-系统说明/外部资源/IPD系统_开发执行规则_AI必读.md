# IPD系统_开发执行规则_AI必读.md（骨架）

> **状态**：❌ 缺失（等待 Gavin 提供原文）
> **重要性**：⭐⭐⭐⭐⭐（AI 二开 agent 的核心约束）

---

## 用途

- 任何 AI agent（二开 Claude / Cursor / Codex 等）**做 IPD 二开必读**
- 11 条硬约束 + 状态机速查 + 文件所有权 + 跳阶段禁令

## 已知章节（开发说明书 §3 已转录）

### 11 条硬约束（G-01 ~ G-11）

完整内容见 `docs/开发说明/开发说明书.md` §3。

### 状态机速查

```
项目      DRAFT → TEAMING → ACTIVE →（SUSPENDED）→ ARCHIVED
招标单    OPEN →（SELECTED / EXPIRED / CLOSED）
Gate 评审 PENDING →（APPROVED / REJECTED / ABSTAINED_TIMEOUT）
删除申请  DRAFT → LEADER_REVIEW →（ADMIN_REVIEW → DELETED / REJECTED）
需求      SUBMITTED → ACCEPTED → EVALUATING → SCHEDULED → PROCESSING → CLOSED / ARCHIVED
移交      DRAFT → CONFIRMED → COMPLETED
人员账号  ACTIVE → FROZEN_PENDING_HANDOVER → DISABLED → RESIGNED
```

**新增任何状态前，先确认它有出口**（G-01）。

## 引用位置

- `docs/开发说明/开发说明书.md` §3 全文引用
- `docs/开发说明/spec/_公共规范.md` 引用 §2（前端硬约束）
- `docs/ipd-系统说明/drift-audit-report.md` §A.5（基线已对齐项）

## 等待填充

- [ ] 文件所有权矩阵（哪个 P 阶段改哪个文件）
- [ ] 跳阶段禁令的具体细节
- [ ] AI agent 的「禁止操作清单」（如不可自动 commit、不可删文件、不可 push）

## 临时替代

本仓库 `docs/ipd-系统说明/drift-audit-report.md` + `docs/ipd-系统说明/改造检查清单.md` 已覆盖大部分硬约束的工程化落地。

---

**最后更新**：2026-09-04