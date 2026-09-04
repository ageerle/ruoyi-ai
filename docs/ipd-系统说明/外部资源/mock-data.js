# mock-data.js（骨架）

> **状态**：❌ 缺失（等待填充）
> **重要性**：⭐⭐⭐⭐（P0 演示 / 联调 / 测试必备）

---

## 用途

- 前端原型演示数据
- 后端 seed 数据来源（数据库初始数据）
- 自动化测试 fixture

## 关键内容（来自开发说明书 §13）

### 13.1 演示人员（13 个，按 v3 角色分组）

⚠️ **NEEDS CLARIFICATION**：原文档列出了真实姓名（傅志谦/杨波/文元彪…），与 BR-USER-01「PM 禁止页面手动新增；来源 = 第三方 API」冲突。**建议改用化名**。

| 角色 | 化名建议 | L | 备注 |
|---|---|---|---|
| 超管 | admin-root | — | 独立账号 |
| 市场PM 组长 | PM-MKT-LDR-A / B / C | L5 | 随 API 同步 |
| 研发PM 组长 | PM-RD-LDR-A / B / C | L5 | 同上 |
| 市场PM | PM-MKT-01 / 02 / 03 | L1–L3 | 同上 |
| 研发PM | PM-RD-01 / 02 / 03 | L1–L3 | 同上 |

### 13.2 演示产品（4 个）

```js
{
  // 在售
  'DEMO-AC-100': { name: '门禁', level: 'A', status: '在售' },
  'DEMO-AT-100': { name: '考勤', level: 'A', status: '在售' },
  // 在研
  'DEMO-AI-200': { name: '智能分析', level: 'S', status: '在研', bioCV: true },
  // 海外
  'DEMO-ME-100': { name: '中东门禁', level: 'S', targetMarkets: ['SA'] },
}
```

### 13.3 演示项目（3 个，与现有代码库对齐三个 IPD 全周期项目）

| 项目 | 模板 | 等级 | 阶段 | 状态 |
|---|---|---|---|---|
| DEMO-PRJ-001（入门级门禁） | HARDWARE | A | 生命周期（LC03 终算） | ACTIVE |
| DEMO-PRJ-002（智能考勤模块） | SOFTWARE | A | 验证 V06 | ACTIVE |
| DEMO-PRJ-003（访客机+万傲瑞达集成） | SOLUTION | S | 发布 L08 | ACTIVE |

每个项目保留：六阶段 69 动作实例 / 双 PM 绑定 / Gate 评审 / 需求变更 / KPI 打分 / 津贴台账 / 贡献度评定。

### 13.4 演示金额（用于奖金公式验证）

⚠️ 必须能在 P3 阶段复现 4 个算例：

- **算例 A**：目标 ¥500 万 / S 系数 1.5 / 达成率 90% / 市场贡献 55% / 绩效系数 0.8 → **市场 PM = 13.2 万**
- **算例 B**（验证 1.2 档）：达成率 130% → 市场 PM = **24.75 万**
- **算例 C**（验证回款口径）：目标 500 万，回款 350 万 → 达成率 = 70%
- **算例 D**（验证 120% 端点）：
 - 达成率 120% → 阶梯 1.0 → 市场 PM = **20.625 万**
 - 达成率 120.1% → 阶梯 1.2 → 市场 PM = **24.75 万**
 - 达成率 99.99% → 阶梯 0.8

### systemConfigs（参数配置）

```js
{
  bonus: {
    poolBase: 'TARGET_SALES',       // Q1
    salesSource: 'RECEIPT',          // Q2
    performanceScoreStrategy: 'PROJECT_SCORE',  // Q3
    coefficientDecider: 'G1_DUAL_SIGN',  // Q4
    multiProjectSplit: 'NONE',       // Q5
    launchAnchor: 'L08_ACTION',      // Q6
    achievementTiers: [
      { threshold: 120, multiplier: 1.2, inclusiveRight: false },
      { threshold: 100, multiplier: 1.0, inclusiveRight: true },
      { threshold: 85,  multiplier: 0.8, inclusiveRight: false },
      { threshold: 70,  multiplier: 0.6, inclusiveRight: false },
      { threshold: 50,  multiplier: 0.3, inclusiveRight: false },
      { threshold: 0,   multiplier: 0,   inclusiveRight: false },
    ],
    performanceTiers: [
      { threshold: 95, multiplier: 1.0 },
      { threshold: 85, multiplier: 0.8 },
      { threshold: 70, multiplier: 0.6 },
      { threshold: 60, multiplier: 0.3 },
      { threshold: 0,  multiplier: 0 },
    ],
  },
  allowance: {
    L1: 1000, L2: 1500, L3: 2000, L4: 2500, L5: 3000,
  },
  gate: {
    grossMarginThreshold: '未配置（G-06）',
    signDeadlineDays: 3,
  },
}
```

## 引用位置

- `docs/开发说明/开发说明书.md` §13 全节
- `docs/开发说明/spec/_导航地图.md` §六 NEEDS CLARIFICATION（演示数据合规性）
- P0 阶段前端原型 + P3 阶段奖金公式测试

## 等待填充

- [ ] 真实姓名是否改用化名（建议改，等 Gavin 决策）
- [ ] 演示项目完整数据（69 动作实例 + 双 PM 绑定 + Gate 评审 + KPI + 津贴台账）
- [ ] systemConfigs 完整参数

## 临时替代

P0 阶段前端原型可以用占位数据，后端 seed 在 P1 阶段开始填充。

---

**最后更新**：2026-09-04