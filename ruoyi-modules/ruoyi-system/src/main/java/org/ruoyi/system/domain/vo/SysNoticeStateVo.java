package org.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.common.excel.annotation.ExcelDictFormat;
import org.ruoyi.common.excel.convert.ExcelDictConvert;
import org.ruoyi.system.domain.SysNoticeState;

import java.io.Serial;
import java.io.Serializable;


/**
 * 用户阅读状态视图对象 sys_notice_state
 *
 * @author Lion Li
 * @date 2024-05-11
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SysNoticeState.class)
public class SysNoticeStateVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @ExcelProperty(value = "ID")
    private Long id;

    /**
     * 用户ID
     */
    @ExcelProperty(value = "用户ID")
    private Long userId;

    /**
     * 公告ID
     */
    @ExcelProperty(value = "公告ID")
    private Long noticeId;

    /**
     * 阅读状态（0未读 1已读）
     */
    @ExcelProperty(value = "阅读状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=未读,1=已读")
    private String readStatus;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;


}
