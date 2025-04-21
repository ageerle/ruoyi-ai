package org.ruoyi.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.domain.ChatVisitorUsage;

import java.io.Serial;
import java.io.Serializable;


/**
 * 访客管理视图对象 chat_visitor_usage
 *
 * @author Lion Li
 * @date 2024-07-14
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = ChatVisitorUsage.class)
public class ChatVisitorUsageVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @ExcelProperty(value = "id")
    private Long id;

    /**
     * 浏览器指纹
     */
    @ExcelProperty(value = "浏览器指纹")
    private String fingerprint;

    /**
     * 使用次数
     */
    @ExcelProperty(value = "使用次数")
    private String usageCount;

    /**
     * ip地址
     */
    @ExcelProperty(value = "ip地址")
    private String ipAddress;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 更新IP
     */
    @ExcelProperty(value = "更新IP")
    private String updateIp;


}
