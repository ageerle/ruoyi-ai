# IPD系统_五大Gate评审要素_v1.md（骨架）

> **状态**：❌ 缺失（等待 Gavin 提供原文）
> **重要性**：⭐⭐⭐⭐（P2 双 PM 机制必备）

---

## 用途

- P2 阶段（双 PM + Gate 机制）核心数据
- Gate 评审要素表的 seed 数据
- 系统管理 → Gate 评审要素配置

## Gate 与要素分布（来自 _导航地图 §六）

| Gate | 名称 | 要素数 | 否决项数 |
|---|---|---|---|
| **G1** | 立项 Go/No-Go | 7 | 5 |
| **G2** | 差异化确认 | 6 | 4 |
| **G3** | 开发双周评审 | 5 | 0（过程性性） |
| **G4** | GTM 就绪 | 8 | 3 |
| **G5** | 上市后 90 天复盘 | 7 | 2 |
| **合计** | | **33** | **14** |

## 字段（每要素）

```sql
gate_review_elements (
  id BIGINT PK,
  gate_id BIGINT NOT NULL,                  -- 关联 gates.id (G1-G5)
  element_code VARCHAR(16) UNIQUE,         -- G1-1 / G1-2 等
  element_name VARCHAR(128),
  pass_criteria TEXT,                       -- 通过标准
  is_veto TINYINT(1) DEFAULT 0,             -- 否决项标记
  sort_order INT,                            -- 显示顺序
  -- + BaseEntity 公共字段
)
```

### 三态判定（开发说明书 §_公共规范.md §六）

```sql
gate_element_results (
  id BIGINT PK,
  gate_id BIGINT NOT NULL,
  element_id BIGINT NOT NULL,
  decision VARCHAR(8) NOT NULL,             -- PASS / CONDITIONAL / FAIL
  responsible_person_id BIGINT NULL,         -- ⚠️ 条件必填
  closure_deadline DATETIME NULL,            -- ⚠️ 条件必填
  remark TEXT,
  -- + BaseEntity 公共字段
)
```

**判定映射**：

| decision | 中文 | 视觉 | 后续动作 |
|---|---|---|---|
| `PASS` | ✅ 通过 | 绿色 | — |
| `CONDITIONAL` | ⚠️ 带条件通过 | 黄色 | 必填责任人 + 关闭期限 |
| `FAIL` | ❌ 不通过 | 红色（否决项标记） | 否决整张 Gate，提交按钮置灰 |

## 引用位置

- `docs/开发说明/开发说明书.md` §5.6 BR-GATE-01b、§11 P2 验证
- `docs/开发说明/spec/_导航地图.md` §六 已解决项

## 等待填充

- [ ] 完整 33 项要素的字段数据
- [ ] 每个 Gate 的通过标准文本（用于 Gate 评审详情页）
- [ ] G1-5 / G2-4 的毛利率门槛值（NEEDS CLARIFICATION，待 Gavin 提供）

## 临时替代

P2 阶段需要从 v1 prompt 或 Gavin 获取完整 33 项要素定义。

---

**最后更新**：2026-09-04