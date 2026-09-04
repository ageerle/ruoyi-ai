---
topic: modules/chat-multimodal
title: ruoyi-chat — 多模态（视频 / 音频 / 图像 / Embedding）
updated: 2026-09-04
raw:
  - raw/multimodal-source/video-AbstractVideoGenerationService.md
  - raw/multimodal-source/audio-AbstractAudioGenerationService.md
  - raw/multimodal-source/image-AbstractImageGenerationService.md
  - raw/multimodal-source/embed-MultiModalEmbedModelService.md
  - raw/chat-source/agents-catalog.md
---

# ruoyi-chat 多模态（视频 / 音频 / 图像 / Embedding）

本篇覆盖 `ruoyi-chat` 的多模态生成与嵌入能力：视频生成、音频生成、图像生成、多模态 embedding。是 [modules/chat.md](../modules/chat.md) 的多模态扩展。

## 18 个多模态文件全景

```
org.ruoyi.service/
├── video/             # 视频生成
│   ├── AbstractVideoGenerationService.java          # 抽象基类
│   ├── provider/AtlasVideoGenerationServiceImpl.java # Atlas Cloud 实现
│   └── provider/OpenAiVideoGenerationServiceImpl.java # OpenAI Sora 实现
├── audio/             # 音频生成
│   ├── AbstractAudioGenerationService.java
│   ├── provider/OpenAiAudioGenerationServiceImpl.java # OpenAI TTS
│   └── provider/AtlasAudioGenerationServiceImpl.java
├── image/             # 图像生成
│   ├── AbstractImageGenerationService.java
│   ├── provider/TongYiWanxImageServiceImpl.java      # 阿里通义万相
│   ├── provider/OpenAiImageGenerationServiceImpl.java # DALL-E
│   └── provider/AtlasImageGenerationServiceImpl.java
├── embed/             # 嵌入模型（多模态）
│   ├── BaseEmbedModelService.java                    # 文本嵌入基类
│   ├── MultiModalEmbedModelService.java               # 多模态嵌入接口
│   └── impl/
│       ├── SiliconFlowEmbeddingProvider.java
│       ├── ZhipuAiEmbeddingProvider.java
│       ├── OpenAiEmbeddingProvider.java
│       ├── MinimaxEmbeddingProvider.java
│       ├── AliBaiLianBaseEmbedProvider.java
│       └── AliBaiLianMultiEmbeddingProvider.java
└── media/             # 多模态支持
    ├── AtlasMediaSupport.java
    ├── OpenAiMediaSupport.java
    └── AtlasPredictionService.java
```

## 视频生成（3 文件）

参见 [video-AbstractVideoGenerationService.md](../raw/multimodal-source/video-AbstractVideoGenerationService.md)。

```java
public abstract class AbstractVideoGenerationService implements IVideoGenerationService {
    @Override
    public MediaGenerationResponse generateVideo(VideoContext videoContext) {
        return doGenerateVideo(videoContext);
    }
    @Override
    public MediaGenerationResponse retrieveVideo(VideoContext videoContext) {
        return doRetrieveVideo(videoContext);
    }
    protected abstract MediaGenerationResponse doGenerateVideo(VideoContext ctx);
    protected abstract MediaGenerationResponse doRetrieveVideo(VideoContext ctx);
}
```

**两阶段 API**：

1. **generateVideo**：提交生成请求，返回 `MediaGenerationResponse`（含 taskId + status）
2. **retrieveVideo**：轮询查询任务状态（`PENDING / RUNNING / SUCCESS / FAILED`），成功时返回视频 URL

**实现**：
- **AtlasVideoGenerationServiceImpl** —— Atlas Cloud 视频模型（参见 [claude-md.md § Sponsors](../raw/project-skeleton/claude-md.md)）
- **OpenAiVideoGenerationServiceImpl** —— OpenAI Sora

**配置**：Atlas 与 OpenAI 的 API key 通过「模型管理」后台配置，**不写在 application.yml**。

## 音频生成（3 文件）

参见 [audio-AbstractAudioGenerationService.md](../raw/multimodal-source/audio-AbstractAudioGenerationService.md)。

接口与视频类似：

```java
public abstract class AbstractAudioGenerationService {
    public abstract MediaGenerationResponse doGenerateAudio(AudioContext ctx);
    public abstract MediaGenerationResponse doRetrieveAudio(AudioContext ctx);
}
```

**典型用例**：AI 角色配音、短剧场景对白、AI 客服语音回复。

**实现**：
- **OpenAiAudioGenerationServiceImpl** —— OpenAI TTS / Whisper
- **AtlasAudioGenerationServiceImpl** —— Atlas 音频

