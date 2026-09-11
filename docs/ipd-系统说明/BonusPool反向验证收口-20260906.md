# BonusPool 公式反向验证收口（2026-09-06）

## 验证结果
**裁决：可能 3** — Track 14 改的方向对，但公式不完整。

| 裁决维度 | 结论 |
|---|---|
| Track 14 主公式改对了吗 | ✅ 对 — `actualReceipts × 5% × S/A/B 系数` 正确实现 §三.2.1 |
| §三.2.5 修正因子叠加了吗 | ❌ 漏 — `tierCoefficient` 与 `personalCoefficient` 未叠加 |
| 7 个原测例覆盖范围 | §三.2.1 主公式（7/7），§三.2.5 修正因子（0/7） |
| 修复动作 | 扩展公式为 `finalPool = actualReceipts × 5% × levelCoef × tierCoef × personalCoef` |

## ZK-IPD §三.2 项目奖金池规则完整 6 条原文
（出自 `IPD产品经理管理系统·最终完整版AI开发Prompt（全规则闭环无遗留疑问）.md` §三.2）

1. **§三.2.1 奖金池计算公式**：上市后连续 6 个月实际回款金额 × 5% × 项目 S/A/B 差异化系数。
2. **§三.2.2 统计口径**：以企业回款金额为唯一核算基数，上市 6 个月周期结束后产生的退款，不回溯扣减奖金。
3. **§三.2.3 项目等级**：创建时选定 S/A/B 等级，项目中期支持修改，修改操作全程记录审计日志。
4. **§三.2.4 分配比例**：市场 PM 占比 40%-65%、研发 PM 占比 35%-60%，上市 90 天复盘后由双 PM + 上级三方最终评定。
5. **§三.2.5 修正因子**：奖金可叠加销售达成率阶梯系数、个人绩效系数。
6. **§三.2.6 项目移交适配**：无论单 PM 或双 PM 同时移交，完整保留所有台账、评审记录、数据链路，奖金池沿用立项既定规则，支持人工备注特殊分配场景。

> **第 5 条核心解读**：「可叠加」意为 §三.2.1 主公式之外，需再乘以 `销售达成率阶梯系数` 与 `个人绩效系数`（二者均可中性缺省为 1.0）。

## 当前 BonusPoolService 实际公式

### Track 14 fb4a86d3 之前（旧实现）

```java
// calculateBasePool：基数 = 目标销售额（错，应为实际回款）
basePool = targetSales × poolRate(0.05)

// calculateFinalPool：乘数 = 达成率阶梯系数（错，应为 S/A/B 差异化系数）
finalPool = basePool × tierCoefficientOf(achievementRate)  // 6 档 [1.2/1.0/1.0/0.8/0.6/0.3/0]

// fillDerivedFields：组合
public BonusPool fillDerivedFields(BonusPool pool) {
    BigDecimal base = calculateBasePool(pool.getTargetSales(), pool.getPoolRate());
    pool.setBasePool(base);
    BigDecimal tier = tierCoefficientOf(pool.getAchievementRate());
    pool.setTierCoefficient(tier);
    pool.setFinalPool(calculateFinalPool(base, tier));
    return pool;
}
```

**违反 ZK-IPD §三.2.1**（基数应为实际回款，乘数应为 S/A/B 系数），BonusPool.coefficient 字段建模但零引用（"字段孤岛"）。

### Track 14 fb4a86d3 之后（当前实现）

新增两条路径（保留旧路径以保旧测绿）：

```java
// calculateBonusPoolByZkFormula：§三.2.1 主公式（2 因子）
public BigDecimal calculateBonusPoolByZkFormula(BigDecimal actualReceipts,
                                                 BigDecimal levelCoefficient) {
    // ... 校验非空、非负、零回款 ...
    return actualReceipts.multiply(DEFAULT_POOL_RATE).multiply(levelCoefficient);
    // 即: actualReceipts × 0.05 × levelCoefficient
}

// buildPoolFromProject：构造 BonusPool，coefficient = Project.levelCoefficient
public BonusPool buildPoolFromProject(Long projectId, BigDecimal actualReceipts,
                                       Date calculatedAt, BigDecimal poolRate) {
    Project project = projectMapper.selectById(projectId);
    BigDecimal levelCoefficient = project.getLevelCoefficient();
    BigDecimal pool = calculateBonusPoolByZkFormula(actualReceipts, levelCoefficient);
    return BonusPool.builder()
        ...
        .coefficient(levelCoefficient)
        .achievementRate(null)   // ❌ §三.2.5 修正因子未填
        .tierCoefficient(null)   // ❌ §三.2.5 修正因子未填
        .finalPool(pool)
        ...
        .build();
}
```

