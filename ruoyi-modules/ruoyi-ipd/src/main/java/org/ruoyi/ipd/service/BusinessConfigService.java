package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.BusinessConfigKeys;
import org.ruoyi.ipd.domain.IpdBusinessConfig;
import org.ruoyi.ipd.domain.IpdBusinessConfigVersion;
import org.ruoyi.ipd.mapper.IpdBusinessConfigMapper;
import org.ruoyi.ipd.mapper.IpdBusinessConfigVersionMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IPD 业务参数读取服务（ROOT-R1 P0-6）
 *
 * <p>与 SystemConfigService 解耦：本服务只管理 ipd_business_config（业务规则参数），
 * SystemConfigService 管理 system_configs（系统参数）。两者通过 {@code config_key} 前缀区分。
 *
 * <p>缓存（PERF-02 同型设计）：
 * <ul>
 *   <li>读走 {@link ConcurrentHashMap} 单 JVM 缓存——热路径日均 10 万+ 次读不再打 DB</li>
 *   <li>{@code computeIfAbsent} per-key 原子装载，天然防击穿</li>
 *   <li>空结果缓存为 {@link Optional#empty()}——防恶意 key 穿透</li>
 *   <li>写穿透失效：update / invalidate 调用后下次读即新值</li>
 * </ul>
 * 单企业私有部署单实例定位，多实例部署需升级 Redis 广播失效（已留 TODO）。
 *
 * <p>方法索引（与 ROOT-R1 §3 根治方案一致）：
 * <ul>
 *   <li>{@link #getString(String)} / {@link #getBigDecimal(String)} / {@link #getInt(String)} / {@link #getBoolean(String)}</li>
 *   <li>{@link #getString(String, String)} 提供 fallback 默认值</li>
 *   <li>{@link #update(String, String, Long)} 写穿透 + 历史版本链</li>
 *   <li>{@link #invalidate(String)} / {@link #invalidateAll()} 写后失效缓存</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessConfigService {

    private final IpdBusinessConfigMapper businessConfigMapper;
    private final IpdBusinessConfigVersionMapper businessConfigVersionMapper;

    /** 单 JVM 缓存；key=业务参数键，value=Optional<IpdBusinessConfig>（empty=配置不存在） */
    private final ConcurrentHashMap<String, Optional<IpdBusinessConfig>> CACHE = new ConcurrentHashMap<>();

    /** 取字符串值（无 key 时抛 ServiceException——按契约禁止穿透到调用方） */
    public String getString(String key) {
        IpdBusinessConfig cfg = requireConfig(key);
        return cfg.getConfigValue();
    }

    /** 取字符串值（带 fallback） */
    public String getString(String key, String defaultValue) {
        return getOptional(key).map(IpdBusinessConfig::getConfigValue).orElse(defaultValue);
    }

    /** 取 BigDecimal 值（按 NUMBER 类型解析） */
    public BigDecimal getBigDecimal(String key) {
        IpdBusinessConfig cfg = requireConfig(key);
        try {
            return new BigDecimal(cfg.getConfigValue());
        } catch (NumberFormatException e) {
            throw new ServiceException("业务参数值非数字 key=" + key + " value=" + cfg.getConfigValue());
        }
    }

    /** 取 BigDecimal 值（带 fallback） */
    public BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
        return getOptional(key)
                .filter(c -> "NUMBER".equals(c.getValueType()))
                .map(IpdBusinessConfig::getConfigValue)
                .map(value -> {
                    try { return new BigDecimal(value); }
                    catch (NumberFormatException e) { return defaultValue; }
                })
                .orElse(defaultValue);
    }

    /** 取 int 值（按 NUMBER 类型解析） */
    public int getInt(String key) {
        return getBigDecimal(key).intValueExact();
    }

    /** 取 int 值（带 fallback） */
    public int getInt(String key, int defaultValue) {
        return getOptional(key)
                .filter(c -> "NUMBER".equals(c.getValueType()))
                .map(IpdBusinessConfig::getConfigValue)
                .map(value -> {
                    try { return new BigDecimal(value).intValueExact(); }
                    catch (NumberFormatException | ArithmeticException e) { return defaultValue; }
                })
                .orElse(defaultValue);
    }

    /** 取 boolean 值（按 BOOL 类型解析；"1"/"true"/"yes" 视为 true） */
    public boolean getBoolean(String key) {
        IpdBusinessConfig cfg = requireConfig(key);
        String v = cfg.getConfigValue();
        return "1".equals(v) || "true".equalsIgnoreCase(v) || "yes".equalsIgnoreCase(v);
    }

    /** 取 boolean 值（带 fallback） */
    public boolean getBoolean(String key, boolean defaultValue) {
        return getOptional(key)
                .filter(c -> "BOOL".equals(c.getValueType()))
                .map(c -> {
                    String v = c.getConfigValue();
                    return "1".equals(v) || "true".equalsIgnoreCase(v) || "yes".equalsIgnoreCase(v);
                })
                .orElse(defaultValue);
    }

    /** 取 Optional<IpdBusinessConfig>（穿透缓存 + DB 一次） */
    public Optional<IpdBusinessConfig> getOptional(String key) {
        return CACHE.computeIfAbsent(key, this::loadFromDb);
    }

    /** 写穿透 + 历史版本链（P0-7 字面量迁移配套） */
    @Transactional(rollbackFor = Exception.class)
    public IpdBusinessConfig update(String key, String newValue, Long operatorId) {
        IpdBusinessConfig existing = requireConfig(key);
        Date now = new Date();
        int newVersion = existing.getVersion() + 1;
        // 1. 闭合当前开区间版本行
        int closed = businessConfigVersionMapper.update(null, new LambdaUpdateWrapper<IpdBusinessConfigVersion>()
                .eq(IpdBusinessConfigVersion::getConfigId, existing.getId())
                .eq(IpdBusinessConfigVersion::getVersion, existing.getVersion())
                .isNull(IpdBusinessConfigVersion::getEffectiveTo)
                .set(IpdBusinessConfigVersion::getEffectiveTo, now));
        if (closed == 0) {
            log.warn("业务参数版本闭合失败 key={} version={}", key, existing.getVersion());
        }
        // 2. 主表更新（version 自增 + 值变更）
        int updated = businessConfigMapper.update(null, new LambdaUpdateWrapper<IpdBusinessConfig>()
                .eq(IpdBusinessConfig::getId, existing.getId())
                .set(IpdBusinessConfig::getConfigValue, newValue)
                .set(IpdBusinessConfig::getVersion, newVersion)
                .set(IpdBusinessConfig::getUpdateBy, operatorId)
                .set(IpdBusinessConfig::getUpdateTime, now));
        if (updated != 1) {
            throw new ServiceException("业务参数更新失败 key=" + key);
        }
        // 3. 追加新版本行
        try {
            IpdBusinessConfigVersion ver = new IpdBusinessConfigVersion();
            ver.setConfigId(existing.getId());
            ver.setConfigKey(key);
            ver.setConfigValue(newValue);
            ver.setVersion(newVersion);
            ver.setEnabled(existing.getEnabled());
            ver.setEffectiveFrom(now);
            ver.setEffectiveTo(null);
            ver.setTenantId(existing.getTenantId());
            ver.setCreateBy(operatorId);
            ver.setCreateTime(now);
            ver.setDelFlag("0");
            businessConfigVersionMapper.insert(ver);
        } catch (DuplicateKeyException e) {
            throw new ServiceException("业务参数版本号冲突 key=" + key + " version=" + newVersion);
        }
        // 4. 写穿透失效
        invalidate(key);
        return businessConfigMapper.selectById(existing.getId());
    }

    /** 失效单个 key 的缓存（外部触发或测试用） */
    public void invalidate(String key) {
        CACHE.remove(key);
    }

    /** 失效全部缓存（系统配置变更后兜底广播） */
    public void invalidateAll() {
        CACHE.clear();
    }

    /** 必填读（无 key 时抛 ServiceException） */
    private IpdBusinessConfig requireConfig(String key) {
        return getOptional(key).orElseThrow(() ->
                new ServiceException("业务参数不存在 key=" + key));
    }

    /** 单次 DB 装载（computeIfAbsent 内部调用） */
    private Optional<IpdBusinessConfig> loadFromDb(String key) {
        LambdaQueryWrapper<IpdBusinessConfig> q = Wrappers.<IpdBusinessConfig>lambdaQuery()
                .eq(IpdBusinessConfig::getConfigKey, key)
                .eq(IpdBusinessConfig::getEnabled, 1)
                .last("LIMIT 1");
        return Optional.ofNullable(businessConfigMapper.selectOne(q));
    }
}
