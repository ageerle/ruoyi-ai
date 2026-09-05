package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.domain.SystemConfig;
import org.ruoyi.ipd.mapper.SystemConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
 *   <li>写穿透失效：后台参数端在提交事务后调
 *       {@link #invalidate(String)}/{@link #invalidateAll()}，下次读即新值——无生效窗口。</li>
 * </ul>
 * 单 JVM 缓存（单企业私有部署单实例定位）；多实例部署时需升级 Redis 广播失效。
 */
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigMapper systemConfigMapper;

    private final ConcurrentHashMap<String, Optional<String>> cache = new ConcurrentHashMap<>();

    /**
     * 读取参数值（缓存命中优先）。
     *
     * @param key          参数键
     * @param defaultValue 库无记录时的默认值
     * @return 参数值
     */
    public String getValue(String key, String defaultValue) {
        return cache.computeIfAbsent(key, k -> {
            SystemConfig config = systemConfigMapper.selectOne(
                new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, k).last("limit 1"));
            return config != null && config.getConfigValue() != null
                ? Optional.of(config.getConfigValue()) : Optional.empty();
        }).orElse(defaultValue);
    }

    /**
     * 读取整型参数。
     *
     * @param key          参数键
     * @param defaultValue 默认值
     * @return 解析后的整数
     */
    public int getIntValue(String key, int defaultValue) {
        try {
            return Integer.parseInt(getValue(key, String.valueOf(defaultValue)).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 读取布尔参数。
     *
     * @param key          参数键
     * @param defaultValue 默认值
     * @return 解析后的布尔值
     */
    public boolean getBoolValue(String key, boolean defaultValue) {
        return Boolean.parseBoolean(getValue(key, String.valueOf(defaultValue)).trim());
    }

    /**
     * 列出全部参数（按 configKey 升序）。
     *
     * @return 参数列表
     */
    public List<SystemConfig> list() {
        return systemConfigMapper.selectList(
            new LambdaQueryWrapper<SystemConfig>().orderByAsc(SystemConfig::getConfigKey));
    }

    /**
     * 按 key 更新 value；写后立即失效缓存（PERF-02）。
     * key 不存在时静默忽略（仅 invalidate，便于回放/幂等）。
     *
     * @param key   参数键
     * @param value 新值
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(String key, String value) {
        SystemConfig existing = systemConfigMapper.selectOne(
            new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, key).last("limit 1"));
        if (existing == null) {
            invalidate(key);
            return;
        }
        existing.setConfigValue(value);
        systemConfigMapper.updateById(existing);
        invalidate(key);
    }

    /**
     * 写穿透失效：参数更新事务提交后调用，下次读即新值。
     *
     * @param key 参数键
     */
    public void invalidate(String key) {
        cache.remove(key);
    }

    /** 写穿透失效：批量/结构化配置变更后调用。 */
    public void invalidateAll() {
        cache.clear();
    }
}
