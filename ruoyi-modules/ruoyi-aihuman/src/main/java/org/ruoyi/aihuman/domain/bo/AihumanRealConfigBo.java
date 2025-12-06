package org.ruoyi.aihuman.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.aihuman.domain.AihumanRealConfig;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 真人交互数字人配置业务对象 aihuman_real_config
 *
 * @author ageerle
 * @date Tue Oct 21 11:46:52 GMT+08:00 2025
 */
@Data

@AutoMapper(target = AihumanRealConfig.class, reverseConvertGenerate = false)
public class AihumanRealConfigBo implements Serializable {

    private Integer id;

    /**
     * 场景名称
     */
    private String name;
    /**
     * 真人形象名称
     */
    private String avatars;
    /**
     * 模型名称
     */
    private String models;
    /**
     * 形象参数（预留）
     */
    private String avatarsParams;
    /**
     * 模型参数（预留）
     */
    private String modelsParams;
    /**
     * 智能体参数（扣子）
     */
    private String agentParams;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    /**
     * 状态
     */
    private Integer status;
    /**
     * 发布状态
     */
    private Integer publish;

    /**
     * 运行参数
     */
    private String runParams;

    /**
     * 运行状态
     */
    private String runStatus;

    /**
     * 创建部门
     */
    private String createDept;
    /**
     * 创建用户
     */
    private String createBy;
    /**
     * 更新用户
     */
    private String updateBy;

}