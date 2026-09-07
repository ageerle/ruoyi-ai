package org.ruoyi.ipd.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;
import org.ruoyi.ipd.domain.AiDocument;

import java.util.Date;

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
}
