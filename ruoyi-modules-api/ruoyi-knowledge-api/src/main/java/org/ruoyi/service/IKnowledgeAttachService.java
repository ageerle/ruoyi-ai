package org.ruoyi.service;


import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.domain.bo.KnowledgeAttachBo;
import org.ruoyi.domain.vo.KnowledgeAttachVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.List;

/**
 * 知识库附件Service接口
 *
 * @author ageerle
 * @date 2025-04-08
 */
public interface IKnowledgeAttachService {

    /**
     * 查询知识库附件
     */
    KnowledgeAttachVo queryById(Long id);

    /**
     * 查询知识库附件列表
     */
    TableDataInfo<KnowledgeAttachVo> queryPageList(KnowledgeAttachBo bo, PageQuery pageQuery);

    /**
     * 查询知识库附件列表
     */
    List<KnowledgeAttachVo> queryList(KnowledgeAttachBo bo);

    /**
     * 新增知识库附件
     */
    Boolean insertByBo(KnowledgeAttachBo bo);

    /**
     * 修改知识库附件
     */
    Boolean updateByBo(KnowledgeAttachBo bo);

    /**
     * 校验并批量删除知识库附件信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 删除知识附件
     */
    void removeKnowledgeAttach(String docId);

    /**
     * 翻译文件
     *
     * @param file 文件
     * @param targetLanguage 目标语音
     */
    String translationByFile(MultipartFile file, String targetLanguage);
}
