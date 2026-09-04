# docs/ipd-系统说明/ — 改造指南总览

> 本目录是 RuoYi-AI → IPD 产品经理管理系统**二次开发**的工作手册。所有做改造的工程师都应从这里开始。

---

## 目录结构与文件用途

```
docs/ipd-系统说明/
├── README.md                       ← 你在这里
├── drift-audit-report.md           ← Drift Audit 报告（哪些一致 / 哪些冲突）
├── 改造检查清单.md                  ← 静态检查脚本路径修正 + CI 集成方案
├── type-mapping.md                  ← PostgreSQL → MySQL 字段类型映射表
├── naming-convention.md             ← Sys* 风格 vs 文档命名约定
├── fork-原与外部资源清单.md           ← 本仓库源 + 10+ 个外部资源清单与适配状态
│
└── 外部资源/                        ← 开发说明书引用的 10 个外部资源骨架
    ├── IPD系统_AI开发主Prompt_v3.md
    ├── IPD系统_六阶段标准动作清单_v3.md
    ├── IPD系统_五大Gate评审要素_v1.md
    ├── IPD系统_验收清单.md
    ├── IPD系统_开发执行规则_AI必读.md
    ├── IPD系统_冲突裁决与最终待确认清单.md
    ├── IPD系统_待确认决策表_v2.md
    ├── assets_公共规范-通用.md
    ├── design-specs_后台-RuoYi-AI.md
    └── mock-data.js
```

---

## 各文件用途速查

| 文件 | 何时读 | 谁写 | 谁读 |
|---|---|---|---|
| `README.md`（本文件） | 第一次接触本目录 | Claude Code | 所有 |
| `drift-audit-report.md` | 改造前 / 阶段验收 | Claude Code | 二开 owner |
| `改造检查清单.md` | 提交 PR 前 / CI 失败排查 | Claude Code | 二开工程师 |
| `type-mapping.md` | 设计新表字段时 | Claude Code | 后端工程师 |
| `naming-convention.md` | 设计新表 / 新 controller 时 | Claude Code | 后端工程师 |
| `fork-原与外部资源清单.md` | 评估与上游同步 / 补外部文档时 | Claude Code | owner |
| `外部资源/` | 按需查阅（被开发说明书引用） | Claude Code + Gavin | 二开工程师 |

---

## 阅读路径建议

### 路径 A：第一次做改造（推荐）

1. **本文件**（README.md）—— 5 分钟，建立心智模型
2. **`drift-audit-report.md`** —— 30 分钟，理解基线 vs 目标的所有差异
3. **`改造检查清单.md`** —— 20 分钟，知道哪些检查项必须做
4. **按需读**：`type-mapping.md` / `naming-convention.md` —— 边写代码边查
5. **`fork-原与外部资源清单.md`** —— 评估与上游同步时

### 路径 B：只想做某个 P 阶段

1. `改造检查清单.md` —— 看本阶段的验收项
2. 对应模块的 `docs/wiki/wiki/modules/*.md` —— 看 RuoYi-AI 基线实现
3. `docs/开发说明/spec/_导航地图.md` —— 看 P 阶段要做的页面
4. `docs/开发说明/开发说明书.md` —— 看具体业务规则

### 路径 C：评估与上游 RuoYi-AI 同步

1. `fork-原与外部资源清单.md` —— 看 fork 源
2. `docs/wiki/wiki/index.md` —— 看基线能力地图
3. 评估哪些 RuoYi-AI 新功能可以引入；哪些 IPD 改造应该反向贡献

---

## 与其他文档目录的关系

```
RuoYi-AI 仓库根
├── README.md                         ← 原始 RuoYi-AI 介绍（基线）
├── README-IPD-OVERRIDE.md            ← 项目改造方向说明（最显眼）
├── CLAUDE.md                         ← Claude Code 项目级规范
│
└── docs/
    ├── 开发说明/                       ← 产品设计文档（目标系统规格，1305+ 行 + 49 页规格）
    ├── ipd-系统说明/                   ← 你在这里（二开工作手册）
    ├── wiki/                          ← RuoYi-AI 基线知识库（karpathy-llm-wiki 生成）
    ├── docker/                        ← Docker Compose 配置
    └── image/ + script/               ← 资源
```

**三层关系**：
- `开发说明/` 是产品设计的「**圣经**」（应该建成什么样）
- `ipd-系统说明/` 是改造工程的「**手册**」（怎么从基线改过来）
- `wiki/` 是 RuoYi-AI 基线的「**事实**」（基线到底是什么）

---

## 维护规则

| 触发条件 | 谁更新 | 怎么更 |
|---|---|---|
| 文档 G-04 与 v3 冲突 → 引用 v3 裁定 | Gavin | 改 `docs/开发说明/` |
| RuoYi-AI 上游重大更新 | Claude Code | update `fork-原与外部资源清单.md` |
| 完成 P 阶段验收 | 二开 owner | update `改造检查清单.md` |
| 新增 drift 发现 | Claude Code | update `drift-audit-report.md` |
| 新增 IPD 业务表 / 接口 | 后端工程师 | 遵循 `type-mapping.md` + `naming-convention.md` |

---

**最后更新**：2026-09-04