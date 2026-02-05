package org.ruoyi.workflow.service;

import jakarta.servlet.http.HttpServletResponse;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.dromara.warm.flow.orm.entity.FlowDefinition;
import org.ruoyi.workflow.domain.vo.FlowDefinitionVo;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 流程定义 服务层
 *
 * @author may
 */
public interface IFlwDefinitionService {

    /**
     * 查询流程定义列表
     *
     * @param flowDefinition 参数
     * @param pageQuery      分页
     * @return 返回分页列表
     */
    TableDataInfo<FlowDefinitionVo> queryList(FlowDefinition flowDefinition, PageQuery pageQuery);

    /**
     * 查询未发布的流程定义列表
     *
     * @param flowDefinition 参数
     * @param pageQuery      分页
     * @return 返回分页列表
     */
    TableDataInfo<FlowDefinitionVo> unPublishList(FlowDefinition flowDefinition, PageQuery pageQuery);

    /**
     * 发布流程定义
     *
     * @param id 流程定义id
     * @return 结果
     */
    boolean publish(Long id);

    /**
     * 导出流程定义
     *
     * @param id       流程定义id
     * @param response 响应
     * @throws IOException 异常
     */
    void exportDef(Long id, HttpServletResponse response) throws IOException;

    /**
     * 导入流程定义
     *
     * @param file     文件
     * @param category 分类
     * @return 结果
     */
    boolean importJson(MultipartFile file, String category);

    /**
     * 删除流程定义
     *
     * @param ids 流程定义id
     * @return 结果
     */
    boolean removeDef(List<Long> ids);

    /**
     * 新增租户流程定义
     *
     * @param tenantId 租户id
     */
    void syncDef(String tenantId);
}
