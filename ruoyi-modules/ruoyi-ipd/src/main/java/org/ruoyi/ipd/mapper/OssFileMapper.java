package org.ruoyi.ipd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.ruoyi.ipd.domain.OssFileEntity;

/**
 * OSS 文件 Mapper（[SEC-FIX-HIGH-1.1-FOLLOWUP]）。
 * 共享 sys_oss 表，IPD 模块独立映射。
 */
@Mapper
public interface OssFileMapper extends BaseMapper<OssFileEntity> {
}
