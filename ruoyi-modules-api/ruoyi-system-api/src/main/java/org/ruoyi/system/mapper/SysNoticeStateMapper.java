package org.ruoyi.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.ruoyi.core.mapper.BaseMapperPlus;
import org.ruoyi.system.domain.SysNoticeState;
import org.ruoyi.system.domain.vo.SysNoticeStateVo;

/**
 * 用户阅读状态Mapper接口
 *
 * @author Lion Li
 * @date 2024-05-11
 */
@Mapper
public interface SysNoticeStateMapper extends BaseMapperPlus<SysNoticeState, SysNoticeStateVo> {

    /**
     * 阅读所有公告
     */
    void readAllNotice();
}
