package org.ruoyi.aihuman.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.aihuman.domain.AihumanRealConfig;
import org.ruoyi.common.excel.annotation.ExcelDictFormat;
import org.ruoyi.common.excel.convert.ExcelDictConvert;

import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * 真人交互数字人配置视图对象 aihuman_real_config
 *
 * @author ageerle
 * @date Tue Oct 21 11:46:52 GMT+08:00 2025
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = AihumanRealConfig.class)
public class AihumanRealConfigVo implements Serializable {

    private Integer id;
    /**
     * 场景名称
     */
    @ExcelProperty(value = "场景名称")
    private String name;
    /**
     * 真人形象名称
     */
    @ExcelProperty(value = "真人形象名称")
    private String avatars;
    /**
     * 模型名称
     */
    @ExcelProperty(value = "模型名称")
    private String models;
    /**
     * 形象参数（预留）
     */
    @ExcelProperty(value = "形象参数", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "$column.readConverterExp()")
    private String avatarsParams;
    /**
     * 模型参数（预留）
     */
    @ExcelProperty(value = "模型参数", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "$column.readConverterExp()")
    private String modelsParams;
    /**
     * 智能体参数（扣子）
     */
    @ExcelProperty(value = "智能体参数", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "$column.readConverterExp()")
    private String agentParams;
    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private LocalDateTime updateTime;
    /**
     * 状态
     */
    @ExcelProperty(value = "状态")
    private Integer status;
    /**
     * 发布状态
     */
    @ExcelProperty(value = "发布状态")
    private Integer publish;

    /**
     * 运行参数
     */
    @ExcelProperty(value = "运行参数")
    private String runParams;

    /**
     * 运行状态
     */
    @ExcelProperty(value = "运行状态")
    private String runStatus;

    /**
     * 创建部门
     */
    @ExcelProperty(value = "创建部门")
    private String createDept;
    /**
     * 创建用户
     */
    @ExcelProperty(value = "创建用户")
    private String createBy;
    /**
     * 更新用户
     */
    @ExcelProperty(value = "更新用户")
    private String updateBy;

}