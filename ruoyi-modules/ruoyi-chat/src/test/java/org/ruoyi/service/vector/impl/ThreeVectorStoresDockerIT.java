package org.ruoyi.service.vector.impl;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.config.VectorStoreProperties;
import org.ruoyi.domain.bo.vector.QueryVectorBo;
import org.ruoyi.domain.bo.vector.StoreEmbeddingBo;
import org.ruoyi.domain.vo.knowledge.KnowledgeRetrievalVo;
import org.ruoyi.domain.entity.knowledge.KnowledgeInfo;
import org.ruoyi.factory.EmbeddingModelFactory;
import org.ruoyi.enums.ModalityType;
import org.ruoyi.mapper.knowledge.KnowledgeAttachMapper;
import org.ruoyi.mapper.knowledge.KnowledgeInfoMapper;
import org.ruoyi.service.vector.VectorStoreService;
import org.ruoyi.service.embed.BaseEmbedModelService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
@EnabledIfEnvironmentVariable(named = "RAG_DOCKER_IT", matches = "true")
class ThreeVectorStoresDockerIT {

    private static final String MODEL = "codex-deterministic-embedding";
    private static final int DIMENSION = 32;

    @Test
    void weaviateLifecycle() {
        verifyLifecycle("weaviate", strategy("weaviate"));
    }

    @Test
    void milvusLifecycle() {
        verifyLifecycle("milvus", strategy("milvus"));
    }

    @Test
    void qdrantLifecycle() {
        verifyLifecycle("qdrant", strategy("qdrant"));
    }

    private void verifyLifecycle(String type, VectorStoreService strategy) {
        String kid = switch (type) {
            case "weaviate" -> "990000001";
            case "milvus" -> "990000002";
            case "qdrant" -> "990000003";
            default -> throw new IllegalArgumentException(type);
        };
        String docId = "codex_rag_verify_doc_" + type;
        List<String> fids = List.of(
            "codex_rag_verify_fid_" + type + "_1",
            "codex_rag_verify_fid_" + type + "_2");
        try {
            strategy.createSchema(kid, MODEL);
            strategy.storeEmbeddings(store(type, kid, docId, fids));

            List<KnowledgeRetrievalVo> found = strategy.search(query(type, kid, "deterministic alpha"));
            assertFalse(found.isEmpty(), type + " must return stored vectors");
            assertTrue(found.stream().anyMatch(v -> fids.contains(v.getId())));

            strategy.removeByFid(fids.get(0), kid);
            assertEventually(() -> strategy.search(query(type, kid, "deterministic alpha")).stream()
                .noneMatch(v -> fids.get(0).equals(v.getId())), type + " fid delete");

            strategy.removeByDocId(docId, kid);
            assertEventually(() -> strategy.search(query(type, kid, "deterministic beta")).isEmpty(),
                type + " doc delete");
        } finally {
            strategy.removeById(kid, MODEL);
        }
    }

    private void assertEventually(BooleanSupplier condition, String operation) {
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(15);
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) return;
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail(operation + " interrupted");
            }
        }
        fail(operation + " did not become visible within 15 seconds");
    }

    private VectorStoreService strategy(String type) {
        VectorStoreProperties properties = properties();
        EmbeddingModelFactory factory = mock(EmbeddingModelFactory.class);
        when(factory.createModel(MODEL)).thenReturn(new DeterministicEmbeddingModel());
        IChatModelService modelService = mock(IChatModelService.class);
        ChatModelVo model = new ChatModelVo();
        model.setModelDimension(DIMENSION);
        when(modelService.selectModelByName(MODEL)).thenReturn(model);
        KnowledgeAttachMapper attachMapper = mock(KnowledgeAttachMapper.class);
        return switch (type) {
            case "weaviate" -> new WeaviateVectorStoreStrategy(properties, modelService, factory, attachMapper);
            case "milvus" -> {
                KnowledgeInfoMapper infoMapper = mock(KnowledgeInfoMapper.class);
                KnowledgeInfo info = new KnowledgeInfo();
                info.setEmbeddingModel(MODEL);
                when(infoMapper.selectById(anyLong())).thenReturn(info);
                yield new MilvusVectorStoreStrategy(properties, modelService, factory, attachMapper, infoMapper);
            }
            case "qdrant" -> new QdrantVectorStoreStrategy(properties, modelService, factory, attachMapper);
            default -> throw new IllegalArgumentException(type);
        };
    }

    private VectorStoreProperties properties() {
        VectorStoreProperties p = new VectorStoreProperties();
        p.getWeaviate().setProtocol("http");
        p.getWeaviate().setHost("127.0.0.1:28080");
        p.getWeaviate().setClassname("CodexRagVerify");
        p.getMilvus().setUrl("http://127.0.0.1:19530");
        p.getMilvus().setCollectionname("CodexRagVerify");
        p.getQdrant().setHost("127.0.0.1");
        p.getQdrant().setPort(6334);
        p.getQdrant().setCollectionname("CodexRagVerify");
        return p;
    }

    private StoreEmbeddingBo store(String type, String kid, String docId, List<String> fids) {
        StoreEmbeddingBo bo = new StoreEmbeddingBo();
        bo.setVectorStoreName(type);
        bo.setKid(kid);
        bo.setDocId(docId);
        bo.setEmbeddingModelName(MODEL);
        bo.setFids(fids);
        bo.setChunkList(List.of("deterministic alpha", "deterministic beta"));
        return bo;
    }

    private QueryVectorBo query(String type, String kid, String text) {
        QueryVectorBo bo = new QueryVectorBo();
        bo.setVectorModelName(type);
        bo.setKid(kid);
        bo.setQuery(text);
        bo.setEmbeddingModelName(MODEL);
        bo.setMaxResults(10);
        return bo;
    }

    private static final class DeterministicEmbeddingModel implements BaseEmbedModelService {
        @Override
        public void configure(ChatModelVo config) {
        }

        @Override
        public Set<ModalityType> getSupportedModalities() {
            return Set.of(ModalityType.TEXT);
        }

        @Override
        public Response<Embedding> embed(String text) {
            return Response.from(vector(text));
        }

        @Override
        public Response<Embedding> embed(TextSegment segment) {
            return embed(segment.text());
        }

        @Override
        public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
            List<Embedding> embeddings = new ArrayList<>(segments.size());
            for (TextSegment segment : segments) embeddings.add(vector(segment.text()));
            return Response.from(embeddings);
        }

        private static Embedding vector(String text) {
            try {
                byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8));
                float[] values = new float[DIMENSION];
                for (int i = 0; i < values.length; i++) values[i] = (digest[i] & 0xff) / 255.0f;
                return Embedding.from(values);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
