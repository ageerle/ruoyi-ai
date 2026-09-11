package org.ruoyi.ipd.mapper;

import org.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;
import org.ruoyi.ipd.domain.GateArbitration;

/**
 * Gate 冲突仲裁与超管终裁意见 mapper（P2-5.4 AC-GATE-10；表 gate_arbitrations，uk gate+round+arbitrator）
 */
public interface GateArbitrationMapper extends BaseMapperPlus<GateArbitration, GateArbitration> {
}
