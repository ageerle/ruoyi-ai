# Milvus向量库实现指南

## 概述

本项目已完成Milvus向量库的集成，基于Milvus SDK 2.6.4版本实现。Milvus是一个开源的向量数据库，专为AI应用和相似性搜索而设计。

## 实现特性

### ✅ 已实现功能

1. **集合管理**
   - 自动创建集合（Collection）
   - 检查集合是否存在
   - 删除集合

2. **数据存储**
   - 批量插入向量数据
   - 支持文本、fid、kid、docId等元数据
   - 自动生成向量嵌入

3. **向量搜索**
   - 基于相似性的向量搜索
   - 支持TopK结果返回
   - 返回相关文本内容

4. **数据删除**
   - 按文档ID删除
   - 按片段ID删除
   - 删除整个集合

## 架构设计

### 策略模式实现

```
AbstractVectorStoreStrategy (抽象基类)
    ↓
MilvusVectorStoreStrategy (Milvus实现)
WeaviateVectorStoreStrategy (Weaviate实现)
```

### 核心类说明

- **MilvusVectorStoreStrategy**: Milvus向量库策略实现
- **VectorStoreStrategyFactory**: 向量库策略工厂，支持动态切换
- **VectorStoreService**: 向量库服务接口

## 配置说明

### 必需配置项

在系统配置中需要设置以下Milvus相关配置：

```properties
# Milvus服务地址
milvus.url=http://localhost:19530

# 集合名称前缀
milvus.collectionname=LocalKnowledge

# 向量库类型选择
vector.store_type=milvus
```

### 集合Schema设计

每个集合包含以下字段：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | Int64 | 主键，自动生成 |
| text | VarChar(65535) | 文本内容 |
| fid | VarChar(255) | 片段ID |
| kid | VarChar(255) | 知识库ID |
| docId | VarChar(255) | 文档ID |
| vector | FloatVector(1024) | 向量数据 |

### 索引配置

- **索引类型**: IVF_FLAT
- **距离度量**: L2 (欧几里得距离)
- **参数**: nlist=1024

## 使用示例

### 1. 创建集合

```java
MilvusVectorStoreStrategy strategy = new MilvusVectorStoreStrategy(configService);
strategy.createSchema("bge-large-zh-v1.5", "test001", "test-model");
```

### 2. 存储向量数据

```java
StoreEmbeddingBo storeEmbeddingBo = new StoreEmbeddingBo();
storeEmbeddingBo.setVectorModelName("bge-large-zh-v1.5");
storeEmbeddingBo.setKid("test001");
storeEmbeddingBo.setDocId("doc001");
storeEmbeddingBo.setChunkList(Arrays.asList("文本1", "文本2"));
storeEmbeddingBo.setFids(Arrays.asList("fid001", "fid002"));

strategy.storeEmbeddings(storeEmbeddingBo);
```

### 3. 查询向量数据

```java
QueryVectorBo queryVectorBo = new QueryVectorBo();
queryVectorBo.setQuery("查询文本");
queryVectorBo.setKid("test001");
queryVectorBo.setMaxResults(5);

List<String> results = strategy.getQueryVector(queryVectorBo);
```

### 4. 删除数据

```java
// 按文档ID删除
strategy.removeByDocId("doc001", "test001");

// 按片段ID删除
strategy.removeByFid("fid001", "test001");

// 删除整个集合
strategy.removeById("test001", "model");
```

## 部署要求

### Milvus服务部署

1. **Docker部署** (推荐)
```bash
# 下载docker-compose文件
wget https://github.com/milvus-io/milvus/releases/download/v2.6.4/milvus-standalone-docker-compose.yml -O docker-compose.yml

# 启动Milvus
docker-compose up -d
```

2. **验证部署**
```bash
# 检查服务状态
docker-compose ps

# 查看日志
docker-compose logs milvus-standalone
```

### 系统要求

- **内存**: 最少8GB，推荐16GB+
- **存储**: SSD推荐，至少50GB可用空间
- **CPU**: 4核心以上
- **网络**: 确保19530端口可访问

## 性能优化

### 1. 索引优化

根据数据量调整索引参数：
- 小数据集(<100万): nlist=1024
- 中等数据集(100万-1000万): nlist=4096
- 大数据集(>1000万): nlist=16384

### 2. 批量操作

- 批量插入：建议每批1000-10000条记录
- 批量查询：避免频繁的单条查询

### 3. 内存管理

```yaml
# docker-compose.yml中的内存配置
environment:
  MILVUS_CONFIG_PATH: /milvus/configs/milvus.yaml
volumes:
  - ./milvus.yaml:/milvus/configs/milvus.yaml
```

## 故障排除

### 常见问题

1. **连接失败**
   - 检查Milvus服务是否启动
   - 验证网络连接和端口
   - 确认配置中的URL正确

2. **集合创建失败**
   - 检查集合名称是否符合规范
   - 验证字段定义是否正确
   - 查看Milvus日志获取详细错误

3. **插入数据失败**
   - 确认向量维度与schema一致
   - 检查数据格式是否正确
   - 验证集合是否已加载

4. **查询无结果**
   - 确认集合中有数据
   - 检查查询参数设置
   - 验证向量化模型一致性

### 日志调试

启用详细日志：
```properties
logging.level.org.ruoyi.service.strategy.impl.MilvusVectorStoreStrategy=DEBUG
logging.level.io.milvus=DEBUG
```

## 与Weaviate对比

| 特性 | Milvus | Weaviate |
|------|--------|----------|
| 性能 | 高性能，专为大规模设计 | 中等性能 |
| 部署 | 需要独立部署 | 可独立部署或云服务 |
| 生态 | 专注向量搜索 | 集成更多AI功能 |
| 学习成本 | 中等 | 较低 |
| 扩展性 | 优秀 | 良好 |

## 后续优化建议

1. **连接池管理**: 实现MilvusClient连接池
2. **异步操作**: 支持异步插入和查询
3. **分片策略**: 大数据集的分片管理
4. **监控告警**: 集成性能监控
5. **备份恢复**: 数据备份和恢复机制

## 参考资料

- [Milvus官方文档](https://milvus.io/docs)
- [Milvus Java SDK](https://github.com/milvus-io/milvus-sdk-java)
- [向量数据库最佳实践](https://milvus.io/docs/performance_faq.md)