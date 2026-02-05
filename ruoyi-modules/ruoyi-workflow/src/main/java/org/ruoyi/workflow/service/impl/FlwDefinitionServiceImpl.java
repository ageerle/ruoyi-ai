package org.ruoyi.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.IoUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.utils.StreamUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.json.utils.JsonUtils;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.dromara.warm.flow.core.dto.DefJson;
import org.dromara.warm.flow.core.enums.NodeType;
import org.dromara.warm.flow.core.enums.PublishStatus;
import org.dromara.warm.flow.core.service.DefService;
import org.dromara.warm.flow.orm.entity.FlowDefinition;
import org.dromara.warm.flow.orm.entity.FlowHisTask;
import org.dromara.warm.flow.orm.entity.FlowNode;
import org.dromara.warm.flow.orm.entity.FlowSkip;
import org.dromara.warm.flow.orm.mapper.FlowDefinitionMapper;
import org.dromara.warm.flow.orm.mapper.FlowHisTaskMapper;
import org.dromara.warm.flow.orm.mapper.FlowNodeMapper;
import org.dromara.warm.flow.orm.mapper.FlowSkipMapper;
import org.ruoyi.workflow.common.ConditionalOnEnable;
import org.ruoyi.workflow.common.constant.FlowConstant;
import org.ruoyi.workflow.domain.FlowCategory;
import org.ruoyi.workflow.domain.vo.FlowDefinitionVo;
import org.ruoyi.workflow.mapper.FlwCategoryMapper;
import org.ruoyi.workflow.service.IFlwCommonService;
import org.ruoyi.workflow.service.IFlwDefinitionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.ruoyi.common.core.constant.TenantConstants.DEFAULT_TENANT_ID;

/**
 * 流程定义 服务层实现
 *
 * @author may
 */
@ConditionalOnEnable
@Slf4j
@RequiredArgsConstructor
@Service
public class FlwDefinitionServiceImpl implements IFlwDefinitionService {

    private final DefService defService;
    private final FlowDefinitionMapper flowDefinitionMapper;
    private final FlowHisTaskMapper flowHisTaskMapper;
    private final FlowNodeMapper flowNodeMapper;
    private final FlowSkipMapper flowSkipMapper;
    private final FlwCategoryMapper flwCategoryMapper;
    private final IFlwCommonService flwCommonService;

    /**
     * 查询流程定义列表
     *
     * @param flowDefinition 流程定义信息
     * @param pageQuery      分页
     * @return 返回分页列表
     */
    @Override
    public TableDataInfo<FlowDefinitionVo> queryList(FlowDefinition flowDefinition, PageQuery pageQuery) {
        LambdaQueryWrapper<FlowDefinition> wrapper = buildQueryWrapper(flowDefinition);
        wrapper.eq(FlowDefinition::getIsPublish, PublishStatus.PUBLISHED.getKey());
        Page<FlowDefinition> page = flowDefinitionMapper.selectPage(pageQuery.build(), wrapper);
        List<FlowDefinitionVo> list = BeanUtil.copyToList(page.getRecords(), FlowDefinitionVo.class);
        return new TableDataInfo<>(list, page.getTotal());
    }

    /**
     * 查询未发布的流程定义列表
     *
     * @param flowDefinition 流程定义信息
     * @param pageQuery      分页
     * @return 返回分页列表
     */
    @Override
    public TableDataInfo<FlowDefinitionVo> unPublishList(FlowDefinition flowDefinition, PageQuery pageQuery) {
        LambdaQueryWrapper<FlowDefinition> wrapper = buildQueryWrapper(flowDefinition);
        wrapper.in(FlowDefinition::getIsPublish, Arrays.asList(PublishStatus.UNPUBLISHED.getKey(), PublishStatus.EXPIRED.getKey()));
        Page<FlowDefinition> page = flowDefinitionMapper.selectPage(pageQuery.build(), wrapper);
        List<FlowDefinitionVo> list = BeanUtil.copyToList(page.getRecords(), FlowDefinitionVo.class);
        return new TableDataInfo<>(list, page.getTotal());
    }

