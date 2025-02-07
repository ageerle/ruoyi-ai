package org.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.system.domain.WxRobRelation;

import java.io.Serial;
import java.io.Serializable;



/**
 * 【请填写功能名称】视图对象 wx_rob_relation
 *
 * @author Lion Li
 * @date 2024-05-01
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = WxRobRelation.class)
public class WxRobRelationVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    @ExcelProperty(value = "")
    private Long id;

    /**
     * 外接唯一码
     */
    @ExcelProperty(value = "外接唯一码")
    private String outKey;

    /**
     * 机器唯一码
     */
    @ExcelProperty(value = "机器唯一码")
    private String uniqueKey;

    /**
     * 目标昵称
     */
    @ExcelProperty(value = "目标昵称")
    private String nickName;

    /**
     * 群1好友0
     */
    @ExcelProperty(value = "群1好友0")
    private Integer toGroup;

    /**
     * 启用1禁用0
     */
    @ExcelProperty(value = "启用1禁用0")
    private Integer enable;

    /**
     * IP白名单
     */
    @ExcelProperty(value = "IP白名单")
    private String whiteList;


}
