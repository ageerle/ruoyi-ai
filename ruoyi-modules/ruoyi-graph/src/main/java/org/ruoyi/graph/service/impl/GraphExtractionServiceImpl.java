package org.ruoyi.graph.service.impl;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.domain.vo.ChatModelVo;
import org.ruoyi.graph.constants.GraphConstants;
import org.ruoyi.graph.dto.ExtractedEntity;
import org.ruoyi.graph.dto.ExtractedRelation;
import org.ruoyi.graph.dto.GraphExtractionResult;
import org.ruoyi.graph.factory.GraphLLMServiceFactory;
import org.ruoyi.graph.prompt.GraphExtractPrompt;
import org.ruoyi.graph.service.IGraphExtractionService;
import org.ruoyi.graph.service.llm.IGraphLLMService;
import org.ruoyi.service.IChatModelService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 图谱实体关系抽取服务实现
 * 使用工厂模式支持多种LLM模型（参考 ruoyi-chat 设计）
 *
 * @author ruoyi
 * @date 2025-09-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "knowledge.graph", name = "enabled", havingValue = "true")
public class GraphExtractionServiceImpl implements IGraphExtractionService {

    /**
     * 实体匹配正则表达式
     * 格式: ("entity"<|>ENTITY_NAME<|>ENTITY_TYPE<|>ENTITY_DESCRIPTION)
     */
    private static final Pattern ENTITY_PATTERN = Pattern.compile(
            "\\(\"entity\"" +
                    Pattern.quote(GraphConstants.GRAPH_TUPLE_DELIMITER) +
                    "([^" + Pattern.quote(GraphConstants.GRAPH_TUPLE_DELIMITER) + "]+)" +
                    Pattern.quote(GraphConstants.GRAPH_TUPLE_DELIMITER) +
                    "([^" + Pattern.quote(GraphConstants.GRAPH_TUPLE_DELIMITER) + "]+)" +
                    Pattern.quote(GraphConstants.GRAPH_TUPLE_DELIMITER) +
                    "([^)]+)\\)"
    );
    /**
     * 关系匹配正则表达式
     * 格式: ("relationship"<|>SOURCE<|>TARGET<|>DESCRIPTION<|>STRENGTH)
     */
    private static final Pattern RELATION_PATTERN = Pattern.compile(
            "\\(\"relationship\"" +
                    Pattern.quote(GraphConstants.GRAPH_TUPLE_DELIMITER) +
                    "([^" + Pattern.quote(GraphConstants.GRAPH_TUPLE_DELIMITER) + "]+)" +
                    Pattern.quote(GraphConstants.GRAPH_TUPLE_DELIMITER) +
                    "([^" + Pattern.quote(GraphConstants.GRAPH_TUPLE_DELIMITER) + "]+)" +
                    Pattern.quote(GraphConstants.GRAPH_TUPLE_DELIMITER) +
                    "([^" + Pattern.quote(GraphConstants.GRAPH_TUPLE_DELIMITER) + "]+)" +
                    Pattern.quote(GraphConstants.GRAPH_TUPLE_DELIMITER) +
                    "([^)]+)\\)"
    );
    private final IChatModelService chatModelService;
    private final GraphLLMServiceFactory llmServiceFactory;

    @Override
    public GraphExtractionResult extractFromText(String text) {
        return extractFromText(text, GraphConstants.DEFAULT_ENTITY_TYPES);
    }

