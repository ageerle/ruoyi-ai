package org.ruoyi.knowledge.service;

import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.knowledge.domain.KnowledgeAttach;
import org.ruoyi.knowledge.domain.bo.KnowledgeAttachBo;
import org.ruoyi.knowledge.domain.bo.KnowledgeInfoBo;
import org.ruoyi.knowledge.domain.req.KnowledgeInfoUploadRequest;
import org.ruoyi.knowledge.domain.vo.KnowledgeInfoVo;

import java.util.Collection;
import java.util.List;

/**
 * 知识库Service接口
 *
 * @author Lion Li
 * @date 2024-10-21
 */
public interface IKnowledgeInfoService {

    /**
     * 查询知识库
     */
    KnowledgeInfoVo queryById(Long id);

    /**
     * 查询知识库列表
     */
    TableDataInfo<KnowledgeInfoVo> queryPageList(KnowledgeInfoBo bo, PageQuery pageQuery);

    /**
     * 查询知识库列表
     */
    List<KnowledgeInfoVo> queryList(KnowledgeInfoBo bo);


    /**
     * 修改知识库
     */
    Boolean updateByBo(KnowledgeInfoBo bo);


    /**
     * 新增知识库
     */
    void saveOne(KnowledgeInfoBo bo);

    /**
     * 上传附件
     */
    void upload(KnowledgeInfoUploadRequest request);

    /**
     * 删除知识库
     */
    void removeKnowledge(String id);

    /**
     * 检查是否有删除权限
     * @param knowledgeInfoList 知识列表
     */
    void check(List<KnowledgeInfoVo> knowledgeInfoList);
}
