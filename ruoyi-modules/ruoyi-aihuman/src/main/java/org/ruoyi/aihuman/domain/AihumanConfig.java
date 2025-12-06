package org.ruoyi.aihuman.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 交互数字人配置对象 aihuman_config
 *
 * @author ageerle
 * @date Fri Sep 26 22:27:00 GMT+08:00 2025
 */
@Data
@TableName("aihuman_config")
public class AihumanConfig implements Serializable {


    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * name
     */
    private String name;

    /**
     * modelName
     */
    private String modelName;

    /**
     * modelPath
     */
    private String modelPath;

    /**
     * modelParams
     */
    private String modelParams;

    /**
     * agentParams
     */
    private String agentParams;

    /**
     * createTime
     */
    private LocalDateTime createTime;

    /**
     * updateTime
     */
    private LocalDateTime updateTime;

    /**
     * status
     */
    private Integer status;

    /**
     * publish
     */
    private Integer publish;


}
