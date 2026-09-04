# AGENTS.md — ruoyi-ai

只收录「读代码推不出来、但直接决定任务成败」的雷区。架构 / 目录 / 完整命令见同目录 `CLAUDE.md`；二开方向总览见 `README-IPD-OVERRIDE.md`（优先级高于根 `README.md`）。

一句话定位：RuoYi-AI（Spring Boot 3.5.8 + Java 17 + Langchain4j 的 Maven 多模块**后端**仓库，父 POM revision 3.1.0）正在二开改造为「IPD 产品经理管理系统」。前端在独立仓库（ruoyi-web / ruoyi-admin 前端），别在这里找 Vue 代码。

## 范围与路由

- `docs/开发说明/**` 是产品设计事实源（"圣经"，G-04）：产品业务决策不可改；owner 已授权**勘误级更新**（错字 / 失效引用 / 数字对齐），勘误须在 `docs/ipd-系统说明/log.md` 登记；工程修复与补充文档一律写到 `docs/ipd-系统说明/` 下。
- Issue 流程：GitHub Issues（origin `wilson323/ruoyi-ai`，upstream `ageerle/ruoyi-ai`）→ `docs/agents/issue-tracker-github.md`；领域文档地图 → `docs/agents/domain.md`。
- 多智能体协同（Ruflo/claude-flow）的启用门槛、拓扑与命令见 `CLAUDE.md` §「Ruflo 多智能体协同底座」；单文件小改动不要上 swarm。

## 构建 / 测试（每条都吃过亏）

- 只在仓库根执行 `mvn`（父 POM 管 `<modules>`）；不要进子模块目录单独构建。
- **假绿陷阱**：Surefire 按 `<groups>${profiles.active}</groups>` 过滤（pom.xml:472），默认 dev profile 下没有 `@Tag("dev")` 的测试类被**静默跳过**——新测试不加 tag，"测试全绿"毫无意义。
- `demo.enabled=true`（application.yml:293）拦截写操作，返回"演示模式，不允许操作"（白名单见 demo.excludes，application.yml:297）；真实开发先关掉（application-dev.yml 覆盖或 `-Ddemo.enabled=false`）。
- 多租户默认开启：新建"租户共享"表必须登记 application.yml 的 `tenant.excludes`（application.yml:148），否则查询被自动追加租户过滤，表现为"数据查不到"。
- 启动入口 `RuoYiAIApplication.main()` 会先自动杀 6039 端口（Windows 风格命令；macOS/Linux 无害 no-op）。

## 依赖与代码生成

- 三个 Langchain4j BOM（stable 1.17.2 / community 1.17.0-beta27 / beta 1.17.2-beta27，pom.xml:56-58）必须一起对齐升级；gRPC 钉死 1.62.2（pom.xml:68，Milvus SDK 冲突），升级前先重测 Milvus。
- 新增注解处理器必须同步登记 `maven-compiler-plugin` 的 `<annotationProcessorPaths>`，否则静默不生效。
- trace 新代码：RAG 载荷构建放 `org.ruoyi.argtrace`（ruoyi-chat 内，现仅 RagTraceNodeTypes / RagTracePayloadBuilder 两个文件）；通用 trace 域模型与写库在 `ruoyi-common-trace` 模块（`org.ruoyi.common.trace.*`）——不要散落到 `chat.*`。

## 自动化栈（Claude Code 运行时强制）

- `.claude/hooks/block-dangerous-git.sh` 阻断 `git push` / `git reset --hard` / `git clean -f` / `git branch -D` / `git checkout .` / `git restore .`——push 被拦是预期行为，需要用户明确授权，不要绕过。
- `.claude/helpers/sensitive-field-guard.cjs` 阻断写 `.env*` / `application-prod.yml` / PEM 私钥内容；警告 JWT secret、明文 password 字面量。
- 改 `pom.xml` 会触发 `pom-edit-hint.cjs` 的非阻断同步提醒（BOM 对齐 / 注解处理器 / gRPC 版本等 5 类）。
- 4 个项目 skill（ai-module-add / gen-test / api-contract / db-migration）与 4 个审查 subagent（code-reviewer / security-reviewer / langchain4j-agent-reviewer / performance-analyzer）在 `.claude/` 下，分工见 `CLAUDE.md` §自动化栈。
- 修改 `docs/wiki/**` 后必须跑 `node docs/wiki/wiki-lint.cjs`（无 CI 门禁，靠自觉）。
