# ddl-field-usage-lint.cjs — 注册说明与 owner 决策点

关联卡：**ROOT-R5-P1-13**（entity-field-usage-lint hook 注册） / `ROOT-R5-字段孤岛治理-20260906.md`

状态：**草案已就位，未注册**。本文件与 `.claude/settings.json.proposed` 一并交 owner 决策。

---

## 1. hook 实际行为（已实测，非推测）

| 项 | 实测结果 |
|---|---|
| 触发工具 | `Write` / `Edit` / `MultiEdit` |
| 路径过滤 | **`*.sql`**（在 hook 内部第 150 行 `/\.(sql)$/i` 判定） |
| 检查内容 | 解析 `CREATE TABLE` / `ALTER TABLE ... ADD COLUMN` 的字段名，用 `git grep -c -w <字段> -- '*.java'` 数引用；`< 3` 处则告警 |
| 级别 | **WARN 不阻断**，`exit 0`（与 `sensitive-field-guard.cjs` 的 ERROR `exit 2` 区分） |
| 输出 | stderr 人读警告 + `---JSON---` 机器可读报告 |
| 实测延时 | 18 字段 `CREATE TABLE` 全程 **0.65s**（故 `timeout: 3000` 与 pom-edit-hint 对齐即可） |
| 异常鲁棒性 | 空 stdin / 非 JSON / 非 SQL 路径 → 静默 `exit 0`（4 项均实测通过） |

### ⚠️ 与卡片描述不一致（决策点 A）

卡片写的触发条件是 `**/ruoyi-ipd/domain/*.java`，但**代码实现的是 `*.sql`**。实测：

```
$ echo '{"tool_name":"Edit","tool_input":{"file_path":".../ipd/domain/Product.java",...}}' \
    | node .claude/helpers/ddl-field-usage-lint.cjs
exit=0   # 无任何输出 —— Java 文件不触发
```

另外 Claude Code 的 `matcher` **只匹配工具名，不匹配文件路径**，路径过滤必须在 hook 内部做。
所以 settings.json 里只能写 `"matcher": "Write|Edit|MultiEdit"`，
`**/ruoyi-ipd/domain/*.java` 这种 glob 写进 matcher 是无效的。

---

## 2. 🔴 阻断性缺陷：引用计数恒为 0（决策点 B，建议先修再注册）

`countJavaReferences()` 第 113 行的解析正则与 `git grep -c` 的真实输出格式**反了**：

- 代码假设：`<count>:<path>` → 正则 `/^(\d+):/`
- 实际输出：`<path>:<count>` → 应为 `/:(\d+)$/`

后果：`total` 永远累加不到，**任何字段都返回 0 引用**，于是 `refs < 3` 恒真——
hook **永远只会输出 WARN，永远不可能输出 OK**。实测对比：

| 字段 | 当前正则 | 修正正则 (`/:(\d+)$/`) |
|---|---|---|
| `del_flag` | 0 | 107 |
| `version` | 0 | 158 |
| `tenant_id` | 0 | 36 |
| `zzz_never_used_a` | 0 | 0 ✅ |

一次 18 字段的建表实测：**18/18 全部 WARN**，含 `id` / `status` / `version` / `remark`
这些明显已被消费的字段。按现状注册 = 纯噪音，且会训练出「忽略这个 hook」的习惯。

**修复是单行改动**（第 113 行正则），但按本任务约束「不动
`.claude/helpers/ddl-field-usage-lint.cjs`」，**未代为修改**，留给 owner 决策。

### 决策点 C：snake_case → camelCase 映射缺失

即使修好正则，DDL 里的 `del_flag` 与 Java 里的 `delFlag` 仍是两个词。实测两者都有独立命中
（`del_flag` 107 / `delFlag` 98），说明项目里 snake_case 主要出现在 mapper XML、
`@TableField` 注解、常量里。若只数 snake_case，会系统性低估 Entity/DTO 层的真实消费。
建议计数时**两种写法都数并取和**。

### 决策点 D：`scope` 参数是死参数

`countJavaReferences(fieldName, scope = 'ruoyi-modules/ruoyi-ipd/src/main/java')`
的 `scope` 声明了但**从未被使用**——`git grep` 的 pathspec 只传了 `'*.java'`，
即全仓库扫描。若本卡意图是只治理 IPD 模块的字段孤岛，需把 `scope` 真正接进 pathspec。

---

## 3. owner 决策清单

| # | 决策项 | 选项 | 建议 |
|---|---|---|---|
| A | 治理对象到底是 DDL 还是 Entity | ① 保持 `*.sql`（改卡片措辞）② 扩展成同时吃 `*.sql` + `ipd/domain/*.java` | ① 先落地，②列入后续卡 |
| B | 计数正则缺陷 | ① 先修再注册 ② 带病注册收集噪音 | **① 先修**，否则 100% 假阳性 |
| C | snake/camel 双写法计数 | ① 只数 DDL 原文 ② 两种都数取和 | ② |
| D | 扫描范围 | ① 全仓库 ② 收敛到 ruoyi-ipd | ② 与卡片意图一致 |
| E | 严格度等级 | **WARN**（`exit 0`，仅 stderr）／ ERROR（`exit 2`，阻断写入） | **WARN**。字段建而不接在开发中途是正常态，阻断会打断 TDD 流；等误报率降下来再议 ERROR |
| F | 阈值 `refs < 3` | 3 / 4（Entity+Mapper+DTO+Controller 四层） | 修好 B/C 后重新标定 |
| G | 全量回扫模式 | 当前 `--report=bootstrap|weekly` 是**未实现的占位**（只打印一行提示） | 需要历史字段回扫时再补 |

---

## 4. 合并与还原

```bash
# 合并（owner 决策后执行）
cp .claude/settings.json.proposed .claude/settings.json

# 还原
cp .claude/settings.json.bak .claude/settings.json

# 校验（settings.json 是 JSON，不能用 node --check，那是 JS 语法检查）
node -e 'JSON.parse(require("fs").readFileSync(".claude/settings.json","utf8"))' && echo OK

# 手工验证 hook（期望 exit 0 + stderr 警告）
echo '{"tool_name":"Edit","tool_input":{"file_path":"/tmp/x.sql","new_string":"ALTER TABLE t ADD COLUMN foo VARCHAR(8);"}}' \
  | node .claude/helpers/ddl-field-usage-lint.cjs; echo "exit=$?"
```

## 5. 建议追加到 .cjs 头部注释的段落（**未应用**，按约束保留原文件）

```js
 * ── 注册状态 ────────────────────────────────────────────────
 * 未注册。候选注册项见 .claude/settings.json.proposed
 * （PostToolUse / matcher "Write|Edit|MultiEdit" / timeout 3000）。
 * matcher 只匹配工具名，路径过滤由本文件第 150 行 /\.(sql)$/i 负责。
 *
 * 已知缺陷（注册前须裁决，详见 ddl-field-usage-lint.README.md）：
 *   B. countJavaReferences 正则 /^(\d+):/ 与 git grep -c 的
 *      <path>:<count> 输出格式相反 → 计数恒为 0 → 100% 假阳性
 *   C. 未做 snake_case → camelCase 映射（del_flag vs delFlag）
 *   D. scope 形参声明未使用，实际为全仓库扫描
 *   G. --report=bootstrap|weekly 为未实现占位
 * ────────────────────────────────────────────────────────────
```
