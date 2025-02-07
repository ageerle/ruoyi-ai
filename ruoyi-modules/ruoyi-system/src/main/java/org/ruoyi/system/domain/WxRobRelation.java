package org.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 【请填写功能名称】对象 wx_rob_relation
 *
 * @author Lion Li
 * @date 2024-05-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wx_rob_relation")
public class WxRobRelation extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 外接唯一码
     */
    private String outKey;

    /**
     * 机器唯一码
     */
    private String uniqueKey;

    /**
     * 目标昵称
     */
    private String nickName;

    /**
     * 群1好友0
     */
    private Integer toGroup;

    /**
     * 启用1禁用0
     */
    private Integer enable;

    /**
     * IP白名单
     */
    private String whiteList;


}
