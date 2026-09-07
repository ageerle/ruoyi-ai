package org.ruoyi.ipd.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;
import org.ruoyi.ipd.domain.AiDocument;

import java.util.Date;
import java.util.List;

/**
 * AI 文档版本链 Mapper（P1-10.1）。
 * 纪律：版本链 append-only——新版本只 insert，历史行内容/摘要零更新通道；
 * 唯一允许的行内更新是审核状态流转（status/reviewed_by/reviewed_at 三列，
 * 经 service 内受控 lambdaUpdate），不提供 updateById(entity) 业务调用通道，
 * 防止并发覆盖历史版本。
 */
public interface AiDocumentMapper extends BaseMapperPlus<AiDocument, AiDocument> {

    /**
     * P4-2.2：本月已消耗 token 聚合（AC-AI-08 月度预算检查）。
     * 仅 AI 原始输出行有 token 计量（人工改版 model/token 为 NULL，SUM 自动跳过）；
     * 自定义 @Select 不走 MP 逻辑删除改写，del_flag 须显式过滤。
     */
    @Select("SELECT COALESCE(SUM(token_prompt + token_completion), 0) FROM ai_documents "
        + "WHERE del_flag = '0' AND create_time >= #{monthStart}")
    Long sumTokensSince(@Param("monthStart") Date monthStart);

    /**
     * P1-10.3 / PERF：自任一版本行 ID 递归向上找根 + 向下取全链，
     * 一次 SQL 替代 history() 的 2N-1 次往返。
     * 租户/逻辑删除由 service 内显式过滤；del_flag = '0' 在 CTE 内自带。
     */
    @Select("""
        WITH RECURSIVE chain AS (
            SELECT id, project_id, doc_type, title, content, model,
                   token_prompt, token_completion, content_sha256, status,
                   parent_version_id, version_no, reviewed_by, reviewed_at,
                   review_comment, archived_at, archived_by,
                   create_dept, create_by, create_time, update_by, update_time,
                   tenant_id, del_flag, remark
            FROM ai_documents
            WHERE id = #{rootId} AND del_flag = '0'
            UNION ALL
            SELECT d.id, d.project_id, d.doc_type, d.title, d.content, d.model,
                   d.token_prompt, d.token_completion, d.content_sha256, d.status,
                   d.parent_version_id, d.version_no, d.reviewed_by, d.review_at,
                   d.review_comment, d.archived_at, d.archived_by,
                   d.create_dept, d.create_by, d.create_time, d.update_by, d.update_time,
                   d.tenant_id, d.del_flag, d.remark
            FROM ai_documents d INNER JOIN chain c ON d.parent_version_id = c.id
            WHERE d.del_flag = '0'
        )
        SELECT * FROM chain ORDER BY version_no ASC
        """)
    List<AiDocument> selectChain(@Param("rootId") Long rootId);
}
