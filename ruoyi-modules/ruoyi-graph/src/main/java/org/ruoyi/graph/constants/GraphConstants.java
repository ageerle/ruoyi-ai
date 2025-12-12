package org.ruoyi.graph.constants;

/**
 * 知识图谱常量
 *
 * @author ruoyi
 * @date 2025-09-30
 */
public class GraphConstants {

    /**
     * 图谱记录分隔符
     */
    public static final String GRAPH_RECORD_DELIMITER = "##";

    /**
     * 图谱元组分隔符
     */
    public static final String GRAPH_TUPLE_DELIMITER = "<|>";

    /**
     * 图谱完成标记
     */
    public static final String GRAPH_COMPLETION_DELIMITER = "<|COMPLETE|>";

    /**
     * 实体类型：人物
     */
    public static final String ENTITY_TYPE_PERSON = "PERSON";

    /**
     * 实体类型：组织机构
     */
    public static final String ENTITY_TYPE_ORGANIZATION = "ORGANIZATION";

    /**
     * 实体类型：地点
     */
    public static final String ENTITY_TYPE_LOCATION = "LOCATION";

    /**
     * 实体类型：概念
     */
    public static final String ENTITY_TYPE_CONCEPT = "CONCEPT";

    /**
     * 实体类型：事件
     */
    public static final String ENTITY_TYPE_EVENT = "EVENT";

    /**
     * 实体类型：产品
     */
    public static final String ENTITY_TYPE_PRODUCT = "PRODUCT";

    /**
     * 实体类型：技术
     */
    public static final String ENTITY_TYPE_TECHNOLOGY = "TECHNOLOGY";

    /**
     * 默认实体抽取类型列表
     */
    public static final String[] DEFAULT_ENTITY_TYPES = {
            ENTITY_TYPE_PERSON,
            ENTITY_TYPE_ORGANIZATION,
            ENTITY_TYPE_LOCATION,
            ENTITY_TYPE_CONCEPT,
            ENTITY_TYPE_EVENT,
            ENTITY_TYPE_PRODUCT,
            ENTITY_TYPE_TECHNOLOGY
    };

    /**
     * 元数据键：知识库UUID
     */
    public static final String METADATA_KB_UUID = "kb_uuid";

    /**
     * 元数据键：知识库条目UUID
     */
    public static final String METADATA_KB_ITEM_UUID = "kb_item_uuid";

    /**
     * 元数据键：文档UUID
     */
    public static final String METADATA_DOC_UUID = "doc_uuid";

    /**
     * 元数据键：片段UUID
     */
    public static final String METADATA_SEGMENT_UUID = "segment_uuid";

    /**
     * RAG最大片段大小（token数）
     */
    public static final int RAG_MAX_SEGMENT_SIZE_IN_TOKENS = 512;

    /**
     * RAG片段重叠大小（token数）
     */
    public static final int RAG_SEGMENT_OVERLAP_IN_TOKENS = 50;
}
