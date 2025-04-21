package org.ruoyi.domain.vo;

import org.ruoyi.domain.ChatRobConfig;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;



/**
 * 聊天机器人配置视图对象 chat_rob_config
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = ChatRobConfig.class)
public class ChatRobConfigVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * 所属用户
     */
    @ExcelProperty(value = "所属用户")
    private Long userId;

    /**
     * 机器人名称
     */
    @ExcelProperty(value = "机器人名称")
    private String botName;

    /**
     * 机器唯一码
     */
    @ExcelProperty(value = "机器唯一码")
    private String uniqueKey;

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
     * 机器人状态  0正常 1启用
     */
    @ExcelProperty(value = "机器人状态  0正常 1启用")
    private String enable;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;


}
