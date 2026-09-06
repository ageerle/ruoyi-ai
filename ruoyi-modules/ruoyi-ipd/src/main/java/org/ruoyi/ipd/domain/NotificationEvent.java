package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.util.Date;

/**
 * IPD 站内通知与可执行待办事件（OPS-05 outbox；AC-TEAM/GATE/DEL 通知场景底座）。
 *
 * <p>发布与投递解耦：业务方事务内 {@code publish}（dedup_key 幂等），消费端
 * {@code dispatchPending} 轮询投递（调度接线待 OPS-04 scheduler 合入主树）。
 * kind 严格区分 FYI（跨组知会，AC-DEL-04）与 ACTION（可执行审批行动）；
 * channel 一期固定 MOCK——是"已落库待投递"的留痕标记，不代表真实外部送达。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName(value = "notification_events", autoResultMap = true)
public class NotificationEvent extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 接收者（persons.id；收件箱仅本人可见） */
    private Long receiverId;

    /** 事件类型（目录见 NotificationService.Types） */
    @TableField("event_type")
    private String eventType;

    /** FYI=跨组知会 / ACTION=可执行审批行动 */
    private String kind;

    /** 业务来源表名（bid_invitations|gates|gate_reviews|deletion_requests|projects） */
    @TableField("source_type")
    private String sourceType;

    /** 业务来源行ID */
    @TableField("source_id")
    private Long sourceId;

    /** 幂等去重键 = source_type:event_type:source_id:receiver_id（uk_notify_dedup 兜底并发双发） */
    @TableField("dedup_key")
    private String dedupKey;

    /** 标题（页03 站内信列表） */
    private String title;

    /** 正文 */
    private String content;

    /** 行动跳转链接（kind=ACTION 时填写） */
    @TableField("action_url")
    private String actionUrl;

    /** 投递渠道（一期 MOCK） */
    private String channel;

    /** PENDING|SENT|FAILED|DEAD */
    @TableField("delivery_status")
    private String deliveryStatus;

    /** 已重试次数（达上限转 DEAD） */
    @TableField("retry_count")
    private Integer retryCount;

    /** 下次可投递时间（指数退避；PENDING 为 NULL） */
    @TableField("next_retry_at")
    private Date nextRetryAt;

    /** 已读：0未读 1已读 */
    @TableField("read_flag")
    private String readFlag;

    /** 已读时间 */
    @TableField("read_at")
    private Date readAt;

    /** 软删除（本域无删除通道，仅映射） */
    @TableLogic
    @TableField("del_flag")
    private String delFlag;
}
