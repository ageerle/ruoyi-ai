package org.ruoyi.domain.vo.chat;

import org.ruoyi.domain.entity.chat.ChatSession;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


/**
 * 会话管理视图对象 chat_session
 *
 * @author ageerle
 * @date 2025-12-30
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = ChatSession.class)
public class ChatSessionVo implements Serializable {

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
     * 会话标题
     */
    @ExcelProperty(value = "会话标题")
    private String sessionTitle;

    /**
     * 会话内容
     */
    @ExcelProperty(value = "会话内容")
    private String sessionContent;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 会话ID
     */
    @ExcelProperty(value = "会话ID")
    private String conversationId;


}
