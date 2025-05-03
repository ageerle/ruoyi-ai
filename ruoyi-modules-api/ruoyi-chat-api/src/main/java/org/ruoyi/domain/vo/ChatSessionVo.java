package org.ruoyi.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.domain.ChatSession;

import java.io.Serial;
import java.io.Serializable;



/**
 * 会话管理视图对象 chat_session
 *
 * @author ageerle
 * @date 2025-05-03
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


}
