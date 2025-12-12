package org.ruoyi.aihuman.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.aihuman.domain.AihumanConfig;
import org.ruoyi.common.excel.annotation.ExcelDictFormat;
import org.ruoyi.common.excel.convert.ExcelDictConvert;

import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * 交互数字人配置视图对象 aihuman_config
 *
 * @author ageerle
 * @date Fri Sep 26 22:27:00 GMT+08:00 2025
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = AihumanConfig.class)
public class AihumanConfigVo implements Serializable {

    private Integer id;
    /**
     * name
     */
    @ExcelProperty(value = "name")
    private String name;
    /**
     * modelName
     */
    @ExcelProperty(value = "modelName")
    private String modelName;
    /**
     * modelPath
     */
    @ExcelProperty(value = "modelPath")
    private String modelPath;
    /**
     * modelParams
     */
    @ExcelProperty(value = "modelParams")
    private String modelParams;
    /**
     * agentParams
     */
    @ExcelProperty(value = "agentParams")
    private String agentParams;
    /**
     * createTime
     */
    @ExcelProperty(value = "createTime")
    private LocalDateTime createTime;
    /**
     * updateTime
     */
    @ExcelProperty(value = "updateTime")
    private LocalDateTime updateTime;
    /**
     * status
     */
    @ExcelProperty(value = "status", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_common_status")
    private Integer status;
    /**
     * publish
     */
    @ExcelProperty(value = "publish", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_common_status")
    private Integer publish;

}
