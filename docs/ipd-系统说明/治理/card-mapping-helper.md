# 卡号批量 reconcile 操作手册（W21-B 收口）

> 工具：`docs/ipd-系统说明/vibe-kanban/batch-sync-commits.py`
> 映射表：`docs/ipd-系统说明/治理/卡号映射-20260906.json`（78 张卡 / 15 分类）

## 快速上手

```bash
# 1. 预览（不实际翻卡）
python3 docs/ipd-系统说明/vibe-kanban/batch-sync-commits.py \
  --dry-run --card-mapping docs/ipd-系统说明/治理/卡号映射-20260906.json --limit 300

# 2. 按 card-filter 只同步某一类（如 ROOT-R 系）
python3 docs/ipd-系统说明/vibe-kanban/batch-sync-commits.py \
  --dry-run --card-mapping docs/ipd-系统说明/治理/卡号映射-20260906.json \
  --card-filter '^ROOT-R' --limit 300

# 3. 确认无误后去掉 --dry-run 实际执行（走 manage.py set → 看板同步）
```

## 参数说明

| 参数 | 作用 |
|---|---|
| `--limit N` | 扫描最近 N 条 commit（默认 33） |
| `--card-mapping FILE` | 卡号映射 JSON（commit 前缀 → 卡号） |
| `--card-filter REGEX` | 正则过滤卡号（如 `^HIGH`） |
| `--dry-run` | 仅打印 what-if + JSON 摘要，不实际翻卡 |

## owner 决策点（映射表 _meta.owner_decision_topics）

1. 卡号命名体系是否在 plan 侧补登记（ROOT-R*/GOVERNANCE-*/CONSISTENCY-* 等）
2. SEC-FIX-HIGH-X.Y 与 HIGH-X.Y 是否合并为单一卡号
3. WAVE14-CONSOLIDATE 等 wave 系列是否引入阶段编号

## 未匹配 commit 的处理

dry-run 输出 `commits_matched=0` 属正常——映射表按 commit 前缀精确匹配，新 commit（映射表生成之后）需 owner 增量补录映射表。

## 维护约定

- 每完成一批卡闭环后，往映射表追加对应 commit 前缀
- 映射表 schema 见 `_meta.schema_version`
- 本手册与 R6 post-commit hook（自动同步）互补：hook 管 plan 内卡号，本工具管 plan 外 78 张
