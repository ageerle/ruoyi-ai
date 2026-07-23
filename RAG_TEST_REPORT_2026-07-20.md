# RAG 完整修复与全量验收报告

验收时间：2026-07-21（Asia/Shanghai）  
验收对象：当前未提交工作区（保留原有改动）

## 结论

计划内的 1–15 项工程缺陷已完成代码修复，默认 Maven 构建已从“跳过测试”改为真实执行测试。全仓 37 个 reactor 模块测试成功，`ruoyi-chat` 49/49 通过，两个前端生产构建通过，`git diff --check` 通过。

本机已运行 MySQL、Redis、MinIO 和 Weaviate 1.30.0；Milvus/Qdrant 容器以及有效的 embedding/chat/rerank provider 凭证不存在，因此这三项真实 provider/存储引擎冒烟被标记为环境限制，不影响确定性代码验收。

## 1–15 项验收

| # | 状态 | 修复/证据 |
|---|---|---|
| 1 | 通过 | Markdown/Java/字符分片的边界、空文档、超长块回归通过。 |
| 2 | 通过 | Supervisor 每轮仅保留一个 RAG 入口，不再用已含 RAG 的 prompt 重复检索。 |
| 3 | 通过 | 历史消息进入 Supervisor prompt，检索 query 与最终 prompt 分离。 |
| 4 | 通过 | `fid` 稳定 ID 贯穿 DB/三种向量库/RRF，融合去重回归通过。 |
| 5 | 通过 | aiflow vector/hybrid 复用统一检索服务；graph 明确返回不支持，不再伪装为 vector。 |
| 6 | 通过 | 重解析改为先写新 fid、再清旧向量、最后替换 DB；失败补偿新向量；删片段/附件/库遇向量删除失败即中止。 |
| 7 | 通过 | embedding/rerank provider 使用 prototype 实例，工厂缓存可按模型刷新，避免跨配置污染。 |
| 8 | 通过 | `similarityThreshold` 仅用于粗召回；`rerankScoreThreshold` 仅在 rerank 真实成功后生效，回归测试通过。 |
| 9 | 通过 | 默认配置和 Compose 统一为 Weaviate 1.30.0、`28080:8080`。 |
| 10 | 通过 | 三种策略均使用 `embedAll`；Weaviate batch objects、Milvus `addAll`、Qdrant `addAll`。 |
| 11 | 通过 | upload/parse/retrieval 权限保留，parse/retrieval 增加分布式防重复提交，upload 由现有知识库+文件名唯一约束兜底。 |
| 12 | 通过 | 分隔符使用字面量语义，`|`/`.`/`*` 回归通过。 |
| 13 | 通过 | hybrid 通道失败可降级到 vector；所有可用通道都失败时抛出明确业务异常。 |
| 14 | 通过 | Weaviate client 稳定懒加载单例；schema 仅在已存在或创建成功后进入缓存。 |
| 15 | 通过 | 工厂新增严格 `getStrategy(type)`，知识库 `vectorModel` 优先，空值才回退全局，非法值直接报错。 |

## 其他完成项

- 多知识库并行检索，按 `kid + docId + fid` 去重，统一上限和字符预算。
- 5 分钟短 TTL 检索缓存，key 覆盖检索参数，知识数据变更主动失效。
- rerank 仅保留 provider 实际返回的文档。
- 知识库文档数改为 group-by 查询，消除该 N+1。
- Milvus/Qdrant/Weaviate 的删 collection/doc/fid 语义对齐；Milvus 删库改为 drop collection。
- MCP `npx` 根据操作系统解析，支持系统属性/环境变量覆盖。
- `fid` 非空唯一、`doc_id varchar(32)`、租户/用户索引与可重复执行迁移脚本已提供。
- 用户端聊天页已接入知识库列表和最小选择器。

## 测试记录

| 检查 | 结果 |
|---|---|
| `mvn -Pdev test` | 37/37 reactor 模块 SUCCESS；`ruoyi-chat` 49/49 |
| `mvn -Pdev -pl ruoyi-modules/ruoyi-aiflow -am -DskipTests compile` | 21/21 SUCCESS |
| `ruoyi-web: pnpm build` | SUCCESS，2621 modules transformed |
| `ruoyi-admin: pnpm build` | SUCCESS，10/10 build tasks |
| `git diff --check` | SUCCESS，无空白错误 |
| Weaviate/MySQL/Redis/MinIO | Docker 服务运行，Weaviate 1.30.0 映射 28080 |
| 三向量库 Docker 集成 | SUCCESS；Weaviate 1.30.0、Milvus 2.5.7、Qdrant 1.17.0 真实写入/检索/删除测试 3/3 通过 |
| 真实 embedding/chat/rerank | 环境限制：当前配置为无效/占位凭证 |

本轮未创建新的 `codex_rag_verify_` 持久化数据；上一轮验收数据已清理，未动现有非测试数据。

## 2026-07-21 三向量库 Docker 补充验收

- 启动并保留 `ruoyi-rag-milvus`、`ruoyi-rag-milvus-etcd`、`ruoyi-rag-milvus-minio`、`ruoyi-rag-qdrant`，四个容器健康检查均为 `healthy`。
- Milvus 专用 MinIO 仅在 Docker 内网可达，没有占用宿主机 9000/9001；Milvus 映射 19530/9091，Qdrant 映射 6333/6334。
- `ThreeVectorStoresDockerIT` 使用 32 维确定性 embedding，对三库逐一验证 batch write、vector search、fid delete、docId delete 和 drop collection，3/3 通过。
- 首轮测试发现 Milvus `autoFlush=false` 导致批量写入后不可立即检索、元数据删除不可立即见；改为写入和删除返回前 flush 后通过。
- 清理后 Weaviate/Qdrant 的 `CodexRagVerify*` collection 计数均为 0，Milvus collection 也由测试 finally 成功 drop；本轮未写入 MySQL 或 OSS 测试数据。
