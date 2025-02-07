package org.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.system.domain.WxRobKeyword;

import java.io.Serial;
import java.io.Serializable;



/**
 * 【请填写功能名称】视图对象 wx_rob_keyword
 *
 * @author Lion Li
 * @date 2024-05-01
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = WxRobKeyword.class)
public class WxRobKeywordVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    @ExcelProperty(value = "")
    private Long id;

    /**
     * 机器唯一码
     */
    @ExcelProperty(value = "机器唯一码")
    private String uniqueKey;

    /**
     * 关键词
     */
    @ExcelProperty(value = "关键词")
    private String keyData;

    /**
     * 回复内容
     */
    @ExcelProperty(value = "回复内容")
    private String valueData;

    /**
     * 回复类型
     */
    @ExcelProperty(value = "回复类型")
    private String typeData;

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


}
