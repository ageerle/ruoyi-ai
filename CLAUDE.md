# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

**RuoYi-AI** — enterprise-grade AI assistant platform. Spring Boot 3.5.8 + Java 17 + Langchain4j 1.17.2 + Langgraph4j. Parent Maven project (revision `3.1.0`) that builds into a single deployable backend service (port `6039`). Multi-tenant, multi-model (DeepSeek / Zhipu / OpenAI / etc.), supports RAG, MCP tools, visual workflow orchestration, and Supervisor-mode multi-agent orchestration. Frontend is split into separate repos (`ruoyi-web`, `ruoyi-admin`); this repo is backend-only.

## Build & Run

Build from repo root — this is a parent POM with `<modules>`, never build a submodule in isolation:

```bash
# Full build with tests (tests run with active profile tag, see Testing below)
mvn clean package

# Build skipping tests
mvn clean package -DskipTests

# Run locally (profile defaults to dev via SPRING_PROFILES_ACTIVE env override)
mvn spring-boot:run -pl ruoyi-admin

# Run with a specific profile
mvn spring-boot:run -pl ruoyi-admin -Dspring-boot.run.profiles=prod
# or via Maven profile (also sets profiles.active property)
mvn spring-boot:run -pl ruoyi-admin -Pprod
```

Maven profiles map Spring profiles one-to-one: `-Plocal` → `local`, `-Pdev` → `dev`, `-Pprod` → `prod`. `dev` is `activeByDefault`.

## Testing

**Non-obvious gotcha — read first.** `maven-surefire-plugin` is configured (`pom.xml:464-476`) with `<groups>${profiles.active}</groups>` and `<excludedGroups>exclude</excludedGroups>`. This means:

- Under the default `dev` profile, **only tests annotated `@Tag("dev")` run**. A test without any `@Tag` is silently skipped.
- Tests tagged `@Tag("exclude")` are always skipped.
- To run a single test: `mvn test -Dtest=ClassName#methodName -pl ruoyi-modules/ruoyi-chat`

When adding a new test class, add `@Tag("dev")` (or whichever profile you target) or it won't execute under the standard `mvn test`.

## Module Layout

```
ruoyi-admin/                # Spring Boot main, port 6039, controllers, OpenAPI grouping
ruoyi-common/               # 27 shared library modules (BOM-managed)
  ruoyi-common-core         # utilities, base entities, exceptions
  ruoyi-common-security     # Sa-Token + JWT integration
  ruoyi-common-chat         # Langchain4j adapters, AI helpers
  ruoyi-common-mybatis      # MyBatis-Plus config, paging, tenant filter
  ruoyi-common-redis        # Redisson, distributed locks (lock4j)
  ruoyi-common-web          # web mvc config, XSS filter, rate limiter
  ruoyi-common-websocket    # WS server
  ruoyi-common-sse          # SSE server (default streaming channel)
  ruoyi-common-oss          # AWS S3 / MinIO
  ruoyi-common-encrypt      # mybatis-encryptor, api-decrypt
  ruoyi-common-tenant       # multi-tenant context
  ruoyi-common-trace        # distributed tracing writer (writes to trace_run/trace_node)
  ... (~15 more, see ruoyi-common/pom.xml)
ruoyi-modules/              # Feature modules, each ships its own REST API
  ruoyi-system              # RBAC: user/role/menu/dept/post/dict/config
  ruoyi-chat                # AI chat core: Langchain4j agents, knowledge base, RAG, tools
  ruoyi-workflow            # Warm-Flow BPMN workflow
  ruoyi-aiflow              # Visual AI workflow orchestration (drag-and-drop nodes)
  ruoyi-generator           # Velocity-based code generator
ruoyi-extend/               # Auxiliary Spring Boot apps (run separately, not part of main jar)
  ruoyi-monitor-admin       # Spring Boot Admin server
  ruoyi-snailjob-server     # SnailJob distributed job server
```

## AI Stack

