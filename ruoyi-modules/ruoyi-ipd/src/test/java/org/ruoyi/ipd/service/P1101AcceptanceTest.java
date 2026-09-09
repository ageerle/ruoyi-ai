package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AiDocument;
import org.ruoyi.ipd.mapper.AiDocumentMapper;
import org.springframework.dao.DuplicateKeyException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P1-10.1 AI 文档原始输出与不可丢失版本链验收（主责 AC-AI-04 / AC-AI-06，BR-AI-03）。
 *
 * <p>形态为 Mockito 单元验收（对齐 OPS05/P033 惯例）；真库表结构探针（补列 + uk_ai_doc_parent）
 * 见 docs/ipd-系统说明/验收/P1-10.1-runner-ops05-20260905.md。
 * 范围限定：只验版本链存储结构（版本号+人工审核+sha256 摘要）；AI 生成/模型配置/预算属 P4-2。
 */
@Tag("dev")
@DisplayName("P1101 AI 文档版本链：v1 锚点/改版追加/审核落名/链完整/并发冲突/不可覆盖")
@ExtendWith(MockitoExtension.class)
class P1101AcceptanceTest {

    @Mock
    private AiDocumentMapper mapper;

    @InjectMocks
    private AiDocumentService service;

    /** 固定时钟：reviewed_at 断言可精确到毫秒 */
    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-09-05T12:00:00Z"), ZoneId.of("UTC"));

