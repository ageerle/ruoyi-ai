package org.ruoyi.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.domain.ChatUsageToken;

import java.io.Serial;
import java.io.Serializable;


/**
 * 用户token使用详情视图对象 chat_usage_token
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = ChatUsageToken.class)
public class ChatUsageTokenVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * 用户
     */
    @ExcelProperty(value = "用户")
    private Long userId;

    /**
     * 待结算token
     */
    @ExcelProperty(value = "待结算token")
    private Integer token;

    /**
     * 模型名称
     */
    @ExcelProperty(value = "模型名称")
    private String modelName;

    /**
     * 累计使用token
     */
    @ExcelProperty(value = "累计使用token")
    private String totalToken;


}
