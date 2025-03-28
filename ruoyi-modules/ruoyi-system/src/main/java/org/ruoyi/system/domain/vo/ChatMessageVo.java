package org.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.system.domain.ChatMessage;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;


/**
 * 聊天消息视图对象 chat_message
 *
 * @author Lion Li
 * @date 2023-11-26
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
    @NotNull(message = "主键不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 用户ID
     */
    @NotBlank(message = "用户ID", groups = { AddGroup.class, EditGroup.class })
    private Long userId;

    /**
     * 用户名称
     */
    @NotBlank(message = "用户名称")
    private String userName;

    /**
     * 对话角色
     */
    private String role;


    /**
     * 消息内容
     */
    @NotBlank(message = "消息内容不能为空", groups = { AddGroup.class, EditGroup.class })
    private String content;


    /**
     * 扣除费用
     */
    private Double deductCost;

    /**
     * 累计 Tokens
     */
    @NotNull(message = "累计 Tokens不能为空", groups = { AddGroup.class, EditGroup.class })
    private Integer totalTokens;

    /**
     * 模型名称
     */
    @NotBlank(message = "模型名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String modelName;

    /**
     * 备注
     */
    @NotBlank(message = "备注不能为空", groups = { AddGroup.class, EditGroup.class })
    private String remark;

    /**
     * 创建时间
     */
    private Date createTime;

}
