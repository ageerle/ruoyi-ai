---
source: 基于 v3 Prompt §10 审计日志规则 + 开发说明书 §13 演示数据 + 4 个奖金算例
collected: 2026-09-04
generated_by: Claude Code（基于已知数据点 + V3 规则自动生成）
status: ✅ 已生成
note: ZK-IPD 未发现现成 mock-data.js；本文件由 Claude Code 基于已确认的 4 个算例 + 13 角色 + 3 项目 + V3 决策表（全部已确认）综合生成
---

# IPD 产品经理管理系统 — 演示数据

> 本文件供前端原型 + 后端 seed 使用。所有数据**符合 V3 Prompt + 开发说明书约束**：
> - 双 PM 制度（市场 PM / 研发 PM）
> - 6 阶段 69 动作（含 P10 / V02 / D11 / V11 特殊动作）
> - 5 Gate 双签（33 项要素 + 14 否决项）
> - L1–L5 津贴（1000 / 1500 / 2000 / 2500 / 3000）
> - 奖金池公式（目标销售额 × 5% × 系数）
> - 6 档达成率阶梯（>120 / 100-120 / 85-99.99 / 70-84.99 / 50-69.99 / <50）

---

## 1. 人员（13 个，按 V3 Prompt §8 角色分组）

```js
export const persons = [
  // ===== 超管（1）=====
  { id: 'P001', employeeNo: 'ADMIN-001', name: 'admin-root', role: 'super_admin',
    groupId: 'G000', level: null, accountStatus: 'ACTIVE',
    title: '超级管理员', email: 'admin@entropy.com' },

  // ===== 市场 PM 组长（3）=====
  { id: 'P002', employeeNo: 'PM-MKT-LDR-A', name: 'PM-MKT-LDR-A', role: 'market_pm',
    groupId: 'G001', level: 'L5', accountStatus: 'ACTIVE',
    title: '市场产品组长 A' },
  { id: 'P003', employeeNo: 'PM-MKT-LDR-B', name: 'PM-MKT-LDR-B', role: 'market_pm',
    groupId: 'G002', level: 'L5', accountStatus: 'ACTIVE',
    title: '市场产品组长 B' },
  { id: 'P004', employeeNo: 'PM-MKT-LDR-C', name: 'PM-MKT-LDR-C', role: 'market_pm',
    groupId: 'G003', level: 'L5', accountStatus: 'ACTIVE',
    title: '市场产品组长 C' },

  // ===== 研发 PM 组长（3）=====
  { id: 'P005', employeeNo: 'PM-RD-LDR-A', name: 'PM-RD-LDR-A', role: 'rd_pm',
    groupId: 'G001', level: 'L5', accountStatus: 'ACTIVE',
    title: '研发产品组长 A' },
  { id: 'P006', employeeNo: 'PM-RD-LDR-B', name: 'PM-RD-LDR-B', role: 'rd_pm',
    groupId: 'G002', level: 'L5', accountStatus: 'ACTIVE',
    title: '研发产品组长 B' },
  { id: 'P007', employeeNo: 'PM-RD-LDR-C', name: 'PM-RD-LDR-C', role: 'rd_pm',
    groupId: 'G003', level: 'L5', accountStatus: 'ACTIVE',
    title: '研发产品组长 C' },

  // ===== 市场 PM（3）=====
  { id: 'P008', employeeNo: 'PM-MKT-01', name: 'PM-MKT-01', role: 'market_pm',
    groupId: 'G001', level: 'L1', accountStatus: 'ACTIVE' },
  { id: 'P009', employeeNo: 'PM-MKT-02', name: 'PM-MKT-02', role: 'market_pm',
    groupId: 'G002', level: 'L2', accountStatus: 'ACTIVE' },
  { id: 'P010', employeeNo: 'PM-MKT-03', name: 'PM-MKT-03', role: 'market_pm',
    groupId: 'G003', level: 'L3', accountStatus: 'ACTIVE' },

  // ===== 研发 PM（3）=====
  { id: 'P011', employeeNo: 'PM-RD-01', name: 'PM-RD-01', role: 'rd_pm',
    groupId: 'G001', level: 'L1', accountStatus: 'ACTIVE' },
  { id: 'P012', employeeNo: 'PM-RD-02', name: 'PM-RD-02', role: 'rd_pm',
    groupId: 'G002', level: 'L2', accountStatus: 'ACTIVE' },
  { id: 'P013', employeeNo: 'PM-RD-03', name: 'PM-RD-03', role: 'rd_pm',
    groupId: 'G003', level: 'L3', accountStatus: 'ACTIVE' },
];

// ===== 产品组（3 个）=====
export const productGroups = [
  { id: 'G001', name: '安防产品组', leaderMarketPm: 'P002', leaderRdPm: 'P005' },
  { id: 'G002', name: '考勤产品组', leaderMarketPm: 'P003', leaderRdPm: 'P006' },
  { id: 'G003', name: '生物识别产品组', leaderMarketPm: 'P004', leaderRdPm: 'P007' },
];
```

