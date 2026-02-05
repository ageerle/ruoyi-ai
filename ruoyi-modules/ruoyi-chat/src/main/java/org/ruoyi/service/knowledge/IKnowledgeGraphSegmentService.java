package org.ruoyi.service.knowledge;

import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.domain.bo.knowledge.KnowledgeGraphSegmentBo;
import org.ruoyi.domain.vo.knowledge.KnowledgeGraphSegmentVo;

import java.util.Collection;
import java.util.List;

/**
 * 知识图谱片段Service接口
 *
 * @author ageerle
 * @date 2025-12-17
 */
public interface IKnowledgeGraphSegmentService {

    /**
     * 查询知识图谱片段
     *
     * @param id 主键
     * @return 知识图谱片段
     */
    KnowledgeGraphSegmentVo queryById(Long id);

    /**
     * 分页查询知识图谱片段列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 知识图谱片段分页列表
     */
    TableDataInfo<KnowledgeGraphSegmentVo> queryPageList(KnowledgeGraphSegmentBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的知识图谱片段列表
     *
     * @param bo 查询条件
     * @return 知识图谱片段列表
     */
    List<KnowledgeGraphSegmentVo> queryList(KnowledgeGraphSegmentBo bo);

    /**
     * 新增知识图谱片段
     *
     * @param bo 知识图谱片段
     * @return 是否新增成功
     */
    Boolean insertByBo(KnowledgeGraphSegmentBo bo);

    /**
     * 修改知识图谱片段
     *
     * @param bo 知识图谱片段
     * @return 是否修改成功
     */
    Boolean updateByBo(KnowledgeGraphSegmentBo bo);

    /**
     * 校验并批量删除知识图谱片段信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
