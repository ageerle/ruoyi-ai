package com.xmzs.midjourney.domain;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 绘画费用信息
 *
 * @author Admin
 */
@Data
@Component
@ConfigurationProperties(prefix = "mj")
public class MjPriceConfig {
    /**
     * 放大图像
     */
    private String upsample;

    /**
     * 变化
     */
    private String change;

    /**
     * 图生图
     */
    private String blend;

    /**
     * 图生文
     */
    private String describe;

    /**
     * 文生图
     */
    private String imagine;

    /**
     * 局部重绘
     */
    private String inpaint;

    /**
     * 提示词分析
     */
    private String shorten;

    /**
     * 换脸
     */
    private String faceSwapping;

}