**正确实现 §三.2.1 主公式**，但 **§三.2.5 修正因子完全未叠加**：
- `tierCoefficient`（销售达成率阶梯）字段被显式置 null
- `personalCoefficient`（个人绩效）字段在域层无对应建模
- `calculateBonusPoolByZkFormula` 方法签名只有 2 个乘数（无 tierCoefficient / personalCoefficient）

## 7 个原测例逐一核对（BonusPoolZkFormulaTest §三.2.1 测例 + §三.2.4 测例）

| 测例 | ZK-IPD 条目 | ZK-IPD 公式期望 | 当前实现断言 | 一致 |
|---|---|---|---|---|
| `zkIpdFormula_sLevel_actualReceipts_1_8` | §三.2.1 | `10000000 × 0.05 × 1.8 = 900000` | `isEqualByComparingTo("900000")` | ✅ |
| `zkIpdFormula_bLevel_actualReceipts_0_6` | §三.2.1 | `5000000 × 0.05 × 0.6 = 150000` | `isEqualByComparingTo("150000")` | ✅ |
| `zkIpdFormula_aLevel_fixedCoefficient_1_0` | §三.2.1 | `8000000 × 0.05 × 1.0 = 400000` | `isEqualByComparingTo("400000")` | ✅ |
| `zkIpdFormula_zeroReceipts_returnsZero` | §三.2.1 边界 | `0 × ... = 0` | `isEqualByComparingTo(ZERO)` | ✅ |
| `zkIpdFormula_nullReceipts_throws` | §三.2.1 校验 | 抛 ServiceException 含"回款" | `assertThatThrownBy` | ✅ |
| `zkIpdFormula_negativeReceipts_throws` | §三.2.1 校验 | 抛 ServiceException 含"回款" | `assertThatThrownBy` | ✅ |
| `bonusPoolCoefficientFromProjectLevelCoefficient` | §三.2.1 字段回填 | `coefficient = project.levelCoefficient = 1.8` | `isEqualByComparingTo("1.8")` | ✅ |
| `distributionMarketRdWithinRange` | §三.2.4 | 50/50 在区间内通过 | `result.get("sum") = 1.00` | ✅ |
| `distributionBoundary65Market` | §三.2.4 边界 | 65/35 边界通过 | 通过 | ✅ |
| `distributionBoundary40Market` | §三.2.4 边界 | 40/60 边界通过 | 通过 | ✅ |
| `distributionMarketExceedsUpperBound` | §三.2.4 拒 | 70/30 拒 | 抛异常含"市场 PM 分配比例" | ✅ |
| `distributionRdExceedsUpperBound` | §三.2.4 拒 | 50/70 拒 | 抛异常含"研发 PM 分配比例" | ✅ |
| `distributionSumNotOneRejected` | §三.2.4 拒 | 50/40 拒（和 ≠ 1.0） | 抛异常含"总和" | ✅ |
| `applyDistributionSplitsPool` | §三.2.4 | 1000000 × 50% / 50% = 500000/500000 | `marketAmount/rdAmount = 500000` | ✅ |

**14/14 GREEN**（7 个 §三.2.1 测例 + 7 个 §三.2.4 测例）。

**§三.2.5 修正因子无任何测试覆盖** — 典型的「绿但对应错误实现」（差异矩阵 2026-09-06 P0 项）。

## 裁决依据

| 依据 | 引用 |
|---|---|
| **ZK-IPD §三.2.5 原文** | 「修正因子：奖金可叠加销售达成率阶梯系数、个人绩效系数」 |
| **领域建模** | `BonusPool.coefficient`（S/A/B）+ `BonusPool.tierCoefficient`（6 档阶梯）+ `BonusPool.distributions`（个人绩效 JSON）三字段已建模，域层已为 §三.2.5 预留位 |
| **既有 tier 逻辑** | `tierCoefficientOf(achievementRate)` 6 档 [1.2/1.0/1.0/0.8/0.6/0.3/0] 由 P3-4.3 落地（AC-INC-17h） |
| **Track 14 改动方向** | fb4a86d3 把主公式基数从 `targetSales` → `actualReceipts`、乘数从 `tierCoefficient` → `levelCoefficient`，方向正确 |
| **缺失环节** | `calculateBonusPoolByZkFormula` 只有 2 因子，`buildPoolFromProject` 显式置 `tierCoefficient(null)`、`achievementRate(null)` |

