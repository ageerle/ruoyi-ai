package org.ruoyi.common.chat.domain.vo.chat;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.common.chat.entity.chat.ChatMessage;
import org.ruoyi.common.excel.annotation.ExcelDictFormat;
import org.ruoyi.common.excel.convert.ExcelDictConvert;

import java.io.Serial;
import java.io.Serializable;


/**
 * 聊天消息视图对象 chat_message
 *
 * @author ageerle
 * @date 2025-12-14
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
     * 会话id
     */
    @ExcelProperty(value = "会话id")
    private Long sessionId;

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
    private Long deductCost;

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
     * 计费类型（1-token计费，2-次数计费）
     */
    @ExcelProperty(value = "计费类型", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "1=-token计费，2-次数计费")
    private String billingType;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;


}
