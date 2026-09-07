package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AiDocument;
import org.ruoyi.ipd.mapper.AiDocumentMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * AI 文档原始输出与不可丢失版本链服务（P1-10.1；主责 AC-AI-04 / AC-AI-06，BR-AI-03）。
 *
 * <p>版本链模型（append-only）：
 * <ul>
 *   <li>v1 = AI 原始输出（status=GENERATED，parent=NULL）——生成入口由 P4-2 接管，
 *       本服务仅登记版本链首环；</li>
 *   <li>人工改版 {@link #revise} = 基于当前 HEAD 追加 v(n+1)（parent=HEAD.id，status=GENERATED
 *       需重新人工审核）；基准版本非 HEAD → STATE_CONFLICT 明确报错，绝不静默覆盖；</li>
 *   <li>人工审核 {@link #review} = 行内状态流转 GENERATED→REVIEWED + reviewed_by/reviewed_at
 *       落名（BR-AI-03：AI 输出未经审核不生效）——条件 UPDATE，不触碰 content/摘要；</li>
 *   <li>历史不可覆盖：全程零 content/标题/摘要更新通道，修正只能产生新版本；</li>
 *   <li>并发防分叉三层防线：服务层 HEAD 校验 → DB 层 uk_ai_doc_parent 唯一索引 →
 *       DuplicateKeyException 映射 STATE_CONFLICT（与 P0-3.3 insertVersion 同款包络）。</li>
 * </ul>
 *
 * <p>链完整性（AC-AI-06）：{@link #history} 自任意版本行向上走到根（根必须 v1）、再向下
 * 按父指针逐环下探，校验版本号连续 v1..vN 无缺失、父链接无断点；任一断点即 STATE_CONFLICT。
 */
@Service
public class AiDocumentService {

    /** AI 原始输出/人工改版后待审 */
    public static final String STATUS_GENERATED = "GENERATED";
    /** 人工审核通过（BR-AI-03） */
    public static final String STATUS_REVIEWED = "REVIEWED";
    /** 审核拒绝——终态，必须重新走 review 流后才能 archive（BR-AI-03 兜底） */
    public static final String STATUS_REJECTED = "REJECTED";
    /** 已归档——终态，仅 REVIEWED 行可归档；归档后不可改版/拒绝/再归档 */
    public static final String STATUS_ARCHIVED = "ARCHIVED";

    /** AC-AI-03：未审核不可归档的对外文案（"须人工审核确认" 固定字面量） */
    public static final String MSG_REVIEW_REQUIRED = "AI 文档须人工审核确认才可归档";

    /** AC-AI-05：diff 报告结构——仅存差异字段，全文重写由调用方按 contentSha256 自查 */
    public record FieldDiff(String field, String fromValue, String toValue, String fromSha256, String toSha256) {}

    /** AC-AI-05：两版本 diff 报告（包含两条版本行 + 字段级差异列表） */
    public record DiffReport(Long fromVersionId, Integer fromVersionNo,
                             Long toVersionId, Integer toVersionNo,
                             List<FieldDiff> differences) {}

    private final AiDocumentMapper mapper;
    private java.time.Clock clock = java.time.Clock.systemDefaultZone();

    public AiDocumentService(AiDocumentMapper mapper) {
        this.mapper = mapper;
    }

    /** 测试口：注入固定时钟（reviewed_at 断言）；生产走系统时钟。 */
    AiDocumentService withClock(java.time.Clock fixed) {
        this.clock = fixed;
        return this;
    }

    /**
     * 登记 AI 原始输出 v1（版本链首环；AI 生成/模型配置/预算属 P4-2，不在本卡）。
     *
     * @return 落库行（versionNo=1，status=GENERATED，contentSha256 已算）
     */
    @Transactional(rollbackFor = Exception.class)
    public AiDocument createGenerated(Long projectId, String docType, String title, String content,
                                      String model, Integer tokenPrompt, Integer tokenCompletion,
                                      Long operatorId) {
        requireArg(projectId != null, "projectId 必填");
        requireArg(title != null && !title.isBlank(), "title 必填");
        requireArg(title.length() <= 200, "title 超长（≤200）");
        requireArg(content != null && !content.isBlank(), "content 必填（AI 原始输出不可为空）");

        AiDocument row = AiDocument.builder()
            .projectId(projectId).docType(docType).title(title).content(content)
            .model(model).tokenPrompt(tokenPrompt).tokenCompletion(tokenCompletion)
            .status(STATUS_GENERATED).parentVersionId(null).versionNo(1)
            .contentSha256(sha256Hex(content))
            .build();
        row.setCreateBy(operatorId);
        mapper.insert(row);
        return row;
    }

    /**
     * 人工改版（AC-AI-04：审核通过后修改 ⇒ 生成新版本 v2，v1 保留）。
     * 基准版本必须为当前 HEAD；非 HEAD（并发被他人改过/拿旧版提交）→ STATE_CONFLICT
     * 明确报错，绝不静默覆盖任何历史版本。
     *
     * @param documentId    链上任一版本行 ID（服务自行解析 HEAD）
     * @param baseVersionId 调用方声明的基准版本（乐观锁用途）
     * @param newContent    改版全文
     * @param title         新标题（空则沿用 HEAD）
     * @return 新版本行 v(n+1)（status=GENERATED，需重新人工审核）
     */
    @Transactional(rollbackFor = Exception.class)
    public AiDocument revise(Long documentId, Long baseVersionId, String newContent,
                             String title, Long operatorId) {
        requireArg(documentId != null, "documentId 必填");
        requireArg(baseVersionId != null, "baseVersionId 必填（声明基准版本）");
        requireArg(newContent != null && !newContent.isBlank(), "改版内容必填");

        AiDocument head = head(documentId);
        if (!baseVersionId.equals(head.getId())) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT);
        }

        AiDocument next = AiDocument.builder()
            .projectId(head.getProjectId()).docType(head.getDocType())
            .title(title != null && !title.isBlank() ? title : head.getTitle())
            .content(newContent)
            .model(null)
            .status(STATUS_GENERATED)
            .parentVersionId(head.getId())
            .versionNo(head.getVersionNo() + 1)
            .contentSha256(sha256Hex(newContent))
            .build();
        next.setCreateBy(operatorId);
        try {
            mapper.insert(next);
        } catch (DuplicateKeyException e) {
            // uk_ai_doc_parent：并发双写同一父版本被 DB 拦截 → 明确报冲突，不静默覆盖
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT);
        }
        return next;
    }

    /**
     * 人工审核通过（BR-AI-03）。仅流转 status + 审核落名三列，内容零触碰；
     * 已审核行幂等返回（不覆盖首位审核人）；ARCHIVED 行拒绝（需走归档流程）。
     *
     * @return 审核后（或幂等时既有）行
     */
    @Transactional(rollbackFor = Exception.class)
    public AiDocument review(Long versionId, Long operatorId) {
        AiDocument row = mapper.selectById(versionId);
        if (row == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND);
        }
        if (STATUS_REVIEWED.equals(row.getStatus())) {
            return row;
        }
        if (STATUS_ARCHIVED.equals(row.getStatus())) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT);
        }
        Date now = Date.from(clock.instant());
        int updated = mapper.update(null, Wrappers.<AiDocument>lambdaUpdate()
            .eq(AiDocument::getId, versionId)
            .eq(AiDocument::getStatus, STATUS_GENERATED)
            .set(AiDocument::getStatus, STATUS_REVIEWED)
            .set(AiDocument::getReviewedBy, operatorId)
            .set(AiDocument::getReviewedAt, now));
        if (updated > 0) {
            row.setStatus(STATUS_REVIEWED);
            row.setReviewedBy(operatorId);
            row.setReviewedAt(now);
            return row;
        }
        // 并发已被他人审核：重读终态返回，不报错不覆盖
        AiDocument fresh = mapper.selectById(versionId);
        return fresh != null ? fresh : row;
    }

    /**
     * AC-AI-03 / BR-AI-02：未审核不可归档。仅流转 status + 归档落名三列；
     * 仅 status=REVIEWED 行可归档（GENERATED/REJECTED 拒绝；ARCHIVED 幂等拒绝）。
     *
     * @param versionId  任一版本行 ID（服务自行定位行）
     * @param operatorId 操作者（写入 archived_by 审计身份）
     * @return 归档后行
     * @throws IpdBusinessException STATE_CONFLICT 未审核 / 已归档 / 已拒绝
     */
    @Transactional(rollbackFor = Exception.class)
    public AiDocument archive(Long versionId, Long operatorId) {
        AiDocument row = mapper.selectById(versionId);
        if (row == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND);
        }
        // AC-AI-03：未审核（包括 GENERATED 初次/REJECTED 拒绝后未重审）→ 拒绝
        if (!STATUS_REVIEWED.equals(row.getStatus())) {
            throw new IpdBusinessException(
                ApiV1ErrorCode.STATE_CONFLICT, MSG_REVIEW_REQUIRED);
        }
        Date now = Date.from(clock.instant());
        // 条件 UPDATE：再次防御并发（他人同时归档覆盖 → 0 行受影响 → 重读终态）
        int updated = mapper.update(null, Wrappers.<AiDocument>lambdaUpdate()
            .eq(AiDocument::getId, versionId)
            .eq(AiDocument::getStatus, STATUS_REVIEWED)
            .set(AiDocument::getStatus, STATUS_ARCHIVED)
            .set(AiDocument::getArchivedAt, now)
            .set(AiDocument::getArchivedBy, operatorId));
        if (updated > 0) {
            row.setStatus(STATUS_ARCHIVED);
            row.setArchivedAt(now);
            row.setArchivedBy(operatorId);
            return row;
        }
        // 并发已被他人归档：终态自洽返回
        AiDocument fresh = mapper.selectById(versionId);
        if (fresh != null && STATUS_ARCHIVED.equals(fresh.getStatus())) {
            return fresh;
        }
        // 并发期间被 reject / delete 抢走：拒绝
        throw new IpdBusinessException(
            ApiV1ErrorCode.STATE_CONFLICT, MSG_REVIEW_REQUIRED);
    }

    /**
     * 审核拒绝（BR-AI-03 兜底：审核可拒绝已 REVIEWED 行，强制重新走审核流才能 ARCHIVED）。
     * 仅 status=REVIEWED 行可拒绝（GENERATED 状态无须拒绝、ARCHIVED 终态不可拒、REJECTED 幂等）。
     *
     * @param versionId  版本行 ID
     * @param operatorId 操作者（写入 reviewed_by 兜底；幂等拒绝时不覆盖）
     * @param comment    拒绝原因（必填；落 review_comment 审计完整性）
     */
    @Transactional(rollbackFor = Exception.class)
    public AiDocument reject(Long versionId, Long operatorId, String comment) {
        requireArg(comment != null && !comment.isBlank(), "拒绝原因必填");
        AiDocument row = mapper.selectById(versionId);
        if (row == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND);
        }
        if (STATUS_REJECTED.equals(row.getStatus())) {
            return row; // 幂等：已拒绝不覆盖原 reviewComment / reviewedBy
        }
        if (STATUS_ARCHIVED.equals(row.getStatus())) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                "已归档行不可拒绝（终态）");
        }
        if (!STATUS_REVIEWED.equals(row.getStatus())) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                "仅 REVIEWED 行可拒绝（GENERATED 状态请先 review）");
        }
        Date now = Date.from(clock.instant());
        int updated = mapper.update(null, Wrappers.<AiDocument>lambdaUpdate()
            .eq(AiDocument::getId, versionId)
            .eq(AiDocument::getStatus, STATUS_REVIEWED)
            .set(AiDocument::getStatus, STATUS_REJECTED)
            .set(AiDocument::getReviewComment, comment)
            .set(AiDocument::getReviewedAt, now));
        if (updated > 0) {
            row.setStatus(STATUS_REJECTED);
            row.setReviewComment(comment);
            row.setReviewedAt(now);
            return row;
        }
        AiDocument reread = mapper.selectById(versionId);
        if (reread != null && STATUS_REJECTED.equals(reread.getStatus())) {
            return reread;
        }
        throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT);
    }

    /**
     * AC-AI-05：两版本字段级 diff（fromVersionId 任意版本行 ID → toVersionId 任意版本行 ID）。
     * 仅在同一条链上返回有意义的结果；跨链调用方需自行负责。
     * 差异字段：title/content/contentSha256（review/archived 落名不参与 diff——属审计维度）。
     *
     * @return DiffReport，含两版本行 ID/版本号 + 字段级差异列表（无差异返回空列表）
     */
    public DiffReport diff(Long fromVersionId, Long toVersionId) {
        requireArg(fromVersionId != null, "fromVersionId 必填");
        requireArg(toVersionId != null, "toVersionId 必填");
        AiDocument from = mapper.selectById(fromVersionId);
        AiDocument to = mapper.selectById(toVersionId);
        if (from == null || to == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND);
        }
        List<FieldDiff> diffs = new ArrayList<>();
        // title：同链 v(n+1) 若未传 title 应等于 v(n)，此处等价视为无差异
        if (!safeEq(from.getTitle(), to.getTitle())) {
            diffs.add(new FieldDiff("title", from.getTitle(), to.getTitle(), null, null));
        }
        // content：必产生新版本 ⇒ 必有差异
        if (!safeEq(from.getContent(), to.getContent())) {
            diffs.add(new FieldDiff("content", from.getContent(), to.getContent(),
                from.getContentSha256(), to.getContentSha256()));
        }
        // contentSha256：内容摘要差异（content 未变则摘要必同；列独立列便于审计溯源）
        if (!safeEq(from.getContentSha256(), to.getContentSha256())) {
            diffs.add(new FieldDiff("contentSha256",
                from.getContentSha256(), to.getContentSha256(), null, null));
        }
        return new DiffReport(from.getId(), from.getVersionNo(),
            to.getId(), to.getVersionNo(), diffs);
    }

    private static boolean safeEq(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    /**
     * 完整版本链（AC-AI-06：AI 原始输出 v1 + 全部人工修改版本，无一缺失）。
     * 自起点向上走到根（根必须 v1），再自根按父指针逐环下探，校验版本号连续；
     * 链断/跳号/起点不在链上（软删分支）→ STATE_CONFLICT。
     *
     * @return v1..vN 升序全链
     */
    public List<AiDocument> history(Long documentId) {
        AiDocument from = mapper.selectById(documentId);
        if (from == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND);
        }
        AiDocument root = from;
        while (root.getParentVersionId() != null) {
            AiDocument parent = mapper.selectById(root.getParentVersionId());
            if (parent == null) {
                throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT);
            }
            root = parent;
        }
        if (root.getVersionNo() == null || root.getVersionNo() != 1) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT);
        }

        List<AiDocument> chain = new ArrayList<>();
        chain.add(root);
        AiDocument cursor = root;
        while (true) {
            AiDocument child = mapper.selectOne(new LambdaQueryWrapper<AiDocument>()
                .eq(AiDocument::getParentVersionId, cursor.getId()));
            if (child == null) {
                break;
            }
            if (child.getVersionNo() == null
                || child.getVersionNo() != cursor.getVersionNo() + 1) {
                // 缺版本/跳号（如链中版本被软删导致父链接跳空）
                throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT);
            }
            chain.add(child);
            cursor = child;
        }
        boolean onChain = chain.stream().anyMatch(r -> documentId.equals(r.getId()));
        if (!onChain) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT);
        }
        return chain;
    }

    /** 当前链头（最新版本）。 */
    private AiDocument head(Long documentId) {
        List<AiDocument> chain = history(documentId);
        return chain.get(chain.size() - 1);
    }

    /**
     * sha256(content) 十六进制摘要（64 字符，UTF-8）。
     */
    public static String sha256Hex(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16))
                    .append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 摘要算法不可用", e);
        }
    }

    private static void requireArg(boolean ok, String message) {
        if (!ok) {
            throw new IpdBusinessException(message);
        }
    }
}
