package org.ruoyi.service.vector.impl;

import com.google.common.util.concurrent.Futures;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.output.Response;
import io.grpc.Status;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections.CollectionOperationResponse;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Points.DeletePoints;
import io.qdrant.client.grpc.Points.UpdateResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.config.VectorStoreProperties;
import org.ruoyi.domain.bo.vector.StoreEmbeddingBo;
import org.ruoyi.factory.EmbeddingModelFactory;
import org.ruoyi.mapper.knowledge.KnowledgeAttachMapper;
import org.ruoyi.service.embed.BaseEmbedModelService;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class QdrantVectorStoreStrategyTest {

    private static final String KID = "42";
    private static final String COLLECTION = "LocalKnowledge" + KID;
    private static final String DOC_ID = "document-1";
    private static final String FID = "fragment-1";
    private static final String MODEL = "test-embedding";

    private MockedStatic<QdrantGrpcClient> grpcFactory;
    private MockedConstruction<QdrantClient> clients;
    private QdrantVectorStoreStrategy strategy;
    private IChatModelService modelService;
    private EmbeddingModelFactory embeddingModelFactory;
    private Throwable deletionFailure;

    @BeforeEach
    void setUp() {
        // Keep the real LangChain4j store/filter conversion and exception wrapping,
        // replacing only the Qdrant transport so no running database is required.
        QdrantGrpcClient.Builder builder = mock(QdrantGrpcClient.Builder.class, RETURNS_SELF);
        grpcFactory = mockStatic(QdrantGrpcClient.class);
        grpcFactory.when(() -> QdrantGrpcClient.newBuilder("localhost", 6334, false)).thenReturn(builder);
        clients = mockConstruction(QdrantClient.class, (client, context) -> {
            when(client.deleteCollectionAsync(COLLECTION)).thenAnswer(invocation -> deletionFailure == null
                ? Futures.immediateFuture(CollectionOperationResponse.getDefaultInstance())
                : Futures.immediateFailedFuture(deletionFailure));
            when(client.deleteAsync(any(DeletePoints.class))).thenAnswer(invocation -> deletionFailure == null
                ? Futures.immediateFuture(UpdateResult.getDefaultInstance())
                : Futures.immediateFailedFuture(deletionFailure));
            when(client.collectionExistsAsync(COLLECTION)).thenReturn(Futures.immediateFuture(false));
            when(client.createCollectionAsync(eq(COLLECTION), any(VectorParams.class)))
                .thenReturn(Futures.immediateFuture(CollectionOperationResponse.getDefaultInstance()));
            when(client.upsertAsync(eq(COLLECTION), anyList()))
                .thenReturn(Futures.immediateFuture(UpdateResult.getDefaultInstance()));
        });
        modelService = mock(IChatModelService.class);
        embeddingModelFactory = mock(EmbeddingModelFactory.class);
        strategy = new QdrantVectorStoreStrategy(new VectorStoreProperties(), modelService,
            embeddingModelFactory, mock(KnowledgeAttachMapper.class));
    }

    @AfterEach
    void tearDown() {
        clients.close();
        grpcFactory.close();
    }

    @ParameterizedTest
    @EnumSource(DeleteOperation.class)
    void missingCollectionIsAlreadyDeleted(DeleteOperation operation) {
        deletionFailure = Status.NOT_FOUND.withDescription("Collection does not exist").asRuntimeException();

        assertDoesNotThrow(() -> delete(operation));

        verifyDeleteRequest(operation);
        verifyNoInteractions(modelService, embeddingModelFactory);
    }

    @ParameterizedTest
    @EnumSource(DeleteOperation.class)
    void wrappedCheckedNotFoundIsAlreadyDeleted(DeleteOperation operation) {
        deletionFailure = Status.NOT_FOUND.asException();

        assertDoesNotThrow(() -> delete(operation));
    }

    @ParameterizedTest
    @EnumSource(DeleteOperation.class)
    void existingCollectionStillDeletesRequestedData(DeleteOperation operation) {
        assertDoesNotThrow(() -> delete(operation));

        verifyDeleteRequest(operation);
    }

    @ParameterizedTest
    @EnumSource(DeleteOperation.class)
    void connectionFailureIsNotIgnored(DeleteOperation operation) {
        assertDeletionFails(operation, Status.UNAVAILABLE.asRuntimeException());
    }

    @ParameterizedTest
    @EnumSource(DeleteOperation.class)
    void permissionFailureIsNotIgnored(DeleteOperation operation) {
        assertDeletionFails(operation, Status.PERMISSION_DENIED.asRuntimeException());
    }

    @ParameterizedTest
    @EnumSource(DeleteOperation.class)
    void notFoundTextWithoutGrpcStatusIsNotIgnored(DeleteOperation operation) {
        assertDeletionFails(operation, new IllegalStateException("NOT_FOUND"));
    }

    @Test
    void firstUploadCanCleanMissingCollectionThenCreateAndStoreEmbeddings() throws Exception {
        deletionFailure = Status.NOT_FOUND.asRuntimeException();
        ChatModelVo model = new ChatModelVo();
        model.setModelDimension(2);
        when(modelService.selectModelByName(MODEL)).thenReturn(model);
        BaseEmbedModelService embeddingModel = mock(BaseEmbedModelService.class);
        when(embeddingModelFactory.createModel(MODEL)).thenReturn(embeddingModel);
        when(embeddingModel.embedAll(anyList()))
            .thenReturn(Response.from(List.of(Embedding.from(new float[] {1, 0}))));
        StoreEmbeddingBo input = new StoreEmbeddingBo();
        input.setKid(KID);
        input.setDocId(DOC_ID);
        input.setEmbeddingModelName(MODEL);
        input.setFids(List.of(FID));
        input.setChunkList(List.of("first upload"));

        assertDoesNotThrow(() -> {
            strategy.removeByDocId(DOC_ID, KID);
            strategy.storeEmbeddings(input);
        });

        assertEquals(3, clients.constructed().size());
        verify(clients.constructed().get(0)).deleteAsync(any(DeletePoints.class));
        verify(clients.constructed().get(1)).createCollectionAsync(eq(COLLECTION),
            argThat((VectorParams params) -> params.getSize() == 2));
        verify(clients.constructed().get(2)).upsertAsync(eq(COLLECTION), argThat(points ->
            points.size() == 1
                && DOC_ID.equals(points.get(0).getPayloadOrThrow("doc_id").getStringValue())
                && FID.equals(points.get(0).getPayloadOrThrow("fid").getStringValue())
                && "first upload".equals(points.get(0).getPayloadOrThrow("text_segment").getStringValue())));
    }

    private void assertDeletionFails(DeleteOperation operation, Throwable failure) {
        deletionFailure = failure;

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> delete(operation));

        if (operation == DeleteOperation.COLLECTION) {
            assertInstanceOf(ServiceException.class, thrown);
        } else {
            assertSame(failure, thrown.getCause().getCause());
        }
        verifyDeleteRequest(operation);
    }

    private void verifyDeleteRequest(DeleteOperation operation) {
        assertEquals(1, clients.constructed().size());
        QdrantClient client = clients.constructed().get(0);
        if (operation == DeleteOperation.COLLECTION) {
            verify(client).deleteCollectionAsync(COLLECTION);
            verify(client).close();
            return;
        }
        ArgumentCaptor<DeletePoints> request = ArgumentCaptor.forClass(DeletePoints.class);
        verify(client).deleteAsync(request.capture());
        assertEquals(COLLECTION, request.getValue().getCollectionName());
        var filter = request.getValue().getPoints().getFilter();
        assertEquals(1, filter.getMustCount());
        var field = filter.getMust(0).getField();
        assertEquals(operation == DeleteOperation.DOCUMENT ? "doc_id" : "fid", field.getKey());
        assertEquals(operation == DeleteOperation.DOCUMENT ? DOC_ID : FID, field.getMatch().getKeyword());
    }

    private void delete(DeleteOperation operation) {
        switch (operation) {
            case COLLECTION -> strategy.removeById(KID, MODEL);
            case DOCUMENT -> strategy.removeByDocId(DOC_ID, KID);
            case FRAGMENT -> strategy.removeByFid(FID, KID);
        }
    }

    private enum DeleteOperation {
        COLLECTION, DOCUMENT, FRAGMENT
    }
}