    private LambdaQueryWrapper<FlowDefinition> buildQueryWrapper(FlowDefinition flowDefinition) {
        LambdaQueryWrapper<FlowDefinition> wrapper = Wrappers.lambdaQuery();
        wrapper.like(StringUtils.isNotBlank(flowDefinition.getFlowCode()), FlowDefinition::getFlowCode, flowDefinition.getFlowCode());
        wrapper.like(StringUtils.isNotBlank(flowDefinition.getFlowName()), FlowDefinition::getFlowName, flowDefinition.getFlowName());
        if (StringUtils.isNotBlank(flowDefinition.getCategory())) {
            List<Long> categoryIds = flwCategoryMapper.selectCategoryIdsByParentId(Convert.toLong(flowDefinition.getCategory()));
            wrapper.in(FlowDefinition::getCategory, StreamUtils.toList(categoryIds, Convert::toStr));
        }
        wrapper.orderByDesc(FlowDefinition::getCreateTime);
        return wrapper;
    }

    /**
     * 发布流程定义
     *
     * @param id 流程定义id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean publish(Long id) {
        List<FlowNode> flowNodes = flowNodeMapper.selectList(new LambdaQueryWrapper<FlowNode>().eq(FlowNode::getDefinitionId, id));
        List<String> errorMsg = new ArrayList<>();
        if (CollUtil.isNotEmpty(flowNodes)) {
            String applyNodeCode = flwCommonService.applyNodeCode(id);
            for (FlowNode flowNode : flowNodes) {
                if (StringUtils.isBlank(flowNode.getPermissionFlag()) && !applyNodeCode.equals(flowNode.getNodeCode()) && NodeType.BETWEEN.getKey().equals(flowNode.getNodeType())) {
                    errorMsg.add(flowNode.getNodeName());
                }
            }
            if (CollUtil.isNotEmpty(errorMsg)) {
                throw new ServiceException("节点【{}】未配置办理人!", StringUtils.joinComma(errorMsg));
            }
        }
        return defService.publish(id);
    }

    /**
     * 导入流程定义
     *
     * @param file 文件
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean importJson(MultipartFile file, String category) {
        try {
            DefJson defJson = JsonUtils.parseObject(file.getBytes(), DefJson.class);
            defJson.setCategory(category);
            defService.importDef(defJson);
        } catch (IOException e) {
            log.error("读取文件流错误: {}", e.getMessage(), e);
            throw new IllegalStateException("文件读取失败，请检查文件内容", e);
        }
        return true;
    }

    /**
     * 导出流程定义
     *
     * @param id       流程定义id
     * @param response 响应
     * @throws IOException 异常
     */
    @Override
    public void exportDef(Long id, HttpServletResponse response) throws IOException {
        byte[] data = defService.exportJson(id).getBytes(StandardCharsets.UTF_8);
        // 设置响应头和内容类型
        response.reset();
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/text");
        response.setHeader("Content-Disposition", "attachment;");
        response.addHeader("Content-Length", "" + data.length);
        IoUtil.write(response.getOutputStream(), false, data);
    }

