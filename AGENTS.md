# AGENTS.md — ruoyi-ai

只收录「读代码推不出来、但直接决定任务成败」的雷区。架构 / 目录 / 完整命令见同目录 `CLAUDE.md`；二开方向总览见 `README-IPD-OVERRIDE.md`（优先级高于根 `README.md`）。

一句话定位：RuoYi-AI（Spring Boot 3.5.8 + Java 17 + Langchain4j 的 Maven 多模块**后端**仓库，父 POM revision 3.1.0）正在二开改造为「IPD 产品经理管理系统」。前端在独立仓库（ruoyi-web / ruoyi-admin 前端），别在这里找 Vue 代码。

## 范围与路由

- `docs/开发说明/**` 是产品设计事实源（"圣经"，G-04）：产品业务决策不可改；owner 已授权**勘误级更新**（错字 / 失效引用 / 数字对齐），勘误须在 `docs/ipd-系统说明/log.md` 登记；工程修复与补充文档一律写到 `docs/ipd-系统说明/` 下。
- Issue 流程：GitHub Issues（origin `wilson323/ruoyi-ai`，upstream `ageerle/ruoyi-ai`）→ `docs/agents/issue-tracker-github.md`；领域文档地图 → `docs/agents/domain.md`。
- **假红陷阱**（假绿的镜像）：本仓常有多个智能体会话同时工作在同一工作树，并发 `-am` / `clean` 构建会交叉重写 `target/`，制造大面积 `NoClassDefFoundError` 假红（实测 85 跑 71 Error 中 ≥66 为假红；也见过兄弟会话中途删文件导致 `需要 class、interface、enum 或 record` 的瞬时编译错）。复核方一律**错峰 + 单模块 + 不带 `-am` 不带 `clean`**：`mvn -o -pl ruoyi-modules/ruoyi-ipd -Dtest=Xxx test`。下结论前复查同一条命令是否已被兄弟会话改写，证据必带时间戳。
- **并发写单一写入者**：Java 源码、SSOT 看板镜像 `docs/ipd-系统说明/开发计划-看板镜像.md` 与本地看板由主协调会话串行写，其他会话只做只读探针 + 证据交付。`manage.py` 写前重算 PLAN 哈希、中途变更即中止——触发中止时重试或让路，**不要绕过**；同一指令被多会话重复执行时先登记归属，别各自建卡（详见 OPS-09）。
- 多智能体协同（Ruflo/claude-flow）的启用门槛、拓扑与命令见 `CLAUDE.md` §「Ruflo 多智能体协同底座」；单文件小改动不要上 swarm。

## 构建 / 测试（每条都吃过亏）

- 只在仓库根执行 `mvn`（父 POM 管 `<modules>`）；不要进子模块目录单独构建。Maven 装在 `/Users/mac/tools/maven/bin/mvn`（3.9.11）、JDK 17 在 `/Users/mac/tools/jdk-17/Contents/Home`，都不在精简 PATH 里——非交互 shell 先补 `export PATH="$HOME/tools/maven/bin:$PATH"` 和 `export JAVA_HOME="$HOME/tools/jdk-17/Contents/Home"`，否则依次报 `mvn: command not found` / `Unable to locate a Java Runtime`。
- **假绿陷阱**：Surefire 按 `<groups>${profiles.active}</groups>` 过滤（pom.xml:472），默认 dev profile 下没有 `@Tag("dev")` 的测试类被**静默跳过**——新测试不加 tag，"测试全绿"毫无意义。另一形态：把测试断言改成"现状"（例如把期望异常类型改成新类型）能让用例转绿而契约缺口仍在，收口前须确认绿的是**契约**不是**既有实现**。第三形态（WB-17-1 仲裁卡教训）：mock 造了真库写入路径不可能产生的数据组合（如 PENDING_SECOND 态配 confirmerId、`decision IS NULL` 但无任何生产者落 NULL 行）——单测全绿但真活卡恒空；三条硬规约与存量违法清单见 `docs/ipd-系统说明/mock合法性与已知死路登记-20260908.md`，生成测试时规约见 `.claude/skills/gen-test/SKILL.md`。
- `demo.enabled` 现**默认 false**（R8-P0-2 已把默认翻成关闭，父 `application.yml` 的 `demo:` 段）。一旦被改回 true，所有写操作被拦截并返回"演示模式，不允许操作"（白名单在同段 `demo.excludes`）。需要演示模式时在 `application-dev.yml` 显式覆盖，反之用 `-Ddemo.enabled=false`。
- 多租户默认开启：新建"租户共享"表必须登记父 `application.yml` 的 `tenant.excludes`，否则查询被自动追加租户过滤，表现为"数据查不到"。反例同样成立：已登记的 `person_roles` 在任何库都还没建表（属"超前登记"，见 `docs/ipd-系统说明/验收/P1-项5-DDL-apply核验-20260905.md` §3）。
- **引用配置用键名，别用行号**：本仓 yml 行号在分钟级就会漂——同一个 `application.yml` 的 `demo:` 段，20:56 实测在 :396、21:10 已到 :400（成因：多会话共工同一工作树，兄弟在途未提交编辑就会推号；`tenant.excludes` 则从早期文档的 :148 漂到 :198）。行号型断言的历史记录一律按"键名 + 当时的值"复核，而不是去对号。
- 本机真实数据源不是 `application-dev.yml` 写的 `127.0.0.1:3306/ruoyi-ai`（3306 无监听且该库不存在）：应用实走 gitignored 的 `.codex/ipd-dev/config/application-ipd-local.yml` → MySQL 8.0.46 @ `127.0.0.1:13306`，业务库 `ipd_dev`（125 表）。只读探针复用 `.codex/ipd-dev/config/mysql-client.cnf` 的 socket 即可（socket 与 13306 是同一实例，`SELECT @@port, @@socket` 已验），凭证不上命令行。仓库无 Flyway/Liquibase，`docs/script/sql/update/**` 全靠人工/DBA apply → "SQL 已 commit"绝不等于"约束已生效"，下结论前跑 `p1-ddl-apply-check.py`。
- 启动入口 `RuoYiAIApplication.main()` 会先自动杀 6039 端口（Windows 风格命令；macOS/Linux 无害 no-op）。

