package org.ruoyi.ipd.mapper;

import org.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;
import org.ruoyi.ipd.domain.NotificationEvent;

/**
 * 站内通知与待办事件 Mapper（OPS-05）。
 * 纪律：投递状态/已读流转仅经 service 内受控 lambdaUpdate（条件 UPDATE 乐观守卫），
 * 不提供 updateById(entity) 业务调用通道，防止并发双发/覆盖他人已读态。
 */
public interface NotificationEventMapper extends BaseMapperPlus<NotificationEvent, NotificationEvent> {
}
