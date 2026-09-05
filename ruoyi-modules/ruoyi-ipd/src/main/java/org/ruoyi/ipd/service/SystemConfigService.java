package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.domain.SystemConfig;
import org.ruoyi.ipd.mapper.SystemConfigMapper;
import org.springframework.stereotype.Service;

/**
 * 系统参数读取服务（v3 §7 参数表 / 开发说明书 D.0.7；种子见 2026-09-04-ipd-p0-config-seed.sql）
 * 取值顺序：库值 → 调用方默认值。数字/布尔由调用方按 valueType 解析。
 */
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigMapper systemConfigMapper;

    public String getValue(String key, String defaultValue) {
        SystemConfig config = systemConfigMapper.selectOne(
            new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, key).last("limit 1"));
        return config != null && config.getConfigValue() != null ? config.getConfigValue() : defaultValue;
    }

    public int getIntValue(String key, int defaultValue) {
        try {
            return Integer.parseInt(getValue(key, String.valueOf(defaultValue)).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBoolValue(String key, boolean defaultValue) {
        return Boolean.parseBoolean(getValue(key, String.valueOf(defaultValue)).trim());
    }
}