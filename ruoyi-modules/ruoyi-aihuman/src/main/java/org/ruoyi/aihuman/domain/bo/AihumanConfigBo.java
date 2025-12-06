package org.ruoyi.aihuman.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.ruoyi.aihuman.domain.AihumanConfig;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 交互数字人配置业务对象 aihuman_config
 *
 * @author ageerle
 * @date Fri Sep 26 22:27:00 GMT+08:00 2025
 */
@Data

@AutoMapper(target = AihumanConfig.class, reverseConvertGenerate = false)
public class AihumanConfigBo implements Serializable {

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
    @NotNull(message = "status不能为空", groups = {AddGroup.class, EditGroup.class})
    private Integer status;
    /**
     * publish
     */
    @NotNull(message = "publish不能为空", groups = {AddGroup.class, EditGroup.class})
    private Integer publish;

}