## 图像生成（4 文件）

参见 [image-AbstractImageGenerationService.md](../raw/multimodal-source/image-AbstractImageGenerationService.md)。

接口同上。**实现最丰富**（4 个 provider）：

| Provider | 模型 | 特点 |
|---|---|---|
| TongYiWanxImageServiceImpl | 阿里通义万相 | 中文 prompt 友好 |
| OpenAiImageGenerationServiceImpl | DALL-E 3 | 国际通用 |
| AtlasImageGenerationServiceImpl | Atlas 自研 | 多模态 |

**典型用例**：AI 配图、短剧分镜图、用户头像、聊天表情包。

## 多模态 Embedding（8 文件）

参见 [embed-MultiModalEmbedModelService.md](../raw/multimodal-source/embed-MultiModalEmbedModelService.md)。

```java
public interface MultiModalEmbedModelService extends BaseEmbedModelService {
    Response<Embedding> embedImage(String imageDataUrl);    // 图像 → 向量
    Response<Embedding> embedVideo(String videoDataUrl);    // 视频 → 向量
    Response<Embedding> embedMultiModal(MultiModalInput input);  // 多模态 → 向量
}
```

**核心能力**：把图像 / 视频转成向量后存到向量库（Weaviate / Milvus / Qdrant），实现「以文搜图」「以图搜图」「视频片段检索」。

**6 个 Embedding Provider**：

| Provider | 维度 | 适用 |
|---|---|---|
| OpenAi | 1536 / 3072 | 通用文本 + 图像 |
| Zhipu | 1024 / 2048 | 中文友好 |
| SiliconFlow | 1024 / 1536 | 多模型聚合 |
| Minimax | 1536 | 多语言 |
| 阿里百炼 Base | 1536 | 通用 |
| 阿里百炼 Multi | 1024 | 多模态 |

**Factory 模式**（参见 [modules/chat.md § Factory 模式](../modules/chat.md)）：

```java
EmbeddingModelFactory.getEmbeddingModel(provider) → EmbeddingModel
```

按 `provider` 名路由到对应实现，配置改了就换实现，不需要改业务代码。

## 与 RAG 的关系

**多模态 Embedding + RAG = 多模态 RAG**：

```
用户上传图片 → embedImage() → 向量存入向量库
用户查询「类似图片」 → embedQuery() → 相似度检索 → 返回 topK
```

这是 [modules/chat.md § RAG 与向量库桥接](../modules/chat.md) 的扩展能力。

## 工厂与路由

视频 / 音频 / 图像生成都通过 factory 模式路由（参见 [chat-service-factory.md](../raw/chat-source/chat-service-factory.md)）：

```java
VideoServiceFactory.getService(provider) → IVideoGenerationService
AudioServiceFactory.getService(provider) → IAudioGenerationService
ImageServiceFactory.getService(provider) → IImageGenerationService
```

**前端调用**：

```json
POST /media/generate
{
  "type": "video",
  "provider": "atlas",
  "context": { "prompt": "...", "duration": 10 }
}
```

## 异步任务模式

视频 / 音频生成通常是**长时间任务**（30 秒 - 5 分钟），不能阻塞 HTTP 请求。流程：

1. `generateXxx()` 返回 `MediaGenerationResponse{taskId: "xxx", status: "PENDING"}`
2. 后端把 taskId 推送到 SSE 流（前端订阅 `/resource/sse`）
3. `retrieveXxx()` 轮询（每 5 秒查一次），完成时通过 SSE 通知前端
4. 前端拿到视频 URL，渲染播放

参见：[modules/chat.md § SSE 流式响应](../modules/chat.md)。

## 多租户 + 配额

- 每个 tenant 有独立的媒体生成配额（`sys_config` 表存）
- 生成历史存 `media_generation_log` 表（含 tenantId）
- 超配额时抛 `ServiceException("配额不足")`

## 已知约束

- **视频生成慢**（5-15 分钟）—— 不能做实时交互
- **图像生成 token 消耗大**—— DALL-E 3 单张图 ≈ ¥0.2-0.5
- **视频版权问题** —— 生成的视频商用前需确认模型供应商条款
- **多模态 embedding 维度不一** —— 切换 provider 时向量库需重建
- **异步任务状态需持久化** —— 进程重启后 `retrieveXxx` 才能继续轮询

## 调试与监控

- 媒体生成任务存 `media_task` 表（taskId / status / resultUrl / retryCount）
- 失败重试 3 次 → 进入 dead-letter
- 监控指标：每 provider 的成功率、平均耗时、配额使用率
- 失败告警：连续 5 次失败 → 飞书 / 邮件通知