---

## 2. 产品（4 个，2 在售 + 1 在研 BioCV + 1 海外）

```js
export const products = [
  // 在售
  { id: 'PROD-AC-100', name: '门禁', level: 'A', status: 'on_sale',
    targetMarket: ['CN'], launchDate: '2024-03-15',
    targetSales: 5000000, actualSales: 4200000 },
  { id: 'PROD-AT-100', name: '考勤', level: 'A', status: 'on_sale',
    targetMarket: ['CN'], launchDate: '2024-06-20',
    targetSales: 3000000, actualSales: 2800000 },
  // 在研（BioCV）
  { id: 'PROD-AI-200', name: '智能分析', level: 'S', status: 'in_progress',
    targetMarket: ['CN', 'US'], launchDate: null,
    targetSales: 5000000, actualSales: 0,
    bioFeature: true, far: 0.02, frr: 0.015 },
  // 海外
  { id: 'PROD-ME-100', name: '中东门禁', level: 'S', status: 'in_progress',
    targetMarket: ['SA', 'AE'], launchDate: null,
    targetSales: 8000000, actualSales: 0,
    certRequired: ['SABER', 'SASO'] },  // 选沙特自动带出
];
```

---

## 3. 项目（3 个全周期）

```js
export const projects = [
  // ===== 项目 1：入门级门禁（HARDWARE，生命周期阶段）=====
  {
    id: 'PRJ-001', productId: 'PROD-AC-100', name: '入门级门禁 v1.0',
    template: 'HARDWARE', level: 'A', levelCoefficient: 1.0,
    status: 'ACTIVE', currentStage: 'LC',  // 生命周期运维
    marketPmId: 'P008', rdPmId: 'P011', groupId: 'G001',
    launchDate: '2024-03-15',  // L08 已录入
    salesPeriod: { start: '2024-03-15', end: '2024-09-15' },
    targetSales: 5000000, actualSales: 4200000,
    achievementRate: 84.0,  // 420/500
    gateReviews: [
      { gateId: 'G1', status: 'APPROVED', round: 1, signedBy: ['P008', 'P011'] },
      { gateId: 'G2', status: 'APPROVED', round: 1, signedBy: ['P008', 'P011'] },
      { gateId: 'G3', status: 'APPROVED', round: 5, signedBy: ['P008', 'P011'] },
      { gateId: 'G4', status: 'APPROVED', round: 1, signedBy: ['P008', 'P011'] },
      { gateId: 'G5', status: 'PENDING', round: 0, dueAt: '2024-12-15' },
    ],
    actions: {
      C: 12, P: 13, D: 11, V: 12, L: 8, LC: 9, total: 65,  // 69 - 4 (历史缺失)
      completed: 64, blocking: 36, depth: { deep: 41, light: 24 },
    },
  },

  // ===== 项目 2：智能考勤模块（SOFTWARE，验证阶段）=====
  {
    id: 'PRJ-002', productId: 'PROD-AT-100', name: '智能考勤模块',
    template: 'SOFTWARE', level: 'A', levelCoefficient: 1.0,
    status: 'ACTIVE', currentStage: 'V',  // 验证
    marketPmId: 'P009', rdPmId: 'P012', groupId: 'G002',
    launchDate: null,
    targetSales: 3000000, actualSales: 0,
    achievementRate: 0,
    gateReviews: [
      { gateId: 'G1', status: 'APPROVED', round: 1, signedBy: ['P009', 'P012'] },
      { gateId: 'G2', status: 'APPROVED', round: 1, signedBy: ['P009', 'P012'] },
      { gateId: 'G3', status: 'IN_PROGRESS', round: 3, dueAt: '2026-10-15' },
    ],
    actions: {
      C: 12, P: 13, D: 11, V: 6, L: 0, LC: 0, total: 42,
      completed: 38, blocking: 24, depth: { deep: 28, light: 14 },
    },
  },

  // ===== 项目 3：访客机 + 万傲瑞达集成（SOLUTION，发布阶段）=====
  {
    id: 'PRJ-003', productId: 'PROD-ME-100', name: '访客机 + 万傲瑞达集成',
    template: 'SOLUTION', level: 'S', levelCoefficient: 1.7,
    status: 'ACTIVE', currentStage: 'L',  // 发布
    marketPmId: 'P010', rdPmId: 'P013', groupId: 'G003',
    launchDate: '2026-08-01',
    targetSales: 8000000, actualSales: 0,
    achievementRate: 0,
    gateReviews: [
      { gateId: 'G1', status: 'APPROVED', round: 1, signedBy: ['P010', 'P013'] },
      { gateId: 'G2', status: 'APPROVED', round: 2, signedBy: ['P010', 'P013', 'P004'] },  // round 2 起组长列席
      { gateId: 'G3', status: 'APPROVED', round: 8, signedBy: ['P010', 'P013'] },
      { gateId: 'G4', status: 'APPROVED', round: 1, signedBy: ['P010', 'P013'] },
      { gateId: 'G5', status: 'PENDING', round: 0, dueAt: '2026-11-01' },
    ],
    actions: {
      C: 12, P: 13, D: 11, V: 12, L: 4, LC: 0, total: 52,  // 69 - 17 (V11/Z04 等自动转深管)
      completed: 50, blocking: 30, depth: { deep: 35, light: 17 },
    },
  },
];
```

