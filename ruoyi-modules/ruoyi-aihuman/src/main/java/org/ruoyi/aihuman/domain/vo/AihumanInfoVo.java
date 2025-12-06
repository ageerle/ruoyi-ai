package org.ruoyi.aihuman.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.aihuman.domain.AihumanInfo;

import java.io.Serializable;
import java.util.Date;

/**
 * AI人类交互信息视图对象
 *
 * @author QingYunAI
 */
@Data
@AutoMapper(target = AihumanInfo.class)
public class AihumanInfoVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 交互名称
     */
    private String name;

    /**
     * 交互内容
     */
    private String content;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}