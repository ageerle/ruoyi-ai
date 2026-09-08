package org.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * URL 快捷入口视图对象（供 Copilot 新会话展示）
 *
 * @author ruoyi
 */
@Data
public class SysUrlShortcutVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 链接ID
     */
    private Long urlId;

    /**
     * 链接名称
     */
    private String name;

    /**
     * HTTP(S) 地址
     */
    private String url;

    /**
     * 链接说明
     */
    private String description;

    /**
     * 排序
     */
    private Integer sortOrder;

}
