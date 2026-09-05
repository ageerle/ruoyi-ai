package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.domain.SystemConfig;
import org.ruoyi.ipd.mapper.SystemConfigMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统参数读取服务（v3 §7 参数表 / 开发说明书 D.0.7；种子见 2026-09-04-ipd-p0-config-seed.sql）
 * 取值顺序：库值 → 调用方默认值。数字/布尔由调用方按 valueType 解析。
 *
 * <p>PERF-02（用户裁决：配置变更立即生效，不允许 TTL 窗口）：
 * <ul>
 *   <li>读走 {@link ConcurrentHashMap} 缓存——KPI/奖金/Gate 热路径日均 10 万+ 次读不再打 DB；
 *       {@code computeIfAbsent} per-key 原子装载，天然防击穿。</li>
 *   <li>空结果缓存为 {@link Optional#empty()}——防恶意 key 穿透。</li>
 *   <li>写穿透失效：后台参数端（P0-3.2 落地时）在提交事务后调
 *       {@link #invalidate(String)}/{@link #invalidateAll()}，下次读即新值——无生效窗口。</li>
 * </ul>
 * 单 JVM 缓存（单企业私有部署单实例定位）；多实例部署时需升级 Redis 广播失效。
 */
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigMapper systemConfigMapper;

    private final ConcurrentHashMap<String, Optional<String>> cache = new ConcurrentHashMap<>();

    public String getValue(String key, String defaultValue) {
        return cache.computeIfAbsent(key, k -> {
            SystemConfig config = systemConfigMapper.selectOne(
                new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, k).last("limit 1"));
            return config != null && config.getConfigValue() != null
                ? Optional.of(config.getConfigValue()) : Optional.empty();
        }).orElse(defaultValue);
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

    /** 写穿透失效：参数更新事务提交后调用，下次读即新值（立即生效语义）。 */
    public void invalidate(String key) {
        cache.remove(key);
    }

    /** 写穿透失效：批量/结构化配置变更后调用。 */
    public void invalidateAll() {
        cache.clear();
    }
}