    /**
     * 删除流程定义
     *
     * @param ids 流程定义id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeDef(List<Long> ids) {
        LambdaQueryWrapper<FlowHisTask> wrapper = Wrappers.lambdaQuery();
        wrapper.in(FlowHisTask::getDefinitionId, ids);
        List<FlowHisTask> flowHisTasks = flowHisTaskMapper.selectList(wrapper);
        if (CollUtil.isNotEmpty(flowHisTasks)) {
            List<FlowDefinition> flowDefinitions = flowDefinitionMapper.selectByIds(StreamUtils.toList(flowHisTasks, FlowHisTask::getDefinitionId));
            if (CollUtil.isNotEmpty(flowDefinitions)) {
                String join = StreamUtils.join(flowDefinitions, FlowDefinition::getFlowCode);
                log.info("流程定义【{}】已被使用不可被删除！", join);
                throw new ServiceException("流程定义【{}】已被使用不可被删除！", join);
            }
        }
        try {
            defService.removeDef(ids);
        } catch (Exception e) {
            log.error("Error removing flow definitions: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to remove flow definitions", e);
        }
        return true;
    }

    /**
     * 新增租户流程定义
     *
     * @param tenantId 租户id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncDef(String tenantId) {
        List<FlowDefinition> flowDefinitions = flowDefinitionMapper.selectList(new LambdaQueryWrapper<FlowDefinition>().eq(FlowDefinition::getTenantId, DEFAULT_TENANT_ID));
        if (CollUtil.isEmpty(flowDefinitions)) {
            return;
        }
        FlowCategory flowCategory = flwCategoryMapper.selectOne(new LambdaQueryWrapper<FlowCategory>()
            .eq(FlowCategory::getTenantId, DEFAULT_TENANT_ID)
            .eq(FlowCategory::getCategoryId, FlowConstant.FLOW_CATEGORY_ID));
        flowCategory.setCategoryId(null);
        flowCategory.setTenantId(tenantId);
        flowCategory.setCreateDept(null);
        flowCategory.setCreateBy(null);
        flowCategory.setCreateTime(null);
        flowCategory.setUpdateBy(null);
        flowCategory.setUpdateTime(null);
        flwCategoryMapper.insert(flowCategory);
        List<Long> defIds = StreamUtils.toList(flowDefinitions, FlowDefinition::getId);
        List<FlowNode> flowNodes = flowNodeMapper.selectList(new LambdaQueryWrapper<FlowNode>().in(FlowNode::getDefinitionId, defIds));
        List<FlowSkip> flowSkips = flowSkipMapper.selectList(new LambdaQueryWrapper<FlowSkip>().in(FlowSkip::getDefinitionId, defIds));
        for (FlowDefinition definition : flowDefinitions) {
            FlowDefinition flowDefinition = BeanUtil.toBean(definition, FlowDefinition.class);
            flowDefinition.setId(null);
            flowDefinition.setTenantId(tenantId);
            flowDefinition.setIsPublish(0);
            flowDefinition.setCategory(Convert.toStr(flowCategory.getCategoryId()));
            int insert = flowDefinitionMapper.insert(flowDefinition);
            if (insert <= 0) {
                log.info("同步流程定义【{}】失败！", definition.getFlowCode());
                continue;
            }
            log.info("同步流程定义【{}】成功！", definition.getFlowCode());
            Long definitionId = flowDefinition.getId();
            if (CollUtil.isNotEmpty(flowNodes)) {
                List<FlowNode> nodes = StreamUtils.filter(flowNodes, node -> node.getDefinitionId().equals(definition.getId()));
                if (CollUtil.isNotEmpty(nodes)) {
                    List<FlowNode> flowNodeList = BeanUtil.copyToList(nodes, FlowNode.class);
                    flowNodeList.forEach(e -> {
                        e.setId(null);
                        e.setDefinitionId(definitionId);
                        e.setTenantId(tenantId);
                        e.setPermissionFlag(null);
                    });
                    flowNodeMapper.insertOrUpdate(flowNodeList);
                }
            }
            if (CollUtil.isNotEmpty(flowSkips)) {
                List<FlowSkip> skips = StreamUtils.filter(flowSkips, skip -> skip.getDefinitionId().equals(definition.getId()));
                if (CollUtil.isNotEmpty(skips)) {
                    List<FlowSkip> flowSkipList = BeanUtil.copyToList(skips, FlowSkip.class);
                    flowSkipList.forEach(e -> {
                        e.setId(null);
                        e.setDefinitionId(definitionId);
                        e.setTenantId(tenantId);
                    });
                    flowSkipMapper.insertOrUpdate(flowSkipList);
                }
            }
        }
    }
}
