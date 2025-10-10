package org.ruoyi.embedding.model;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Builder;
import lombok.Data;

/**
 * @Author: Robust_H
 * @Date: 2025-09-30-下午2:13
 * @Description: 多模态输入
 */
@Data
@Builder
public class MultiModalInput {
    private String text;
    private byte[] imageData;
    private byte[] videoData;
    private String imageMimeType;
    private String videoMimeType;
    private String[] multiImageUrls;
    private String imageUrl;
    private String videoUrl;

    /**
     * 检查是否有文本内容
     */
    public boolean hasText() {
        return StrUtil.isNotBlank(text);
    }

    /**
     * 检查是否有图片内容
     */
    public boolean hasImage() {
        return ArrayUtil.isNotEmpty(imageData) || StrUtil.isNotBlank(imageUrl);
    }

    /**
     * 检查是否有视频内容
     */
    public boolean hasVideo() {
        return ArrayUtil.isNotEmpty(videoData) || StrUtil.isNotBlank(videoUrl);
    }

    /**
     * 检查是否有多图片
     */
    public boolean hasMultiImages() {
        return ArrayUtil.isNotEmpty(multiImageUrls);
    }

    /**
     * 检查是否有任何内容
     */
    public boolean hasAnyContent() {
        return hasText() || hasImage() || hasVideo() || hasMultiImages();
    }

    /**
     * 获取内容的数量
     */
    public int getContentCount() {
        int count = 0;
        if (hasText()) count++;
        if (hasImage()) count++;
        if (hasVideo()) count++;
        if (hasMultiImages()) count++;
        return count;
    }
}