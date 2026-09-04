# IPD系统_六阶段标准动作清单_v3.md（骨架）

> **状态**：❌ 缺失（等待 Gavin 提供原文）
> **重要性**：⭐⭐⭐⭐⭐（P1 IPD 主干必备——69 动作 seed 数据源）

---

## 用途

- P1 阶段（IPD 主干）核心数据
- 系统管理 → 动作清单配置的 seed 数据
- 阶段可视化的内容来源

## 动作清单（来自 _导航地图 §6 整理）

### 数量分布（69 个 = 12 + 13 + 11 + 12 + 8 + 9 + 4）

| 阶段 | 编号范围 | 数量 | 角色（深管 / 轻管） |
|---|---|---|---|
| **概念（Concept）** | C01–C12 | 12 | （混合） |
| **计划（Plan）** | P01–P13 | 13 | （混合） |
| **开发（Development）** | D01–D11 | 11 | 研发PM 主导，默认轻管 |
| **验证（Validation）** | V01–V12 | 12 | 研发PM 主导，默认轻管 |
| **发布（Launch）** | L01–L08 | 8 | （混合） |
| **生命周期（Life Cycle）** | LC01–LC09 | 9 | （混合） |
| **KPI 归集** | K01–K04 | 4 | （产品组长录入） |
| **合计** | | **69** | **深管 42 / 轻管 27** |

### 阻断性 vs 非阻断性

- **阻断 38 / 非阻断 31**（来自开发说明书 BR-IPD-02）

### 特殊动作

| 编号 | 特殊处理 | 来源 |
|---|---|---|
| Z01–Z05 | 新增（v3 prompt 引入） | BR-IPD-02 |
| Z06 / Z07 / Z10 | 不实现 | §14.2 |
| C10 | 升级深管非阻断 | BR-IPD-02 |
| C05 | 维持轻管 | BR-IPD-02 |
| P10 / V02 | 法规认证保留阻断 + 登记证书编号 + 通过日期 | BR-IPD-05 例外一 |
| D11 / Z01 | BioCV：必须登记 FAR / FRR | BR-IPD-05 例外二 |
| V11 / Z04 | SOLUTION 模板下自动转深管 | BR-IPD-05 例例三 |
| D11 / Z01 | BioCV 项目自动挂 C12 | 阶段门禁 |

### 模板自动挂动作（BR-PROD-02）

新建项目选模板时自动挂载：

- HARDWARE（硬件）：C01–C12 + P01–P13 + D01–D11 + V01–V12 + L01–L08
- SOFTWARE（软件）：同上
- SOLUTION（解决方案）：同上 + V11 / Z04 转深管

### 字段（每动作）

```sql
stage_actions (
  id BIGINT PK,
  action_code VARCHAR(8) UNIQUE,           -- C01 / P05 / D11 等
  action_name VARCHAR(64),
  stage_id BIGINT NOT NULL,                -- 关联 project_stages.id
  template_type VARCHAR(16),               -- HARDWARE / SOFTWARE / SOLUTION
  depth VARCHAR(8) NOT NULL,                -- DEEP / LIGHT
  is_blocking TINYINT(1) DEFAULT 0,
  is_bio_feature TINYINT(1) DEFAULT 0,      -- D11 / C12 驱动
  far_value DECIMAL(5,2) NULL,             -- D11 FAR
  frr_value DECIMAL(5,2) NULL,             -- D11 FRR
  cert_no VARCHAR(64) NULL,                 -- V02 认证编号
  cert_passed_at DATETIME NULL,
  responsible_role VARCHAR(32),              -- MARKET_PM / RD_PM
  sop_revision_id BIGINT NULL,
  -- + BaseEntity 公共字段
)
```

## 引用位置

- `docs/开发说明/开发说明书.md` §5.4 BR-IPD-02、§5.5 BR-IPD-04、§5.4 BR-IPD-05b
- `docs/开发说明/spec/_导航地图.md` §六 已解决项（69 动作完整编号）

## 等待填充

- [ ] 完整 69 动作的字段数据（每个动作的所有字段）
- [ ] 6 个 Z 动作的最终状态（v3 是否要做 / 不做）
- [ ] SOP 绑定关系（哪些深管动作绑 SOP、初始版本号）

## 临时替代

在原文未到位前，**P1 阶段**需要从 v3 prompt 或 Gavin 直接获取完整 69 动作定义。

---

**最后更新**：2026-09-04