package org.ruoyi.service.agent.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import dev.langchain4j.skills.FileSystemSkill;
import dev.langchain4j.skills.FileSystemSkillLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.json.utils.JsonUtils;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.config.agent.SkillsPathResolver;
import org.ruoyi.domain.entity.agent.Agent;
import org.ruoyi.domain.bo.agent.AgentBo;
import org.ruoyi.domain.entity.mcp.McpTool;
import org.ruoyi.domain.vo.agent.AgentVo;
import org.ruoyi.domain.vo.agent.SkillOptionVo;
import org.ruoyi.domain.vo.knowledge.KnowledgeInfoVo;
import org.ruoyi.mapper.agent.AgentMapper;
import org.ruoyi.mapper.mcp.McpToolMapper;
import org.ruoyi.service.agent.IAgentService;
import org.ruoyi.service.knowledge.IKnowledgeInfoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 智能体服务实现
 * <p>
 * 注意：entity 的 mcpToolIds/skillNames/knowledgeIds 是 JSON 字符串列，
 * VO 中是 List 类型。MapStruct 无法自动 String→List，因此这里查询 entity 后
 * 手动用 JsonUtils 解析并组装 VO。
 *
 * @author ruoyi team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements IAgentService {

    private final AgentMapper baseMapper;
    private final McpToolMapper mcpToolMapper;
    private final IKnowledgeInfoService knowledgeInfoService;
    private final IChatModelService chatModelService;

    @Override
    public TableDataInfo<AgentVo> queryPageList(AgentBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<Agent> wrapper = buildQueryWrapper(bo);
        Page<Agent> page = baseMapper.selectPage(pageQuery.build(), wrapper);
        List<AgentVo> records = page.getRecords() == null ? List.of()
            : page.getRecords().stream().map(this::toVo).toList();
        Page<AgentVo> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return TableDataInfo.build(voPage);
    }

    @Override
    public List<AgentVo> queryList(AgentBo bo) {
        LambdaQueryWrapper<Agent> wrapper = buildQueryWrapper(bo);
        List<Agent> list = baseMapper.selectList(wrapper);
        return list.stream().map(this::toVo).toList();
    }

    @Override
    public AgentVo queryById(Long id) {
        Agent entity = baseMapper.selectById(id);
        return entity == null ? null : toVo(entity);
    }

    @Override
    @Transactional
    public Boolean insertByBo(AgentBo bo) {
        Agent entity = MapstructUtils.convert(bo, Agent.class);
        if (!StringUtils.hasText(entity.getStatus())) {
            entity.setStatus("0");
        }
        if (!StringUtils.hasText(entity.getEnableThinking())) {
            entity.setEnableThinking("0");
        }
        entity.setMcpToolIds(JsonUtils.toJsonString(bo.getMcpToolIds()));
        entity.setSkillNames(JsonUtils.toJsonString(bo.getSkillNames()));
        entity.setKnowledgeIds(JsonUtils.toJsonString(bo.getKnowledgeIds()));
        return baseMapper.insert(entity) > 0;
    }

    @Override
    @Transactional
    public Boolean updateByBo(AgentBo bo) {
        Agent entity = MapstructUtils.convert(bo, Agent.class);
        entity.setMcpToolIds(JsonUtils.toJsonString(bo.getMcpToolIds()));
        entity.setSkillNames(JsonUtils.toJsonString(bo.getSkillNames()));
        entity.setKnowledgeIds(JsonUtils.toJsonString(bo.getKnowledgeIds()));
        return baseMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional
    public Boolean deleteByIds(Collection<Long> ids) {
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public List<AgentVo> queryEnabledOptions() {
        LambdaQueryWrapper<Agent> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Agent::getStatus, "0")
            .orderByDesc(Agent::getUpdateTime)
            .orderByDesc(Agent::getId);
        return baseMapper.selectList(wrapper).stream().map(this::toVo).toList();
    }

    @Override
    public List<SkillOptionVo> listSkillOptions() {
        try {
            List<FileSystemSkill> skills = FileSystemSkillLoader.loadSkills(SkillsPathResolver.resolveSkillsPath());
            if (skills == null || skills.isEmpty()) {
                return Collections.emptyList();
            }
            return skills.stream()
                .map(s -> new SkillOptionVo(s.name(), s.description()))
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("加载磁盘 Skills 失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * entity → vo，展开 JSON 数组并填充关联名称与模型名
     */
    private AgentVo toVo(Agent entity) {
        if (entity == null) {
            return null;
        }
        AgentVo vo = new AgentVo();
        vo.setId(entity.getId());
        vo.setAgentName(entity.getAgentName());
        vo.setAgentDescribe(entity.getAgentDescribe());
        vo.setAgentShow(entity.getAgentShow());
        vo.setModelId(entity.getModelId());
        vo.setEnableThinking(entity.getEnableThinking());
        vo.setSystemPrompt(entity.getSystemPrompt());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());

        // 展开 JSON 数组
        List<Long> toolIds = parseLongArray(entity.getMcpToolIds());
        List<String> skillNames = parseStringArray(entity.getSkillNames());
        List<Long> knowledgeIds = parseLongArray(entity.getKnowledgeIds());
        vo.setMcpToolIds(toolIds);
        vo.setSkillNames(skillNames);
        vo.setKnowledgeIds(knowledgeIds);

        // 绑定模型名称
        if (entity.getModelId() != null) {
            try {
                ChatModelVo model = chatModelService.queryById(entity.getModelId());
                if (model != null) {
                    vo.setModelName(model.getModelName());
                }
            } catch (Exception e) {
                log.warn("查询模型失败: modelId={}, err={}", entity.getModelId(), e.getMessage());
            }
        }
        // MCP 工具名称
        if (toolIds != null && !toolIds.isEmpty()) {
            List<McpTool> tools = mcpToolMapper.selectByIds(toolIds);
            if (tools != null) {
                vo.setMcpToolNames(tools.stream().map(McpTool::getName).toList());
            }
        }
        // 知识库名称
        if (knowledgeIds != null && !knowledgeIds.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (Long kid : knowledgeIds) {
                try {
                    KnowledgeInfoVo kb = knowledgeInfoService.queryById(kid);
                    if (kb != null) {
                        names.add(kb.getName());
                    }
                } catch (Exception e) {
                    log.warn("查询知识库失败: kid={}, err={}", kid, e.getMessage());
                }
            }
            vo.setKnowledgeNames(names);
        }
        return vo;
    }

    private List<Long> parseLongArray(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        List<Long> list = JsonUtils.parseArray(json, Long.class);
        return list == null ? new ArrayList<>() : list;
    }

    private List<String> parseStringArray(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        List<String> list = JsonUtils.parseArray(json, String.class);
        return list == null ? new ArrayList<>() : list;
    }

    private LambdaQueryWrapper<Agent> buildQueryWrapper(AgentBo bo) {
        LambdaQueryWrapper<Agent> wrapper = Wrappers.lambdaQuery();
        wrapper.like(StringUtils.hasText(bo.getAgentName()), Agent::getAgentName, bo.getAgentName())
            .like(StringUtils.hasText(bo.getAgentDescribe()), Agent::getAgentDescribe, bo.getAgentDescribe())
            .eq(StringUtils.hasText(bo.getStatus()), Agent::getStatus, bo.getStatus())
            .eq(bo.getModelId() != null, Agent::getModelId, bo.getModelId())
            .orderByDesc(Agent::getUpdateTime);
        return wrapper;
    }
}
