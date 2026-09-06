package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.domain.SystemConfig;
import org.ruoyi.ipd.domain.SystemConfigVersion;
import org.ruoyi.ipd.mapper.SystemConfigMapper;
import org.ruoyi.ipd.mapper.SystemConfigVersionMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;
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
 *
 * <p>P0-3.3 参数版本持久化、不可变快照与时点解析（G-05/G-08；AC-GLB-09/10）：
 * <ul>
 *   <li>每次 {@link #update(String, String, Long)} 在同一事务内写版本链：闭合当前开区间行
 *       （effective_to=now，受控 lambdaUpdate，仅此一处允许 UPDATE）+ 追加新行
 *       （version=last+1, effective_from=now, is_immutable=1）；首个版本前先落基线行，
 *       使时点解析对建表前的历史也能回溯。</li>
 *   <li>版本行 append-only：无 updateById(config_value)/delete 通道；uk(config_key,version,tenant_id)
 *       在真库兕底并发双分配。</li>
 *   <li>{@link #resolveAsOf(String, Date)}：valid-time 区间判定
 *       effective_from ≤ T ＜ effective_to(或 NULL)；无命中行时回退出厂默认 defaultValue。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigMapper systemConfigMapper;
    private final SystemConfigVersionMapper systemConfigVersionMapper;

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
     * 按 key 更新 value；遗留通道（P0-3.2 契约），操作人记 0=系统通道。
     *
     * @param key   参数键
     * @param value 新值
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(String key, String value) {
        update(key, value, null);
    }

    /**
     * 按 key 更新 value；写后立即失效缓存（PERF-02）。
     * key 不存在时静默忽略（仅 invalidate，便于回放/幂等）。
     *
     * <p>P0-3.3：同一事务内写不可变版本链（闭合旧行 + 追加新行；首个版本前补基线行），
     * 事务回滚时版本链与本体值同归 rollback，不产生孤儿版本。
     *
     * @param key        参数键
     * @param value      新值
     * @param operatorId 会话操作人（null → 落 0=系统通道，真库 changed_by NOT NULL）
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(String key, String value, Long operatorId) {
        String normalized = value;
        if (GateEngine.A_LEVEL_CONFIG_KEY.equals(key)) {
            // P1-5.2：A 级必做集 trim/去重/未知码拒绝后再落库
            normalized = GateEngine.validateAndNormalizeALevelConfigValue(value);
        }
        SystemConfig existing = systemConfigMapper.selectOne(
            new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, key).last("limit 1"));
        if (existing == null) {
            invalidate(key);
            return;
        }
        String oldValue = existing.getConfigValue();
        // CodeReview L-1：同值短路——不写本体、不写版本链，避免回放/重复提交膨胀不可变版本链
        if (Objects.equals(normalized, oldValue)) {
            invalidate(key);
            return;
        }
        existing.setConfigValue(normalized);
        systemConfigMapper.updateById(existing);
        recordVersion(existing, oldValue, normalized, operatorId);
        invalidate(key);
    }

    /**
     * P0-3.3 版本链写入（仅在 update 事务内调用）：
     * 首次为该 key 落版本时，先补一行基线（旧值，effective_from=本体行 create_time），
     * 保证时点解析可回溯到建表前；随后闭合当前开区间行并追加新值行。
     *
     * <p>CodeReview M-1：select-then-insert 的版本号分配依赖 uk_config_version 兕底并发双分配；
     * DuplicateKeyException 在此映射为 STATE_CONFLICT 包络（而非落基线 GlobalExceptionHandler 裸 R），
     * 冲突方重试即可（last 已推进）。
     */
    private void recordVersion(SystemConfig existing, String oldValue, String newValue, Long operatorId) {
        String key = existing.getConfigKey();
        Date now = new Date();
        SystemConfigVersion last = systemConfigVersionMapper.selectOne(
            new LambdaQueryWrapper<SystemConfigVersion>()
                .eq(SystemConfigVersion::getConfigKey, key)
                .orderByDesc(SystemConfigVersion::getVersion)
                .last("limit 1"));
        int nextVersion;
        if (last == null) {
            // 基线行：回溯锚。effective_from 取本体行 create_time（缺失时取 epoch）
            Date baselineFrom = existing.getCreateTime() != null ? existing.getCreateTime() : new Date(0L);
            insertVersion(SystemConfigVersion.builder()
                .configKey(key).configValue(oldValue).version(1)
                .effectiveFrom(baselineFrom).effectiveTo(now)
                .isImmutable(Boolean.TRUE)
                .changedBy(operatorId != null ? operatorId : 0L)
                .changeReason("P0-3.3 baseline snapshot")
                .createTime(now)
                .build(), key);
            nextVersion = 2;
        } else {
            // 闭合当前开区间行：版本行唯一允许的 UPDATE，且仅改 effective_to 元数据
            systemConfigVersionMapper.update(null, Wrappers.<SystemConfigVersion>lambdaUpdate()
                .eq(SystemConfigVersion::getConfigKey, key)
                .isNull(SystemConfigVersion::getEffectiveTo)
                .set(SystemConfigVersion::getEffectiveTo, now));
            nextVersion = last.getVersion() + 1;
        }
        insertVersion(SystemConfigVersion.builder()
            .configKey(key).configValue(newValue).version(nextVersion)
            .effectiveFrom(now).effectiveTo(null)
            .isImmutable(Boolean.TRUE)
            .changedBy(operatorId != null ? operatorId : 0L)
            .changeReason("P0-3.3 param update")
            .createTime(now)
            .build(), key);
    }

    /** uk 冲突 → STATE_CONFLICT 包络（CodeReview M-1） */
    private void insertVersion(SystemConfigVersion row, String key) {
        try {
            systemConfigVersionMapper.insert(row);
        } catch (DuplicateKeyException e) {
            throw new ServiceException("并发参数更新冲突，请重试（key=" + key + "）",
                ApiV1ErrorCode.STATE_CONFLICT.getCode());
        }
    }

    /**
     * P0-3.3 时点解析：取 asOf 时点生效的参数值（valid-time 区间判定）。
     * 无命中版本时回退出厂默认 defaultValue；仍无则返回 null（调用方自定默认）。
     *
     * @param key  参数键
     * @param asOf 时点（含）；须为已发生时间
     * @return 命中版本的 config_value；或出厂默认；或 null
     */
    public String getValueAsOf(String key, Date asOf) {
        SystemConfigVersion hit = systemConfigVersionMapper.selectOne(
            new LambdaQueryWrapper<SystemConfigVersion>()
                .eq(SystemConfigVersion::getConfigKey, key)
                .le(SystemConfigVersion::getEffectiveFrom, asOf)
                .and(w -> w.isNull(SystemConfigVersion::getEffectiveTo)
                    .or().gt(SystemConfigVersion::getEffectiveTo, asOf))
                .orderByDesc(SystemConfigVersion::getVersion)
                .last("limit 1"));
        if (hit != null) {
            return hit.getConfigValue();
        }
        SystemConfig row = systemConfigMapper.selectOne(
            new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, key).last("limit 1"));
        return row != null && row.getDefaultValue() != null ? row.getDefaultValue() : null;
    }

    /**
     * P0-3.3 时点解析（含证据视图）：返回命中版本或回退源，供审计/重算对账。
     *
     * @return key/value/version/effectiveFrom/effectiveTo/resolvedFrom（VERSION|FACTORY_DEFAULT|NONE）
     */
    public java.util.LinkedHashMap<String, Object> resolveAsOf(String key, Date asOf) {
        SystemConfigVersion hit = systemConfigVersionMapper.selectOne(
            new LambdaQueryWrapper<SystemConfigVersion>()
                .eq(SystemConfigVersion::getConfigKey, key)
                .le(SystemConfigVersion::getEffectiveFrom, asOf)
                .and(w -> w.isNull(SystemConfigVersion::getEffectiveTo)
                    .or().gt(SystemConfigVersion::getEffectiveTo, asOf))
                .orderByDesc(SystemConfigVersion::getVersion)
                .last("limit 1"));
        java.util.LinkedHashMap<String, Object> view = new java.util.LinkedHashMap<>();
        view.put("key", key);
        view.put("asOf", asOf);
        if (hit != null) {
            view.put("value", hit.getConfigValue());
            view.put("version", hit.getVersion());
            view.put("effectiveFrom", hit.getEffectiveFrom());
            view.put("effectiveTo", hit.getEffectiveTo());
            view.put("resolvedFrom", "VERSION");
            return view;
        }
        SystemConfig row = systemConfigMapper.selectOne(
            new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, key).last("limit 1"));
        if (row != null && row.getDefaultValue() != null) {
            view.put("value", row.getDefaultValue());
            view.put("resolvedFrom", "FACTORY_DEFAULT");
        } else {
            view.put("value", null);
            view.put("resolvedFrom", "NONE");
        }
        return view;
    }

    /**
     * P0-3.3 版本历史（最新在前，键内 version 降序）。
     *
     * @param key   参数键
     * @param limit 上限（1-200）
     */
    public List<SystemConfigVersion> listVersions(String key, int limit) {
        return systemConfigVersionMapper.selectList(
            new LambdaQueryWrapper<SystemConfigVersion>()
                .eq(SystemConfigVersion::getConfigKey, key)
                .orderByDesc(SystemConfigVersion::getVersion)
                .last("limit " + Math.min(Math.max(limit, 1), 200)));
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
