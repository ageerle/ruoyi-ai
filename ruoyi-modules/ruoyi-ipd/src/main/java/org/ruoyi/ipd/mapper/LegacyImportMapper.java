package org.ruoyi.ipd.mapper;

import org.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;
import org.ruoyi.ipd.domain.LegacyImport;

/**
 * 存量导入批次 Mapper（P1-9.1 legacy_imports）。
 * 2026-09-08 缺口补齐：LegacyImportService 批量导入此前无批次落库路径，
 * legacy_imports 表 0 行、导入不可追溯——本 Mapper 仅供批次记录读写。
 */
public interface LegacyImportMapper extends BaseMapperPlus<LegacyImport, LegacyImport> {
}
