# Doublecheck spec

## Goal
两文档树（docs/开发说明 + docs/ipd-系统说明）与根部门面文档（CLAUDE.md / README-IPD-OVERRIDE.md / AGENTS.md）达到全局一致、零噪音零污染：Q2=回款RECEIPT、硬约束=11条G-01~G-11、4算例=开发说明书§8.4、P4=12页、AC=235、外部资源=7核心已填充且5个已吸收历史件清理；全部修复有 path:line 证据且可复跑验证脚本 exit 0。

## Scope
IN：docs/开发说明/**（仅勘误级：错字/失效引用/数字对齐，不改产品决策）；docs/ipd-系统说明/**（工程级修复+清理）；CLAUDE.md；README-IPD-OVERRIDE.md；AGENTS.md（G-04 勘误通道口径）。OUT：产品业务决策内容；v3 等外部资源原文正文（mock-data.js 生成件勘误除外）；任何代码/pom/.claude/docs/wiki；git push。

## Acceptance criteria
①全树（docs+根md）grep『12 条硬约束』=0 且『G-12』=0；②mock-data.js salesSource=RECEIPT 且『Q2 裁定：出库』=0；③一致性报告§C Q2 行=回款、算例行与§8.4 一致；④『11 页』=0（导航地图 P4=12）；⑤外部资源/额外资源/ 目录不存在且 README/fork清单 无悬空引用（log.md 历史条目保留）；⑥CLAUDE.md 外部资源段=已填充+235 条 AC；⑦/tmp 验证脚本全断言 PASS exit 0。

## Failure modes
误删未被 v3 吸收的业务输入 → 已用证据排除（v3 清单含熵基 5 处、决策表:13 全部回填 v3）；把第一轮审计结论当权威 → 已用 v3:1135 原文反转 Q2；edit 破坏表格结构 → 一律先 read 后 edit、old_string 唯一匹配；清理后悬空引用 → 引用面已盘（仅 log.md 历史条目，保留）；勘误越权改产品决策 → 开发说明仅动数字/标题/引用级。

## Priorities
P0 事实口径反转（Q2/算例/11条）> P1 数字对齐（P4=12页、237→235、G-12 删除）> P2 噪音清理（5 历史件、骨架表述、缩进）> P3 门面同步与 log 登记 + commit（不 push）。

## Non-goals
不重写产品决策与业务规则；不生成 49 页原型；不新增 CI workflow（.github/workflows 待建项维持待建）；不 push；不动代码与依赖版本；不改 naming-convention/type-mapping 正文（已验证无过期口径）。
