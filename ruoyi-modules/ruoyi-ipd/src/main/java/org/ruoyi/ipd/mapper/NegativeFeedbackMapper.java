package org.ruoyi.ipd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.ruoyi.ipd.domain.NegativeFeedback;

/**
 * 负反馈 Mapper（P3-8.2；BR-INC-10；AC-INC-36b/37/38/39/40）
 *
 * <p>单表 CRUD + LambdaQueryWrapper 即可；唯一索引 uk_nf_project_trigger_active 保证幂等去重。
 */
@Mapper
public interface NegativeFeedbackMapper extends BaseMapper<NegativeFeedback> {
}