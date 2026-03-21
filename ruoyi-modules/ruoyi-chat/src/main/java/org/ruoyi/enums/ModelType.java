package org.ruoyi.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 模型分类枚举
 *
 * @author ageerle@163.com
 * @date 2025-12-30
 */
@Getter
@AllArgsConstructor
public enum ModelType {

    /**
     * 聊天模型
     */
    CHAT(0, "chat", "聊天模型"),

    /**
     * 图片识别模型
     */
    IMAGE(1, "image", "图片识别模型"),

    /**
     * 知识库向量模型
     */
    VECTOR(3, "vector", "知识库向量模型"),

    /**
     * 知识库内容重新排序模型
     */
    RERANKER(4, "reranker", "知识库内容重新排序模型"),

    /**
     * 语音生成模型
     */
    AUDIO(5, "audio", "语音生成模型"),

    /**
     * 语音转文本模型
     */
    TEXT(6, "text", "语音转文本模型"),

    /**
     * 文生视频模型
     */
    VIDEO(7, "video", "文生视频模型"),

    /**
     * 文生PPT模型
     */
    PPT(8, "ppt", "文生PPT模型"),

    /**
     * 文生音乐模型
     */
    MUSIC(9, "music", "文生音乐模型"),
    ;

    /**
     * 编码
     */
    private final Integer code;

    /**
     * 标识
     */
    private final String key;

    /**
     * 描述
     */
    private final String description;

}