---

## 4. 69 个标准动作（节选关键）

完整清单见 `IPD系统_六阶段标准动作清单_v3.md`。分布：

| 阶段 | 编号 | 数量 | 关键特殊动作 |
|---|---|---|---|
| **概念** | C01-C12 | 12 | C05 轻管 / C10 升级深管非阻断 / C12 生物特征合规 |
| **计划** | P01-P13 | 13 | P10 法规认证保留阻断 |
| **开发** | D01-D11 | 11 | D11 BioCV 必登记 FAR/FRR / 全部默认轻管（G-10） |
| **验证** | V01-V12 | 12 | V02 法规认证保留阻断 / V11 SOLUTION 自动转深管 |
| **发布** | L01-L08 | 8 | L08 录入上市日期（**双签不可改字段**） |
| **生命周期** | LC01-LC09 | 9 | LC03 终算 |

**6 个 Z 动作**（v3 引入，Z06/Z07/Z10 不实现）：

```js
const specialActions = {
  Z01: { stage: 'D', name: 'BioCV FAR/FRR 登记', depth: 'DEEP', blocking: true, parent: 'D11' },
  Z02: { stage: 'C', name: '生物特征合规', depth: 'DEEP', blocking: true, parent: 'C12' },
  Z03: { stage: 'V', name: 'BioCV 验证复核', depth: 'DEEP', blocking: true, parent: 'D11' },
  Z04: { stage: 'V', name: 'SOLUTION 模板深度化', depth: 'DEEP', blocking: false, parent: 'V11' },
  Z05: { stage: 'L', name: '智能产品上市', depth: 'DEEP', blocking: false, parent: 'L08' },
  // Z06 / Z07 / Z10 — 不实现（V3.1 §14.2 二期对接）
};
```

---

## 5. 五大 Gate + 33 项要素 + 14 项否决项

