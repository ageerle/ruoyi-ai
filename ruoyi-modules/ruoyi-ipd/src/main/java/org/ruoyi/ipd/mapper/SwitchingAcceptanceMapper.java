package org.ruoyi.ipd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.ruoyi.ipd.domain.SwitchingAcceptance;

/**
 * P3-7.1 月度账务切换验收 Mapper（仅 Base CRUD；复杂查询在 Service 内组装）。
 */
@Mapper
public interface SwitchingAcceptanceMapper extends BaseMapper<SwitchingAcceptance> {
}