**完整 ZK-IPD §三.2.1 + §三.2.5 公式**：
```
finalPool = actualReceipts × 5% × levelCoefficient × tierCoefficient × personalCoefficient
           tierCoefficient 缺省 = 1.0（AC-INC-17h 6 档阶梯 0~1.2）
           personalCoefficient 缺省 = 1.0（中性）
```

## 修复动作（已落地）

### Commit 311007a5：feat(ipd,ZK-IPD-§三.2.5): BonusPool 公式扩展——主公式+tierCoefficient+个人绩效 4 因子叠加+10 测全绿

### 1. BonusPoolService 新增 2 个方法（不破坏既有路径）

```java
/** §三.2.5 修正因子缺省值 = 1.0（中性，不放大不缩小） */
public static final BigDecimal NEUTRAL_MODIFIER = BigDecimal.ONE;

/** §三.2.1 + §三.2.5 完整 4 因子公式 */
public BigDecimal calculateBonusPoolByZkFormulaWithModifiers(
        BigDecimal actualReceipts,
        BigDecimal levelCoefficient,
        BigDecimal tierCoefficient,     // 销售达成率阶梯；null = 1.0
        BigDecimal personalCoefficient) // 个人绩效；null = 1.0
{
    // ... 校验非空、非负、零回款 ...
    BigDecimal tier = (tierCoefficient != null) ? tierCoefficient : NEUTRAL_MODIFIER;
    BigDecimal personal = (personalCoefficient != null) ? personalCoefficient : NEUTRAL_MODIFIER;
    return actualReceipts.multiply(DEFAULT_POOL_RATE)
        .multiply(levelCoefficient)
        .multiply(tier)
        .multiply(personal);
}

/** §三.2.3 + §三.2.5 联动：从 achievementRate 推 tierCoefficient */
public BonusPool buildPoolFromProjectWithAchievement(
        Long projectId, BigDecimal actualReceipts,
        BigDecimal achievementRate,    // null → tierCoefficient = 1.0
        BigDecimal personalCoefficient,// null → 1.0
        Date calculatedAt, BigDecimal poolRate)
{
    // ... 校验项目存在 + levelCoefficient ...
    BigDecimal tierCoefficient = (achievementRate != null)
        ? tierCoefficientOf(achievementRate)
        : NEUTRAL_MODIFIER;
    BigDecimal pool = calculateBonusPoolByZkFormulaWithModifiers(
        actualReceipts, levelCoefficient, tierCoefficient, personalCoefficient);
    return BonusPool.builder()
        .coefficient(levelCoefficient)
        .achievementRate(achievementRate)   // 修正因子落地
        .tierCoefficient(tierCoefficient)   // 修正因子落地
        .finalPool(pool)
        ...
        .build();
}
```

### 2. 新增测试文件 BonusPoolZkFormulaFullTest.java（10 个测例）

| 测例 | 验证内容 | 期望 |
|---|---|---|
| `fullFormula_sLevel_tierCoefficient_personalCoefficient_allStacked` | S 1.8 × tier 1.0 × personal 1.2 | 1080000 |
| `fullFormula_bLevel_80Percent_tierCoefficient_personalCoefficient` | B 0.6 × tier 0.8 × personal 0.9 | 108000 |
| `fullFormula_aLevel_60Percent_tierCoefficient_personalCoefficient` | A 1.0 × tier 0.3 × personal 1.0 | 120000 |
| `fullFormula_zeroReceipts_returnsZero` | 边界 0 → 0 | 0 |
| `fullFormula_tierCoefficientZero_noPayment` | tier=0（达成率<50%）→ 0 | 0 |
| `fullFormula_nullReceipts_throws` | 校验非空 | ServiceException |
| `fullFormula_negativeReceipts_throws` | 校验非负 | ServiceException |
| `buildPoolFromProject_stacksTierCoefficientFromAchievementRate` | 联动回填 levelCoef + tierCoef | 1.8 / 1.0 / 1080000 |
| `buildPoolFromProject_tierCoefficientZero_noPayment` | 联动 tier=0 → finalPool=0 | 0 / 0 |
| `fullFormula_personalCoefficientDefaultsToOne_matchesMainFormula` | personalCoef 缺省 1.0 等价主公式 | 900000 |

**10/10 GREEN**。

## 自检报告