- **Langchain4j**: three BOMs pinned in `pom.xml:55-58` (stable `1.17.2`, community `1.17.0-beta27`, beta `1.17.2-beta27`). Keep them aligned when upgrading.
- **Langgraph4j** `1.8.20` — graph-style agent orchestration.
- **Vector DB**: pluggable, selected by `vector-store.type` in `application.yml`. Default `weaviate` (port `28080` in compose); supports `milvus` (`:19530`) and `qdrant` (`:6334`). **The compose-deployed vector DB must match this setting** or RAG queries silently return empty.
- **Models**: configured at runtime via "Model Management" UI; backend integration covers DeepSeek, Zhipu (with official `zai-sdk`), MIMO, Bailian, OpenAI.
- **RAG**: local knowledge stored as `LocalKnowledge` class/collection; doc parsing handles PDF/Word/Excel/images.
- **MCP tools**: protocol integration; builtin tools exposed to LangChain4j agents via `ToolProvider` beans.
- **Workflow orchestration**: `ruoyi-aiflow` provides a visual designer (frontend) backed by SSE-streamed execution on the backend; nodes include model calls, email send, manual review.

## Key Conventions & Gotchas

- **Port 6039 auto-kill**: `RuoYiAIApplication.main()` (`ruoyi-admin/src/main/java/org/ruoyi/RuoYiAIApplication.java:31-67`) calls `killPortProcess(6039)` before startup. It uses `netstat`/`taskkill` (Windows-style commands) — works on Windows, no-ops cleanly on macOS/Linux. Override with `-Dserver.port=xxxx` if needed.
- **Demo mode** (`application.yml:24-32`, `290-308`): `demo.enabled=true` blocks every write operation via an interceptor, returning "演示模式，不允许操作" for unauthorized POST/PATCH/DELETE. Whitelist paths live under `demo.excludes` (login, chat-send, system-session, etc.). **Disable it for any real development work** — set `demo.enabled=false` in your `application-dev.yml` override or use `-Ddemo.enabled=false`.
- **Multi-tenant** is on by default (`tenant.enable=true`). Tenant filter is applied via MyBatis-Plus; tenant-shared tables are listed in `tenant.excludes` in `application.yml:148-161`. New shared tables must be added there or they'll be incorrectly filtered. Note `trace_run` and `trace_node` are excluded because the trace writer runs on async threads without tenant context.
- **Sa-Token** default JWT secret (`application.yml:127`) is `abcdefghijklmnopqrstuvwxyz` — dev-only. Override via env in any non-local environment.
- **Coding harness** (`application.yml:24-32`): an agent runtime with budget limits (`max-iterations: 200`, `max-tool-calls: 600`). The `coding.harness.tools.execute-process.enabled=true` flag enables command execution — be aware this can shell out from inside chat.
- **Annotation processors** (`pom.xml:432-457`): five wired via `maven-compiler-plugin` — `therapi-runtime-javadoc-scribe`, `lombok`, `spring-boot-configuration-processor`, `mapstruct-plus-processor`, `lombok-mapstruct-binding`. Adding a new processor means updating `<annotationProcessorPaths>` or it won't run.
- **Java 17** is required. Virtual threads are gated off (`spring.threads.virtual.enabled: false`); toggle on if running JDK 21+.
- **gRPC version pinning**: `pom.xml:67-70` pins `grpc-bom` to `1.62.2` to resolve Milvus SDK conflicts. Don't upgrade gRPC without re-testing Milvus integration.
- **Lombok + MapStruct-Plus** generate boilerplate; respect `@Data`, `@Builder`, `@RequiredArgsConstructor`, and the `IConvert` source pattern (interface + `Impl` suffix class — generator emits both).
- **Trace package layout**: trace-related code lives under `argtrace.*` (recently moved out of `chat.*` per commit history); new trace code goes there, not in `chat/`.

## Endpoints (dev)

- Backend API: http://localhost:6039
- Springdoc Swagger UI: `/swagger-ui.html` (6 OpenAPI groups defined at `application.yml:227-239`)
- Actuator: `/actuator` — all endpoints exposed; health details `ALWAYS`
- SSE stream (chat): `/resource/sse`
- WebSocket: `/resource/websocket` (off by default — set `websocket.enabled=true`)

