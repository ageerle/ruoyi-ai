package org.ruoyi.service.knowledge;

import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.domain.bo.knowledge.KnowledgeFragmentBo;
import org.ruoyi.domain.vo.knowledge.KnowledgeFragmentVo;
import org.ruoyi.domain.vo.knowledge.KnowledgeRetrievalVo;

import java.util.Collection;
import java.util.List;

/**
 * 知识片段Service接口
 *
 * @author ageerle
 * @date 2025-12-17
 */
public interface IKnowledgeFragmentService {

    /**
     * 查询知识片段
     *
     * @param id 主键
     * @return 知识片段
     */
    KnowledgeFragmentVo queryById(Long id);

    /**
     * 分页查询知识片段列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 知识片段分页列表
     */
    TableDataInfo<KnowledgeFragmentVo> queryPageList(KnowledgeFragmentBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的知识片段列表
     *
     * @param bo 查询条件
     * @return 知识片段列表
     */
    List<KnowledgeFragmentVo> queryList(KnowledgeFragmentBo bo);

    /**
     * 新增知识片段
     *
     * @param bo 知识片段
     * @return 是否新增成功
     */
    Boolean insertByBo(KnowledgeFragmentBo bo);

    /**
     * 修改知识片段
     *
     * @param bo 知识片段
     * @return 是否修改成功
     */
    Boolean updateByBo(KnowledgeFragmentBo bo);

    /**
     * 校验并批量删除知识片段信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 检索测试
     *
     * @param bo 检索参数
     * @return 检索结果
     */
    List<KnowledgeRetrievalVo> retrieval(KnowledgeFragmentBo bo);
}