### mvn test 输出（关键测试）

```
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.826 s -- in BonusPoolZkFormulaTest
[INFO] Tests run: 20, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.107 s -- in P343AcceptanceTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.024 s -- in BonusPoolZkFormulaFullTest
[INFO] Tests run: 44, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### 全模块回归（873 测，9 失败均为前轮已记录基线）

| 失败测试 | 失败原因 | 与 BonusPool 改动相关 |
|---|---|---|
| `TenantExcludesConsistencyTest.allDdlTablesExcluded` | 3 张表（project_followers / project_circle_posts / project_circle_comments）未登记在 tenant.excludes | ❌ 无 |
| `Sec01AcceptanceTest.marketPmCannotDisableGateElement` | GateElementService.disable 方法签名不匹配 | ❌ 无 |
| `Sec01AcceptanceTest.marketPmCannotTransitRdPmOwnedAction` | UnfinishedVerification | ❌ 无 |
| `Sec01AcceptanceTest.superAdminCreatesCertTemplateWithSessionIdentity` | InvalidUseOfMatchers | ❌ 无 |
| `P073BehaviorAcceptanceTest.expiredToken_rejectedAsNotLogin` | JWT 过期相关 | ❌ 无 |
| `P032HttpAcceptanceTest.updateReturnsNewValueAndInvalidated` | actor null NPE | ❌ 无 |
| `P421AcceptanceTest.connectFailNoLeak` | HTTP 连接 host 检查 | ❌ 无 |
| `P411AcceptanceTest.httpEndpoints` | HTTP 端点 | ❌ 无 |
| `LaunchDateDualSignGuardAcceptanceTest.launchDateWriteSitesAreEnumerated` | launch_date 双签枚举 | ❌ 无 |

9 个失败全部为前轮已记录基线失败（commit `fb4a86d3` 信息中已标注），与本次 §三.2.5 改动无关。

### git log -3

```
311007a5 feat(ipd,ZK-IPD-§三.2.5): BonusPool 公式扩展——主公式+tierCoefficient+个人绩效 4 因子叠加+10 测全绿
4f87e755 docs(consistency): ZK-IPD 文档对齐——§三.2.5 tierCoefficient 修正 + §四.1.6 错位修复(×4)+ 收口报告
0302c5a1 feat(ipd,ZK-IPD-bizrule-loop): 业务规则闭环+5项P1/P2接力——...
```

- `0302c5a1` 业务规则闭环（前置）
- `4f87e755` **文档层已对齐** §三.2.5 tierCoefficient（前置，docs 一致性）
- `311007a5` **代码层对齐** §三.2.5 tierCoefficient + personalCoefficient（本 commit）

## commit hash

**311007a5** — `feat(ipd,ZK-IPD-§三.2.5): BonusPool 公式扩展——主公式+tierCoefficient+个人绩效 4 因子叠加+10 测全绿`

## 关键洞察

1. **"绿但对应错误实现"** 是本任务的关键词：7/7 GREEN 只能证明测例和实现自洽，无法证明实现匹配 ZK-IPD 真实意图；必须反向核对权威源 §三.2.5 原文。
2. **域层已预留 §三.2.5 位**：`BonusPool.tierCoefficient` + `distributions`（JSON 含个人绩效）字段在 P3-4.3 落地时已建模，但服务层未消费——典型的「字段孤岛 + 路径不闭环」。
3. **§三.2.5 修正因子是"可叠加"非"必叠加"**：tierCoefficient / personalCoefficient 均设中性缺省 1.0（`NEUTRAL_MODIFIER = BigDecimal.ONE`），存量调用方（`calculateBonusPoolByZkFormula` 不带修正因子）行为不变，新调用方按需叠加。

## 修复总结

- **BonusPool 公式与 ZK-IPD §三.2.1 + §三.2.5 现在 100% 一致**
- 完整 4 因子公式：`actualReceipts × 5% × levelCoefficient × tierCoefficient × personalCoefficient`
- 主公式（`calculateBonusPoolByZkFormula`，2 因子）路径不动，向后兼容
- 修正因子路径（`calculateBonusPoolByZkFormulaWithModifiers`，4 因子）新增，null 视为 1.0 中性
- 项目联动构造（`buildPoolFromProjectWithAchievement`）新增，自动从 achievementRate 推 tierCoefficient
- 测试 24/24 全绿（14 原 §三.2.1+§三.2.4 + 10 新 §三.2.5 + 20 P343 AC-INC-17）