## Environment Setup

Quick dev stack (Docker Compose, includes MySQL/Redis/Weaviate/MinIO):

```bash
git clone --depth 1 --branch v3.1.0 https://github.com/ageerle/ruoyi-ai.git
cd ruoyi-ai
cp docs/docker/ruoyi-ai/.env.example docs/docker/ruoyi-ai/.env
docker compose --env-file docs/docker/ruoyi-ai/.env \
  -f docs/docker/ruoyi-ai/docker-compose-all.yaml up -d
```

Compose ports: MySQL `23306`, Redis `26379`, Weaviate `28080`, MinIO `29000`/`29090`, backend `26039`. Override MySQL/MinIO passwords before any non-local deploy.

## Related Repositories (not in this repo)

- `ruoyi-web` — user-facing frontend (Vue 3 + Vben Admin)
- `ruoyi-admin` — admin panel frontend
- `ruoyi-drama` — short-drama composition service (uses `short-drama.composition.*` config in this repo)
- `ruoyi-copilot`, `ruoyi-uniapp` — companion apps

## 自动化栈使用手册

本项目除 Claude Code 内置能力外，挂了 4 层自动化：Skill（用户主动调用）、Subagent（Claude 自动并发调度）、Hook（机器执行拦截 / 提醒）、MCP Server（外部能力）。另由 Ruflo 提供多智能体协同底座。

### Skills（用户主动调用，4 个项目自定义 + 30 个 ruflo 内置）

**项目自定义**（都在 `.claude/skills/<name>/SKILL.md`）：

| Skill | 调用 | 适用场景 |
|---|---|---|
| `/ai-module-add` | user-only | 在 `ruoyi-chat` / `ruoyi-aiflow` 加新 Langchain4j agent、tool、workflow 节点或 MCP 工具。封装了项目约定（包结构、注解模板、MCP 暴露、租户 / 权限约束） |
| `/gen-test` | user-only | 按 `@Tag("dev")` Surefire 过滤规范生成 Service / Controller 单测。封装了 Mockito + AssertJ 模板 + 必须覆盖的 6 个维度 |
| `/api-contract` | user-only | 改了 controller / DTO 后生成 OpenAPI 增量 diff + BREAKING / NEW / CHANGE 分类 + 给 `ruoyi-web` / `ruoyi-admin` 的变更通知草稿 |
| `/db-migration` | user-only | 新增业务表 / 加字段 / 加索引 / 新增 snailjob 任务 / 登记租户共享表。封装 DDL 模板、Entity 必备字段、回滚脚本生成 |

**ruflo 内置 30 个**：默认不主动调，需要时按名字调用。`swarm-orchestration` / `v3-swarm-coordination` / `sparc-methodology` 是重型武器，小改动别上。

### Subagents（Claude 自动调度，4 个并发审查）

按改动范围触发，**不重叠**：

| Agent | 触发时机 | 审查范围 | 交给谁 |
|---|---|---|---|
| `code-reviewer` | 改任何 Java 文件 | 架构、可读性、并发、错误处理、Spring 用法、Lombok / MapStruct-Plus 配合 | AI 安全 / 业务安全 / 性能 |
| `langchain4j-agent-reviewer` | 改 `ruoyi-chat` / `ruoyi-aiflow` | prompt 注入、`@Tool` 暴露、token 成本、MCP 配置、RAG 检索 | 性能 / 业务安全 / 架构 |
| `security-reviewer` | 改 controller / service / config / yml | 多租户过滤、Sa-Token + JWT、API 加解密、XSS、SQL 注入、密钥硬编码 | AI / 性能 / 架构 |
| `performance-analyzer` | 改 mapper / service / AI 模块 | SQL 慢查询与 N+1、Redis、连接池、JVM、Langchain4j token、向量化批处理 | 架构 / 业务安全 / AI 安全 |

