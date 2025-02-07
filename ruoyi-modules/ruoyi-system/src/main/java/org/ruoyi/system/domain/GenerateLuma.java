package org.ruoyi.system.domain;

import lombok.Data;

/**
 * 描述：文生视频请求对象
 *
 * @author ageerle@163.com
 * date 2024/6/27
 */
@Data
public class GenerateLuma {

    private String aspect_ratio;

    private boolean expand_prompt;

    private String image_url;

    private String user_prompt;

}