```js
const gateElements = {
  G1: {  // 立项 Go/No-Go
    name: '立项决策',
    elements: [
      { code: 'G1-1', name: '客户一手验证', criteria: '≥5 家', isVeto: true },
      { code: 'G1-2', name: '毛利率门槛', criteria: '≥ 产品线门槛（待 Gavin 配置）', isVeto: true },
      { code: 'G1-3', name: '市场窗口期分析', criteria: '≥18 个月', isVeto: true },
      { code: 'G1-4', name: '技术可行性', criteria: '预研完成', isVeto: false },
      { code: 'G1-5', name: '团队组建计划', criteria: '双 PM 锁定', isVeto: true },
      { code: 'G1-6', name: '上市目标', criteria: '明确日期 + 目标销售额', isVeto: true },
      { code: 'G1-7', name: '差异化论证', criteria: '竞品分析完成', isVeto: false },
    ],
  },
  G2: {  // 差异化确认
    name: '差异化决策',
    elements: [
      { code: 'G2-1', name: '产品定位', criteria: '明确', isVeto: true },
      { code: 'G2-2', name: '市场细分', criteria: '明确', isVeto: false },
      { code: 'G2-3', name: '毛利率门槛', criteria: '≥ 配置值', isVeto: true },
      { code: 'G2-4', name: '客户验证', criteria: '≥1 家书面意向（B 级）', isVeto: true },
      { code: 'G2-5', name: '竞品对标', criteria: '完成', isVeto: false },
      { code: 'G2-6', name: '差异化论证', criteria: '通过', isVeto: true },
    ],
  },
  G3: {  // 开发双周评审
    name: '过程性评审',
    elements: [
      { code: 'G3-1', name: '进度对齐', criteria: '偏差 < 10%', isVeto: false },
      { code: 'G3-2', name: '风险评审', criteria: '识别 + 应对', isVeto: false },
      { code: 'G3-3', name: '质量数据', criteria: '无 P0 缺陷', isVeto: false },
      { code: 'G3-4', name: '资源协调', criteria: 'OK', isVeto: false },
      { code: 'G3-5', name: '需求变更', criteria: '< 5%', isVeto: false },
    ],
  },
  G4: {  // GTM 就绪
    name: '上市前评审',
    elements: [
      { code: 'G4-1', name: '产品就绪度', criteria: 'L01-L08 全部完成', isVeto: true },
      { code: 'G4-2', name: '渠道就绪度', criteria: '≥ 3 个有效渠道', isVeto: true },
      { code: 'G4-3', name: '营销物料', criteria: '完成', isVeto: false },
      { code: 'G4-4', name: '培训就绪', criteria: '销售培训完成', isVeto: false },
      { code: 'G4-5', name: '定价审批', criteria: '超管批准', isVeto: true },
      { code: 'G4-6', name: '法务合规', criteria: '无重大风险', isVeto: false },
      { code: 'G4-7', name: '备货就绪', criteria: '首单库存 OK', isVeto: false },
      { code: 'G4-8', name: 'GTM 计划', criteria: '已批', isVeto: false },
    ],
  },
  G5: {  // 上市后 90 天复盘
    name: '复盘决策',
    elements: [
      { code: 'G5-1', name: '销量达成率', criteria: '≥ 70%', isVeto: true },
      { code: 'G5-2', name: '质量回访', criteria: 'P0 缺陷 = 0', isVeto: true },
      { code: 'G5-3', name: 'NPS', criteria: '≥ 30', isVeto: false },
      { code: 'G5-4', name: '客户案例', criteria: '≥ 3 个', isVeto: false },
      { code: 'G5-5', name: '技术债评估', criteria: '完成', isVeto: false },
      { code: 'G5-6', name: '二期规划', criteria: '已出', isVeto: false },
      { code: 'G5-7', name: '复盘报告', criteria: '通过', isVeto: false },
    ],
  },
};
// 7 + 6 + 5 + 8 + 7 = 33 要素；否决项 5 + 4 + 0 + 3 + 2 = 14
```

---

## 6. systemConfigs（参数表，6 项强制可切换）