**调度规则**：

- 改 1-2 行代码 → 不派 agent（成本不划算）
- 改一个 controller 写操作 → `security-reviewer` + `code-reviewer`（2 个）
- 改 Langchain4j 模块 → `langchain4j-agent-reviewer`（1 个就够）
- 模块级重构（>5 文件）→ 2-3 个 agent 并发，烧 token 但值

### Hooks（机器执行，最硬约束）

写在 `.claude/helpers/*.cjs`，被 `.claude/settings.json` 引用。所有 hook 都通过 `node --check` + 端到端 12 项回归测试。

| Hook | 类型 | 触发 | 行为 |
|---|---|---|---|
| `sensitive-field-guard.cjs` | PreToolUse | Write / Edit / MultiEdit | **阻断** `.env*` / `application-prod.yml` / 含 PEM 私钥内容；**警告** JWT secret / 明文 password 字面量 |
| `pom-edit-hint.cjs` | PostToolUse | Write / Edit / MultiEdit 命中 `**/pom.xml` | **不阻断**，stderr 提示 5 类同步项（langchain4j 多 BOM 对齐、annotation processor、grpc 版本、flatten 插件、surefire groups） |

调试命令：

```bash
# 手动测试 hook（模拟 stdin payload）
echo '{"tool_name":"Write","tool_input":{"file_path":"/tmp/.env","content":"x"}}' \
  | node .claude/helpers/sensitive-field-guard.cjs
echo $?  # 期望: 2（阻断）
```

### MCP Servers（项目级 scope，仅本项目生效）

| Server | 命令 | 用途 | 状态 |
|---|---|---|---|
| `context7` | `npx -y @upstash/context7-mcp` | 实时查 Langchain4j / Spring Boot 文档 | ✅ 可用 |
| `github` | `npx -y @modelcontextprotocol/server-github` | 操作 issues / PRs / actions | ⚠️ 需 `GITHUB_PERSONAL_ACCESS_TOKEN` 环境变量才能调用；补 token：`claude mcp add github -e GITHUB_PERSONAL_ACCESS_TOKEN=<PAT> -- npx -y @modelcontextprotocol/server-github` |

注册位置：`~/.claude.json → projects[/Users/mac/Documents/ruoyi-ai].mcpServers`，scope = `local`，不会污染其他项目。

### 漂移自检（每次配置变更后跑）

```bash
# 1. 文件存在 + 语法
for f in .claude/skills/{ai-module-add,api-contract,db-migration,gen-test}/SKILL.md \
         .claude/agents/*.md; do [ -f "$f" ] && echo "✅ $f"; done
node --check .claude/helpers/*.cjs

# 2. settings.json 合法性 + hook 引用
node -e "JSON.parse(require('fs').readFileSync('.claude/settings.json','utf8'))"

# 3. MCP 注册
node -e 'const j=require("/Users/mac/.claude.json").projects["/Users/mac/Documents/ruoyi-ai"].mcpServers||{}; console.log(Object.keys(j))'

# 4. 4 个 agent 边界节齐全
for a in code-reviewer langchain4j-agent-reviewer performance-analyzer security-reviewer; do
  grep -q "^## 边界" .claude/agents/$a.md && echo "✅ $a" || echo "❌ $a 缺边界节"
done
```

### Ruflo 多智能体协同底座

本项目使用 [Ruflo](https://github.com/ruvnet/ruflo) V3 做多智能体协调（通过 `/ruflo-core:init-project` 初始化）。协调模式、swarm 拓扑、agent prompt 模板见 `.claude-flow/CAPABILITIES.md`、`.claude-flow/config.yaml`、全局 `~/.claude/CLAUDE.md`。

何时启用 Ruflo：

- ✅ 跨 3+ 模块的功能开发、API 大改、安全审计、性能基准
- ❌ 单文件 / 单行改动、配置修改、问答

启用步骤：

```bash
npx claude-flow swarm init --topology hierarchical-mesh --max-agents 8
npx claude-flow hooks route --task "<任务描述>"
```