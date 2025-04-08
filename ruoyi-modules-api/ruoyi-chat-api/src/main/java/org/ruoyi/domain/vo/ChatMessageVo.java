package org.ruoyi.domain.vo;

import java.math.BigDecimal;
import org.ruoyi.system.domain.ChatMessage;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;




/**
 * 聊天消息视图对象 chat_message
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = ChatMessage.class)
public class ChatMessageVo implements Serializable {

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
    @ExcelProperty(value = "用户id")
    private Long userId;

    /**
     * 消息内容
     */
    @ExcelProperty(value = "消息内容")
    private String content;

    /**
     * 对话角色
     */
    @ExcelProperty(value = "对话角色")
    private String role;

    /**
     * 扣除金额
     */
    @ExcelProperty(value = "扣除金额")
    private BigDecimal deductCost;

    /**
     * 累计 Tokens
     */
    @ExcelProperty(value = "累计 Tokens")
    private Long totalTokens;

    /**
     * 模型名称
     */
    @ExcelProperty(value = "模型名称")
    private String modelName;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;


}