    @BeforeAll
    static void initTableInfo() {
        // 纯 Mockito JVM 无 mapper 注册环节，lambda 列解析需显式初始化（对齐 P033/OPS05）
        TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), ""), AiDocument.class);
    }

    private static AiDocument row(long id, int versionNo, Long parentId, String status) {
        AiDocument r = AiDocument.builder()
            .id(id).projectId(900L).docType("CHARTER").title("v" + versionNo + " 标题")
            .content("content-of-v" + versionNo).model(versionNo == 1 ? "deepseek-v3" : null)
            .status(status).parentVersionId(parentId).versionNo(versionNo)
            .contentSha256(AiDocumentService.sha256Hex("content-of-v" + versionNo))
            .build();
        r.setCreateBy(7L);
        return r;
    }

    @Test
    @DisplayName("登记 v1：versionNo=1、parent=NULL、status=GENERATED、sha256 落锚、作者绑操作者")
    void create_v1AnchorRow() {
        AiDocument created = service.createGenerated(
            900L, "CHARTER", "项目章程草案", "AI 原始输出全文", "deepseek-v3", 120, 800, 7L);

        ArgumentCaptor<AiDocument> captor = ArgumentCaptor.forClass(AiDocument.class);
        verify(mapper).insert(captor.capture());
        AiDocument row = captor.getValue();
        assertThat(row.getVersionNo()).isEqualTo(1);
        assertThat(row.getParentVersionId()).isNull();
        assertThat(row.getStatus()).isEqualTo(AiDocumentService.STATUS_GENERATED);
        assertThat(row.getContentSha256())
            .isEqualTo(AiDocumentService.sha256Hex("AI 原始输出全文"))
            .hasSize(64);
        assertThat(row.getCreateBy()).isEqualTo(7L);
        assertThat(created).isSameAs(row);
    }

    @Test
    @DisplayName("登记校验：projectId/title/content 必填，空值零插入")
    void create_validation() {
        assertThatThrownBy(() -> service.createGenerated(null, "T", "t", "c", null, 0, 0, 7L))
            .isInstanceOf(IpdBusinessException.class);
        assertThatThrownBy(() -> service.createGenerated(900L, "T", " ", "c", null, 0, 0, 7L))
            .isInstanceOf(IpdBusinessException.class);
        assertThatThrownBy(() -> service.createGenerated(900L, "T", "t", " ", null, 0, 0, 7L))
            .isInstanceOf(IpdBusinessException.class);
        verify(mapper, never()).insert(any(AiDocument.class));
    }

    @Test
    @DisplayName("AC-AI-04：审核通过后修改 ⇒ 追加 v2（parent=HEAD、需重审、摘要不同），v1 零触碰")
    void reviseAfterReview_appendsV2KeepsV1() {
        AiDocument v1 = row(1L, 1, null, AiDocumentService.STATUS_REVIEWED);
        // P1-10.3/PERF 后 head() 改走 selectChain 递归 CTE（旧 selectById/selectOne stub 已不消费）
        when(mapper.selectChain(1L)).thenReturn(List.of(v1));

        AiDocument v2 = service.revise(1L, 1L, "人工改版全文 v2", null, 7L);

        ArgumentCaptor<AiDocument> captor = ArgumentCaptor.forClass(AiDocument.class);
        verify(mapper).insert(captor.capture());
        AiDocument inserted = captor.getValue();
        assertThat(inserted.getVersionNo()).isEqualTo(2);
        assertThat(inserted.getParentVersionId()).isEqualTo(1L);
        assertThat(inserted.getStatus()).isEqualTo(AiDocumentService.STATUS_GENERATED);
        assertThat(inserted.getModel()).isNull();
        assertThat(inserted.getTitle()).isEqualTo(v1.getTitle());
        assertThat(inserted.getContentSha256())
            .isEqualTo(AiDocumentService.sha256Hex("人工改版全文 v2"))
            .isNotEqualTo(v1.getContentSha256());
        // 历史不可覆盖：改版路径只有 insert，零 updateById / 零 update
        verify(mapper, never()).updateById(any(AiDocument.class));
        verify(mapper, never()).update(any(), any());
        assertThat(v2).isSameAs(inserted);
    }

    @Test
    @DisplayName("基准非 HEAD（拿旧版提交/并发已被改）⇒ STATE_CONFLICT 明确报错，零插入")
    void revise_baseNotHead_conflict() {
        AiDocument v1 = row(1L, 1, null, AiDocumentService.STATUS_GENERATED);
        AiDocument v2 = row(2L, 2, 1L, AiDocumentService.STATUS_GENERATED);
        // selectChain 返回全链，head=链尾 v2；基准 1L 非 HEAD → STATE_CONFLICT（语义同旧 selectOne 探测）
        when(mapper.selectChain(1L)).thenReturn(List.of(v1, v2));

        assertThatThrownBy(() -> service.revise(1L, 1L, "基于旧版的改版", null, 7L))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);
        verify(mapper, never()).insert(any(AiDocument.class));
    }

    @Test
    @DisplayName("并发双写同一父版本撞 uk_ai_doc_parent ⇒ DuplicateKey 映射 STATE_CONFLICT（非静默覆盖）")
    void revise_concurrentDuplicateKey_mappedToConflict() {
        AiDocument v1 = row(1L, 1, null, AiDocumentService.STATUS_GENERATED);
        when(mapper.selectChain(1L)).thenReturn(List.of(v1));
        when(mapper.insert(any(AiDocument.class)))
            .thenThrow(new DuplicateKeyException("uk_ai_doc_parent"));

        assertThatThrownBy(() -> service.revise(1L, 1L, "并发改版", null, 7L))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);
    }

    @Test
    @DisplayName("人工审核（BR-AI-03）：条件 UPDATE 仅流转 status/reviewed_by/reviewed_at，内容零触碰")
    void review_marksReviewed() {
        service = new AiDocumentService(mapper).withClock(FIXED);
        AiDocument v1 = row(1L, 1, null, AiDocumentService.STATUS_GENERATED);
        when(mapper.selectById(1L)).thenReturn(v1);
        when(mapper.update(isNull(), any())).thenReturn(1);

        AiDocument reviewed = service.review(1L, 7L);

        assertThat(reviewed.getStatus()).isEqualTo(AiDocumentService.STATUS_REVIEWED);
        assertThat(reviewed.getReviewedBy()).isEqualTo(7L);
        assertThat(reviewed.getReviewedAt()).isEqualTo(Date.from(FIXED.instant()));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaUpdateWrapper<AiDocument>> captor =
            ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(mapper).update(isNull(), captor.capture());
        java.util.Map<String, Object> params = captor.getValue().getParamNameValuePairs();
        assertThat(params.values())
            .contains(AiDocumentService.STATUS_REVIEWED, 7L, Date.from(FIXED.instant()))
            .doesNotContain(v1.getContent())
            .doesNotContain(v1.getTitle())
            .doesNotContain(v1.getContentSha256());
    }

    @Test
    @DisplayName("审核幂等：已 REVIEWED 行直接返回，不二次更新、不覆盖首位审核人")
    void review_idempotent_alreadyReviewed() {
        AiDocument v1 = row(1L, 1, null, AiDocumentService.STATUS_REVIEWED);
        v1.setReviewedBy(3L);
        v1.setReviewedAt(new Date(0));
        when(mapper.selectById(1L)).thenReturn(v1);

        AiDocument result = service.review(1L, 7L);

        assertThat(result.getReviewedBy()).isEqualTo(3L);
        verify(mapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("ARCHIVED 行拒绝审核（须走归档流程）⇒ STATE_CONFLICT")
    void review_archived_rejected() {
        when(mapper.selectById(1L)).thenReturn(row(1L, 1, null, AiDocumentService.STATUS_ARCHIVED));

        assertThatThrownBy(() -> service.review(1L, 7L))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);
        verify(mapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("AC-AI-06：完整链检查 ⇒ v1(AI 原始输出)+v2+v3 全量返回，版本号连续无一缺失")
    void history_fullChainNoGap() {
        AiDocument v1 = row(1L, 1, null, AiDocumentService.STATUS_REVIEWED);
        AiDocument v2 = row(2L, 2, 1L, AiDocumentService.STATUS_REVIEWED);
        AiDocument v3 = row(3L, 3, 2L, AiDocumentService.STATUS_GENERATED);
        // selectChain 一次性返回全链（CTE 升序 v1..vN），链连续性由 service 校验
        when(mapper.selectChain(3L)).thenReturn(List.of(v1, v2, v3));

        List<AiDocument> chain = service.history(3L);

        assertThat(chain).extracting(AiDocument::getId, AiDocument::getVersionNo)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(1L, 1),
                org.assertj.core.groups.Tuple.tuple(2L, 2),
                org.assertj.core.groups.Tuple.tuple(3L, 3));
        assertThat(chain.get(0).getParentVersionId()).isNull();
        assertThat(chain.get(1).getParentVersionId()).isEqualTo(1L);
        assertThat(chain.get(2).getParentVersionId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("链断裂（父版本被软删，向上走不到根）⇒ STATE_CONFLICT")
    void history_brokenChain_rejected() {
        AiDocument v3 = row(3L, 3, 2L, AiDocumentService.STATUS_GENERATED);
        // v2 已软删：CTE 向上走到断点，链只含 v3 且到不了 v1 根（chain[0].versionNo != 1）
        when(mapper.selectChain(3L)).thenReturn(List.of(v3));

        assertThatThrownBy(() -> service.history(3L))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);
    }

    @Test
    @DisplayName("链跳号（中间版本缺失致版本号不连续）⇒ STATE_CONFLICT")
    void history_versionJump_rejected() {
        AiDocument v1 = row(1L, 1, null, AiDocumentService.STATUS_GENERATED);
        AiDocument v3 = row(3L, 3, 1L, AiDocumentService.STATUS_GENERATED);
        // CTE 返回跳号链 [v1, v3]（中间 v2 缺失）
        when(mapper.selectChain(3L)).thenReturn(List.of(v1, v3));

        assertThatThrownBy(() -> service.history(3L))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);
    }

    @Test
    @DisplayName("不存在/已删文档 ⇒ NOT_FOUND")
    void history_notFound() {
        // CTE 无命中 → 空链 ⇒ NOT_FOUND（旧 selectById(404L) stub 已不消费）
        when(mapper.selectChain(404L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.history(404L))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ApiV1ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("根非 v1（链被截头）⇒ STATE_CONFLICT")
    void history_rootNotV1_rejected() {
        AiDocument orphan = row(9L, 2, null, AiDocumentService.STATUS_GENERATED);
        // 链被截头：CTE 只回孤儿行自身，chain[0].versionNo=2 != 1 ⇒ STATE_CONFLICT
        when(mapper.selectChain(9L)).thenReturn(List.of(orphan));

        assertThatThrownBy(() -> service.history(9L))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);
    }

    @Test
    @DisplayName("sha256 摘要契约：64 位 hex、随内容变化")
    void sha256_contract() {
        String a = AiDocumentService.sha256Hex("内容 A");
        String b = AiDocumentService.sha256Hex("内容 B");
        assertThat(a).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(b).hasSize(64).isNotEqualTo(a);
        assertThat(AiDocumentService.sha256Hex("内容 A")).isEqualTo(a);
    }
}