## 依赖与代码生成

- 三个 Langchain4j BOM（stable 1.17.2 / community 1.17.0-beta27 / beta 1.17.2-beta27，pom.xml:56-58）必须一起对齐升级；gRPC 钉死 1.62.2（pom.xml:68，Milvus SDK 冲突），升级前先重测 Milvus。
- 新增注解处理器必须同步登记 `maven-compiler-plugin` 的 `<annotationProcessorPaths>`，否则静默不生效。
- trace 新代码：RAG 载荷构建放 `org.ruoyi.argtrace`（ruoyi-chat 内，现仅 RagTraceNodeTypes / RagTracePayloadBuilder 两个文件）；通用 trace 域模型与写库在 `ruoyi-common-trace` 模块（`org.ruoyi.common.trace.*`）——不要散落到 `chat.*`。

## 自动化栈（Claude Code 运行时强制）

- `.claude/hooks/block-dangerous-git.sh` 阻断 `git push` / `git reset --hard` / `git clean -f` / `git branch -D` / `git checkout .` / `git restore .`——push 被拦是预期行为，需要用户明确授权，不要绕过。
- `.claude/helpers/sensitive-field-guard.cjs` 阻断写 `.env*` / `application-prod.yml` / PEM 私钥内容；警告 JWT secret、明文 password 字面量。
- `.claude/helpers/ipd-frontend-drift-guard.cjs` 阻断 IPD 前端工程（`/Users/mac/Documents/ruoyi-ipd-web/apps/web-antd/`）写入"漂移产物"——创建 `views/ipd/<domain>/<domain>-error.ts`、重写 `api/ipd/product-group.ts`、在 `api/ipd/*.ts` 新增与既有文件冲突的 export 都会 exit 2。完整规约见 `docs/ipd-系统说明/前端架构规约-20260906.md`。
- `scripts/check-ipd-frontend-drift.sh` 是上述 hook 的 CI/手动版：4 项检查（同名导出 / 错误码文件 / product-group 兼容层 / 手写 BackendPending）。CI 接入位置 `.github/workflows/ipd-frontend-drift.yml`（待补）。
- 改 `pom.xml` 会触发 `pom-edit-hint.cjs` 的非阻断同步提醒（BOM 对齐 / 注解处理器 / gRPC 版本等 5 类）。
- 4 个项目 skill（ai-module-add / gen-test / api-contract / db-migration）与 4 个审查 subagent（code-reviewer / security-reviewer / langchain4j-agent-reviewer / performance-analyzer）在 `.claude/` 下，分工见 `CLAUDE.md` §自动化栈。
- 修改 `docs/wiki/**` 后必须跑 `node docs/wiki/wiki-lint.cjs`（无 CI 门禁，靠自觉）。

## Learned User Preferences

- 全局梳理 / 治理类任务：要用专业智能体与工具做蜂群并行，走「盘点 → 实施 → 验证 → 文档/看板同步」闭环，不要只给建议。
- 执行中必须及时更新看板卡片状态（待办 / 进行中 / 阻塞 / 待审核 / 已完成）；证据不足时标 PARTIAL，不得提前标 done。
- 选定方案后用「继续 / A / 指定卡号」直接落地推进，少停在方案对比。

## Learned Workspace Facts

- 本工作区是 `ruoyi-ai`（IPD 产品经理管理系统）；勿与 `ZKER-staff`（`/Users/mac/Documents/ChatGPT/ZKER- staff`）或用户规则里的 IOE-DREAM 一卡通内容混淆。
- 看板操作走 `user-zker_vibe_kanban` MCP，并与 SSOT 镜像 `docs/ipd-系统说明/开发计划-看板镜像.md` 对齐；Mock/单测绿不等于业务闭环，真库或 HTTP 未过不得伪完成。
- 多会话并行时同一 Controller/测试签名会被兄弟会话改写；验收前以磁盘现态重编译，假红/假绿规则见上文「构建 / 测试」。
