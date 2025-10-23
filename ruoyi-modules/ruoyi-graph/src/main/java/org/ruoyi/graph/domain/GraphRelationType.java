package org.ruoyi.graph.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 图谱关系类型定义对象 graph_relation_type
 *
 * @author ruoyi
 * @date 2025-09-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("graph_relation_type")
public class GraphRelationType extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 关系名称
     */
    private String relationName;

    /**
     * 关系编码
     */
    private String relationCode;

    /**
     * 描述
     */
    private String description;

    /**
     * 关系方向：0双向、1单向
     */
    private Integer direction;

    /**
     * 可视化样式(JSON格式)
     */
    private String style;

    /**
     * 显示顺序
     */
    private Integer sort;

    /**
     * 是否启用（0否 1是）
     */
    private Integer isEnable;
}