    @Override
    public GraphExtractionResult extractFromText(String text, String[] entityTypes) {
        log.info("开始从文本中抽取实体和关系，文本长度: {}", text.length());

        try {
            // 1. 构建提示词
            String prompt = GraphExtractPrompt.buildExtractionPrompt(text, entityTypes);

            // 2. 调用LLM（使用默认模型）
            String llmResponse = callLLM(prompt);

            // 3. 解析响应
            GraphExtractionResult result = parseGraphResponse(llmResponse);
            result.setRawResponse(llmResponse);
            result.setSuccess(true);

            log.info("抽取完成，实体数: {}, 关系数: {}",
                    result.getEntities().size(), result.getRelations().size());

            return result;

        } catch (Exception e) {
            log.error("实体关系抽取失败", e);
            return GraphExtractionResult.builder()
                    .entities(new ArrayList<>())
                    .relations(new ArrayList<>())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public GraphExtractionResult extractFromTextWithModel(String text, String modelName) {
        log.info("开始从文本中抽取实体和关系，使用模型: {}, 文本长度: {}", modelName, text.length());

        try {
            // 1. 获取模型配置
            ChatModelVo chatModel = chatModelService.selectModelByName(modelName);
            if (chatModel == null) {
                log.warn("未找到模型: {}, 使用默认模型", modelName);
                return extractFromText(text);
            }

            // 2. 构建提示词
            String prompt = GraphExtractPrompt.buildExtractionPrompt(text, GraphConstants.DEFAULT_ENTITY_TYPES);

            // 3. 调用LLM（使用指定模型）
            String llmResponse = callLLMWithModel(prompt, chatModel);

            // 4. 解析响应
            GraphExtractionResult result = parseGraphResponse(llmResponse);
            result.setRawResponse(llmResponse);
            result.setSuccess(true);

            log.info("抽取完成，实体数: {}, 关系数: {}, 使用模型: {}",
                    result.getEntities().size(), result.getRelations().size(), modelName);

            // ⭐ 调试：如果没有关系，记录原始响应（便于诊断）
            if (result.getRelations().isEmpty() && !result.getEntities().isEmpty()) {
                log.warn("⚠️ LLM 提取到 {} 个实体，但没有提取到任何关系！", result.getEntities().size());
                log.warn("LLM 原始响应预览（前500字符）: {}",
                        llmResponse.length() > 500 ? llmResponse.substring(0, 500) + "..." : llmResponse);
            }

            return result;

        } catch (Exception e) {
            log.error("实体关系抽取失败，模型: {}", modelName, e);
            return GraphExtractionResult.builder()
                    .entities(new ArrayList<>())
                    .relations(new ArrayList<>())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public GraphExtractionResult parseGraphResponse(String response) {
        log.debug("开始解析图谱响应，响应长度: {}", response != null ? response.length() : 0);

        List<ExtractedEntity> entities = new ArrayList<>();
        List<ExtractedRelation> relations = new ArrayList<>();

        if (StrUtil.isBlank(response)) {
            log.warn("响应为空，无法解析");
            return GraphExtractionResult.builder()
                    .entities(entities)
                    .relations(relations)
                    .success(false)
                    .errorMessage("LLM响应为空")
                    .build();
        }

        try {
            // 1. 解析实体
            Matcher entityMatcher = ENTITY_PATTERN.matcher(response);
            while (entityMatcher.find()) {
                String name = entityMatcher.group(1).trim();
                String type = entityMatcher.group(2).trim();
                String description = entityMatcher.group(3).trim();

                // ⭐ 过滤无效实体（N/A 或包含特殊字符）
                if (isInvalidEntity(name, type)) {
                    log.debug("跳过无效实体: name={}, type={}", name, type);
                    continue;
                }

                ExtractedEntity entity = ExtractedEntity.builder()
                        .name(name)
                        .type(type)
                        .description(description)
                        .build();

                entities.add(entity);
                log.debug("解析到实体: name={}, type={}", name, type);
            }

            // 2. 解析关系
            Matcher relationMatcher = RELATION_PATTERN.matcher(response);
            while (relationMatcher.find()) {
                String sourceEntity = relationMatcher.group(1).trim();
                String targetEntity = relationMatcher.group(2).trim();
                String description = relationMatcher.group(3).trim();
                String strengthStr = relationMatcher.group(4).trim();

                Integer strength = parseStrength(strengthStr);
                Double confidence = calculateConfidence(strength);

                ExtractedRelation relation = ExtractedRelation.builder()
                        .sourceEntity(sourceEntity)
                        .targetEntity(targetEntity)
                        .description(description)
                        .strength(strength)
                        .confidence(confidence)
                        .build();

                relations.add(relation);
                log.debug("解析到关系: sourceEntity={}, targetEntity={}, strength={}",
                        sourceEntity, targetEntity, strength);
            }

            log.info("解析完成，实体数: {}, 关系数: {}", entities.size(), relations.size());

            return GraphExtractionResult.builder()
                    .entities(entities)
                    .relations(relations)
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("解析图谱响应失败", e);
            return GraphExtractionResult.builder()
                    .entities(entities)
                    .relations(relations)
                    .success(false)
                    .errorMessage("解析失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 调用LLM获取响应（使用默认模型）
     *
     * @param prompt 提示词
     * @return LLM响应
     */
    private String callLLM(String prompt) {
        // 获取聊天分类的最高优先级模型作为默认模型
        ChatModelVo defaultModel = chatModelService.selectModelByCategoryWithHighestPriority("chat");
        if (defaultModel == null) {
            // 如果没有chat分类的模型，尝试查询任意可用模型
            List<ChatModelVo> models = chatModelService.queryList(new org.ruoyi.domain.bo.ChatModelBo());
            if (models != null && !models.isEmpty()) {
                defaultModel = models.get(0);
            }
        }

        if (defaultModel == null) {
            log.error("未找到可用的LLM模型");
            throw new RuntimeException("未找到可用的LLM模型，请先配置聊天模型");
        }

        log.info("使用默认模型: {}", defaultModel.getModelName());
        return callLLMWithModel(prompt, defaultModel);
    }

    /**
     * 使用指定模型调用LLM获取响应（使用工厂模式，支持多种LLM）
     *
     * @param prompt    提示词
     * @param chatModel 模型配置
     * @return LLM响应
     */
    private String callLLMWithModel(String prompt, ChatModelVo chatModel) {
        log.info("调用LLM模型: model={}, category={}, 提示词长度={}",
                chatModel.getModelName(), chatModel.getCategory(), prompt.length());

        try {
            // 根据模型类别获取对应的LLM服务实现
            IGraphLLMService llmService = llmServiceFactory.getLLMService(chatModel.getCategory());

            // 调用LLM进行图谱抽取
            String responseText = llmService.extractGraph(prompt, chatModel);

            log.info("LLM调用成功: model={}, category={}, 响应长度={}",
                    chatModel.getModelName(), chatModel.getCategory(), responseText.length());

            return responseText;

        } catch (IllegalArgumentException e) {
            // 不支持的模型类别，降级到默认实现
            log.warn("不支持的模型类别: {}, 尝试使用OpenAI兼容模式", chatModel.getCategory());

            try {
                IGraphLLMService openAiService = llmServiceFactory.getLLMService("openai");
                return openAiService.extractGraph(prompt, chatModel);
            } catch (Exception fallbackEx) {
                log.error("降级调用也失败: {}", fallbackEx.getMessage(), fallbackEx);
                throw new RuntimeException("LLM调用失败: " + fallbackEx.getMessage(), fallbackEx);
            }

        } catch (Exception e) {
            log.error("LLM调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("LLM调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析关系强度
     *
     * @param strengthStr 强度字符串
     * @return 强度值（0-10）
     */
    private Integer parseStrength(String strengthStr) {
        try {
            // 尝试解析为整数
            int strength = Integer.parseInt(strengthStr);
            // 限制在0-10范围内
            return Math.max(0, Math.min(10, strength));
        } catch (NumberFormatException e) {
            log.debug("无法解析关系强度: {}, 使用默认值5", strengthStr);
            return 5; // 默认中等强度
        }
    }

    /**
     * 验证实体是否有效
     * 过滤 N/A 以及包含 Neo4j 不支持的特殊字符的实体
     *
     * @param name 实体名称
     * @param type 实体类型
     * @return true=无效，false=有效
     */
    private boolean isInvalidEntity(String name, String type) {
        // 1. 检查是否为 N/A
        if ("N/A".equalsIgnoreCase(name) || "N/A".equalsIgnoreCase(type)) {
            return true;
        }

        // 2. 检查是否为空或纯空格
        if (StrUtil.isBlank(name) || StrUtil.isBlank(type)) {
            return true;
        }

        // 3. 检查类型是否包含 Neo4j Label 不支持的字符
        // Neo4j Label 规则：不能包含 / : & | 等特殊字符
        if (type.matches(".*[/:&|\\\\].*")) {
            log.warn("⚠️ 实体类型包含非法字符，将被过滤: type={}", type);
            return true;
        }

        // 4. 检查名称是否过长（Neo4j 建议 < 256）
        if (name.length() > 255 || type.length() > 64) {
            log.warn("⚠️ 实体名称或类型过长，将被过滤: name.length={}, type.length={}",
                    name.length(), type.length());
            return true;
        }

        return false;
    }

    /**
     * 根据关系强度计算置信度
     *
     * @param strength 关系强度（0-10）
     * @return 置信度（0.0-1.0）
     */
    private Double calculateConfidence(Integer strength) {
        if (strength == null) {
            return 0.5;
        }
        // 将0-10的强度映射到0.3-1.0的置信度
        return 0.3 + (strength / 10.0) * 0.7;
    }
}
