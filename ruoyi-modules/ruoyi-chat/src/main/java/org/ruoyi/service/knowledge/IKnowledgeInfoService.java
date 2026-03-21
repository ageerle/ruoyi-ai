package org.ruoyi.service.knowledge;

import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.domain.bo.knowledge.KnowledgeInfoBo;
import org.ruoyi.domain.vo.knowledge.KnowledgeInfoVo;

import java.util.Collection;
import java.util.List;

/**
 * 知识库Service接口
 *
 * @author ageerle
 * @date 2025-12-17
 */
public interface IKnowledgeInfoService {

    /**
     * 查询知识库
     *
     * @param id 主键
     * @return 知识库
     */
    KnowledgeInfoVo queryById(Long id);

    /**
     * 分页查询知识库列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 知识库分页列表
     */
    TableDataInfo<KnowledgeInfoVo> queryPageList(KnowledgeInfoBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的知识库列表
     *
     * @param bo 查询条件
     * @return 知识库列表
     */
    List<KnowledgeInfoVo> queryList(KnowledgeInfoBo bo);

    /**
     * 新增知识库
     *
     * @param bo 知识库
     * @return 是否新增成功
     */
    Boolean insertByBo(KnowledgeInfoBo bo);

    /**
     * 修改知识库
     *
     * @param bo 知识库
     * @return 是否修改成功
     */
    Boolean updateByBo(KnowledgeInfoBo bo);

    /**
     * 校验并批量删除知识库信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

}
