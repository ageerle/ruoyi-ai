package org.ruoyi.service.knowledge;

import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.domain.bo.knowledge.KnowledgeGraphInstanceBo;
import org.ruoyi.domain.vo.knowledge.KnowledgeGraphInstanceVo;

import java.util.Collection;
import java.util.List;

/**
 * 知识图谱实例Service接口
 *
 * @author ageerle
 * @date 2025-12-17
 */
public interface IKnowledgeGraphInstanceService {

    /**
     * 查询知识图谱实例
     *
     * @param id 主键
     * @return 知识图谱实例
     */
    KnowledgeGraphInstanceVo queryById(Long id);

    /**
     * 分页查询知识图谱实例列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 知识图谱实例分页列表
     */
    TableDataInfo<KnowledgeGraphInstanceVo> queryPageList(KnowledgeGraphInstanceBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的知识图谱实例列表
     *
     * @param bo 查询条件
     * @return 知识图谱实例列表
     */
    List<KnowledgeGraphInstanceVo> queryList(KnowledgeGraphInstanceBo bo);

    /**
     * 新增知识图谱实例
     *
     * @param bo 知识图谱实例
     * @return 是否新增成功
     */
    Boolean insertByBo(KnowledgeGraphInstanceBo bo);

    /**
     * 修改知识图谱实例
     *
     * @param bo 知识图谱实例
     * @return 是否修改成功
     */
    Boolean updateByBo(KnowledgeGraphInstanceBo bo);

    /**
     * 校验并批量删除知识图谱实例信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
