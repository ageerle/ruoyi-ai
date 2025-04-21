package org.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 用户阅读状态对象 sys_notice_state
 *
 * @author Lion Li
 * @date 2024-05-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notice_state")
public class SysNoticeState extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 公告ID
     */
    private Long noticeId;

    /**
     * 阅读状态（0未读 1已读）
     */
    private String readStatus;

    /**
     * 备注
     */
    private String remark;


}
