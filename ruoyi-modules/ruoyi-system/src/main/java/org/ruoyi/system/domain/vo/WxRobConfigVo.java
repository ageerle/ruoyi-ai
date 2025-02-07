package org.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import org.ruoyi.common.excel.annotation.ExcelDictFormat;
import org.ruoyi.common.excel.convert.ExcelDictConvert;
import org.ruoyi.system.domain.WxRobConfig;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;



/**
 * 微信机器人视图对象 wx_rob_config
 *
 * @author Lion Li
 * @date 2024-05-01
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = WxRobConfig.class)
public class WxRobConfigVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 机器人名称
     */
    private String botName;

    /**
     * 机器唯一码
     */
    @ExcelProperty(value = "机器唯一码")
    private String uniqueKey;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "备注")
    private String remark;

    /**
     * 默认好友回复开关
     */
    @ExcelProperty(value = "默认好友回复开关")
    private String defaultFriend;

    /**
     * 默认群回复开关
     */
    @ExcelProperty(value = "默认群回复开关")
    private String defaultGroup;


    /**
     * 机器启用1禁用0
     */
    @ExcelProperty(value = "机器启用1禁用0")
    private String enable;


}
