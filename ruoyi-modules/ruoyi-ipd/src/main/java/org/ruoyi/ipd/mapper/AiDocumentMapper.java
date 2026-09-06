package org.ruoyi.ipd.mapper;

import org.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;
import org.ruoyi.ipd.domain.AiDocument;

/**
 * AI 文档版本链 Mapper（P1-10.1）。
 * 纪律：版本链 append-only——新版本只 insert，历史行内容/摘要零更新通道；
 * 唯一允许的行内更新是审核状态流转（status/reviewed_by/reviewed_at 三列，
 * 经 service 内受控 lambdaUpdate），不提供 updateById(entity) 业务调用通道，
 * 防止并发覆盖历史版本。
 */
public interface AiDocumentMapper extends BaseMapperPlus<AiDocument, AiDocument> {
}