```js
export const systemConfigs = {
  // 6 项涉钱参数（G-08 必须可切换）
  bonus: {
    poolBase: 'TARGET_SALES',                    // Q1 裁定：目标销售额
    salesSource: 'SHIPMENT',                      // Q2 裁定：出库（V3.1 §3.3 一致）
    performanceScoreStrategy: 'PROJECT_SCORE',   // Q3 裁定：项目维度
    coefficientDecider: 'G1_DUAL_SIGN',           // Q4 裁定：G1 双签 + 直接上级
    multiProjectSplit: 'NONE',                    // Q5 裁定：1:1 不分摊
    launchAnchor: 'L08_ACTION',                   // Q6 裁定：L08 录入
  },

  // 6 档达成率阶梯（E22 区间制，G-08 + 决策表已确认）
  achievementTiers: [
    { threshold: 120,  multiplier: 1.2, inclusiveRight: false },  // > 120%
    { threshold: 100,  multiplier: 1.0, inclusiveRight: true },   // 100-120% (含两端)
    { threshold: 85,   multiplier: 0.8, inclusiveRight: false },  // 85-99.99%
    { threshold: 70,   multiplier: 0.6, inclusiveRight: false },  // 70-84.99%
    { threshold: 50,   multiplier: 0.3, inclusiveRight: false },  // 50-69.99%
    { threshold: 0,    multiplier: 0,   inclusiveRight: false },  // < 50%
  ],

  // 绩效系数 5 档
  performanceTiers: [
    { threshold: 95, multiplier: 1.0 },
    { threshold: 85, multiplier: 0.8 },
    { threshold: 70, multiplier: 0.6 },
    { threshold: 60, multiplier: 0.3 },
    { threshold: 0,  multiplier: 0 },
  ],

  // 津贴（L1-L5）
  allowance: { L1: 1000, L2: 1500, L3: 2000, L4: 2500, L5: 3000 },

  // 贡献度（市场 + 研发 = 100%）
  contribution: {
    marketMin: 40, marketMax: 65,
    rdMin: 35, rdMax: 60,
    sum: 100,  // 强制和
  },

  // Gate 评审
  gate: {
    signDeadlineDays: 3,         // 签署期限 3 个自然日（决策表 D17）
    g1MinCustomerVerifications: 5, // G1-1 ≥ 5 家
    monthlyDeadlineDay: 5,        // 共担 KPI 月度截止日 = 次月第 5 个工作日
  },

  // 销售达成（K-01 销量达成率）
  kpi: {
    reviewWeights: { market: 40, rd: 40, self: 20 },  // 评定权重（和 100%）
  },
};
```

---

## 7. 4 个奖金算例（P3 TDD 必过）

### 算例 A：标准场景

```js
const exampleA = {
  inputs: { targetSales: 5000000, level: 'S', levelCoefficient: 1.5,
            achievementRate: 90, marketContribution: 55, performanceScore: 0.8 },
  steps: {
    pool: 5000000 * 0.05 * 1.5,           // 375000
    tier: 0.8,                            // 90% 落在 85-99.99% 档
    market: 375000 * 0.8 * 0.55 * 0.8,     // 132000 → 13.2 万
    rd: 375000 * 0.8 * 0.45 * 0.8,        // 108000 → 10.8 万
  },
  results: { pool: 375000, market: 132000, rd: 108000, total: 240000 },
};
```

### 算例 B：超额场景

```js
const exampleB = {
  inputs: { targetSales: 5000000, level: 'S', levelCoefficient: 1.5,
            achievementRate: 130, marketContribution: 55, performanceScore: 1.0 },
  steps: {
    pool: 5000000 * 0.05 * 1.5,           // 375000
    tier: 1.2,                            // 130% > 120% 档
    market: 375000 * 1.2 * 0.55 * 1.0,     // 247500 → 24.75 万
    rd: 375000 * 1.2 * 0.45 * 1.0,        // 202500 → 20.25 万
  },
  results: { pool: 375000, market: 247500, rd: 202500, total: 450000 },
};
```

### 算例 C：边界 100%（含端点）

```js
const exampleC = {
  inputs: { targetSales: 5000000, level: 'S', levelCoefficient: 1.5,
            achievementRate: 100, marketContribution: 55, performanceScore: 1.0 },
  steps: {
    pool: 375000,
    tier: 1.0,                            // 100% 命中 100-120% 档（inclusiveRight: true）
    market: 375000 * 1.0 * 0.55 * 1.0,     // 206250 → 20.625 万
    rd: 375000 * 1.0 * 0.45 * 1.0,        // 168750 → 16.875 万
  },
  results: { pool: 375000, market: 206250, rd: 168750, total: 375000 },
};
```

### 算例 D：边界 99.99%（不进 1.0 档）

```js
const exampleD = {
  inputs: { targetSales: 5000000, level: 'S', levelCoefficient: 1.5,
            achievementRate: 99.99, marketContribution: 55, performanceScore: 1.0 },
  steps: {
    pool: 375000,
    tier: 0.8,                            // 99.99% 落在 85-99.99% 档（不进 100-120%）
    market: 375000 * 0.8 * 0.55 * 1.0,     // 165000
    rd: 375000 * 0.8 * 0.45 * 1.0,        // 135000
  },
  results: { pool: 375000, market: 165000, rd: 135000, total: 300000 },
};
```

---

## 8. 需求池示例（游客提交）

```js
export const requirements = [
  {
    id: 'REQ-2026-001', code: 'D-2026-001',  // 游客需求 code
    productId: 'PROD-AC-100',  // 选产品
    submitter: '王先生（外部游客）', contact: 'wang@external.com',
    description: '希望门禁支持人脸 + 指纹双模认证',
    attachmentUrl: '/oss/req/2026-001/face-finger-spec.pdf',
    status: 'ACCEPTED',  // 已被 PM 受理
    code: 'D-2026-001',  // 8 位查询码
    submittedAt: '2026-08-15', acceptedAt: '2026-08-16',
    assignedPm: 'P008',  // 市场 PM
    projectId: 'PRJ-001',  // 关联项目
  },
];
```

---

## 9. 审计日志示例（hash 链）

```js
export const auditLogSamples = [
  {
    seq: 1001, action: 'create', entityType: 'project', entityId: 'PRJ-001',
    operator: 'P008', operatorName: 'PM-MKT-01', ip: '192.168.1.10',
    payload: { name: '入门级门禁 v1.0', template: 'HARDWARE' },
    prevHash: '00000000000000000000000000000000', currHash: 'abc123...', timestamp: '2024-01-15T10:00:00Z',
  },
  {
    seq: 1002, action: 'gate_submit', entityType: 'gate', entityId: 'G1-PRJ-001',
    operator: 'P008', payload: { elements: { 'G1-1': 'PASS', 'G1-2': 'PASS', ... } },
    prevHash: 'abc123...', currHash: 'def456...', timestamp: '2024-01-20T14:00:00Z',
  },
];
```

---

## 10. 状态机示例（7 实体）

```js
export const stateMachineExamples = {
  project: ['DRAFT', 'TEAMING', 'ACTIVE', 'SUSPENDED', 'ARCHIVED'],
  bidInvitation: ['OPEN', 'SELECTED', 'EXPIRED', 'CLOSED'],
  gateReview: ['PENDING', 'APPROVED', 'REJECTED', 'ABSTAINED_TIMEOUT'],
  deletionRequest: ['DRAFT', 'LEADER_REVIEW', 'ADMIN_REVIEW', 'DELETED', 'REJECTED'],
  requirement: ['SUBMITTED', 'ACCEPTED', 'EVALUATING', 'SCHEDULED', 'PROCESSING', 'CLOSED', 'ARCHIVED'],
  handover: ['DRAFT', 'CONFIRMED', 'COMPLETED'],
  personAccount: ['ACTIVE', 'FROZEN_PENDING_HANDOVER', 'DISABLED', 'RESIGNED'],
};
```

---

## 验证

4 个奖金算例的**手算结果**与 V3 Prompt §3.9 算例完全一致（开发说明书 §8.4 / V3 Prompt 第 7 节参数表交叉验证）：

- 算例 A: 13.2 万 / 10.8 万 ✅
- 算例 B: 24.75 万 ✅
- 算例 C: 20.625 万 ✅
- 算例 D: 16.5 万（不进 1.0 档，进 0.8 档）✅

6 档阶梯的**边界值**（100% / 99.99% / 120.1% / 119.99%）与决策表 E22 区间制 + 决策表 D17（3 个自然日签署期限）+ Q1-Q6 全部 6 项裁定一致。

---

**最后更新**：2026-09-04（Claude Code 基于 ZK-IPD v3 资源 + 开发说明书 + 决策表综合生成）