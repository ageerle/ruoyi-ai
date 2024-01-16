package com.xmzs.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.xmzs.common.core.constant.CacheNames;
import com.xmzs.common.core.exception.ServiceException;
import com.xmzs.common.core.utils.MapstructUtils;
import com.xmzs.common.core.utils.StreamUtils;
import com.xmzs.common.core.utils.StringUtils;
import com.xmzs.common.json.utils.JsonUtils;
import com.xmzs.common.mybatis.core.page.PageQuery;
import com.xmzs.common.mybatis.core.page.TableDataInfo;
import com.xmzs.common.oss.constant.OssConstant;
import com.xmzs.common.redis.utils.CacheUtils;
import com.xmzs.common.redis.utils.RedisUtils;
import com.xmzs.common.tenant.core.TenantEntity;
import com.xmzs.common.tenant.helper.TenantHelper;
import com.xmzs.system.domain.SysOssConfig;
import com.xmzs.system.domain.bo.SysOssConfigBo;
import com.xmzs.system.domain.vo.SysOssConfigVo;
import com.xmzs.system.mapper.SysOssConfigMapper;
import com.xmzs.system.service.ISysOssConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 对象存储配置Service业务层处理
 *
 * @author Lion Li
 * @author 孤舟烟雨
 * @date 2021-08-13
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SysOssConfigServiceImpl implements ISysOssConfigService {

    private final SysOssConfigMapper baseMapper;

    /**
     * 项目启动时，初始化参数到缓存，加载配置类
     */
    @Override
    public void init() {
        List<SysOssConfig> list = TenantHelper.ignore(() ->
            baseMapper.selectList(
                new LambdaQueryWrapper<SysOssConfig>().orderByAsc(TenantEntity::getTenantId))
        );
        Map<String, List<SysOssConfig>> map = StreamUtils.groupByKey(list, SysOssConfig::getTenantId);
        try {
            for (String tenantId : map.keySet()) {
                TenantHelper.setDynamic(tenantId);
                // 加载OSS初始化配置
                for (SysOssConfig config : map.get(tenantId)) {
                    String configKey = config.getConfigKey();
                    if ("0".equals(config.getStatus())) {
                        RedisUtils.setCacheObject(OssConstant.DEFAULT_CONFIG_KEY, configKey);
                    }
                    CacheUtils.put(CacheNames.SYS_OSS_CONFIG, config.getConfigKey(), JsonUtils.toJsonString(config));
                }
            }
        } finally {
            TenantHelper.clearDynamic();
        }
    }

    @Override
    public SysOssConfigVo queryById(Long ossConfigId) {
        return baseMapper.selectVoById(ossConfigId);
    }

    @Override
    public TableDataInfo<SysOssConfigVo> queryPageList(SysOssConfigBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysOssConfig> lqw = buildQueryWrapper(bo);
        Page<SysOssConfigVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }


    private LambdaQueryWrapper<SysOssConfig> buildQueryWrapper(SysOssConfigBo bo) {
        LambdaQueryWrapper<SysOssConfig> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getConfigKey()), SysOssConfig::getConfigKey, bo.getConfigKey());
        lqw.like(StringUtils.isNotBlank(bo.getBucketName()), SysOssConfig::getBucketName, bo.getBucketName());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), SysOssConfig::getStatus, bo.getStatus());
        return lqw;
    }

    @Override
    public Boolean insertByBo(SysOssConfigBo bo) {
        SysOssConfig config = MapstructUtils.convert(bo, SysOssConfig.class);
        validEntityBeforeSave(config);
        boolean flag = baseMapper.insert(config) > 0;
        if (flag) {
            CacheUtils.put(CacheNames.SYS_OSS_CONFIG, config.getConfigKey(), JsonUtils.toJsonString(config));
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(SysOssConfigBo bo) {
        SysOssConfig config = MapstructUtils.convert(bo, SysOssConfig.class);
        validEntityBeforeSave(config);
        LambdaUpdateWrapper<SysOssConfig> luw = new LambdaUpdateWrapper<>();
        luw.set(ObjectUtil.isNull(config.getPrefix()), SysOssConfig::getPrefix, "");
        luw.set(ObjectUtil.isNull(config.getRegion()), SysOssConfig::getRegion, "");
        luw.set(ObjectUtil.isNull(config.getExt1()), SysOssConfig::getExt1, "");
        luw.set(ObjectUtil.isNull(config.getRemark()), SysOssConfig::getRemark, "");
        luw.eq(SysOssConfig::getOssConfigId, config.getOssConfigId());
        boolean flag = baseMapper.update(config, luw) > 0;
        if (flag) {
            CacheUtils.put(CacheNames.SYS_OSS_CONFIG, config.getConfigKey(), JsonUtils.toJsonString(config));
        }
        return flag;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(SysOssConfig entity) {
        if (StringUtils.isNotEmpty(entity.getConfigKey())
            && !checkConfigKeyUnique(entity)) {
            throw new ServiceException("操作配置'" + entity.getConfigKey() + "'失败, 配置key已存在!");
        }
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            if (CollUtil.containsAny(ids, OssConstant.SYSTEM_DATA_IDS)) {
                throw new ServiceException("系统内置, 不可删除!");
            }
        }
        List<SysOssConfig> list = CollUtil.newArrayList();
        for (Long configId : ids) {
            SysOssConfig config = baseMapper.selectById(configId);
            list.add(config);
        }
        boolean flag = baseMapper.deleteBatchIds(ids) > 0;
        if (flag) {
            list.forEach(sysOssConfig ->
                CacheUtils.evict(CacheNames.SYS_OSS_CONFIG, sysOssConfig.getConfigKey()));
        }
        return flag;
    }

    /**
     * 判断configKey是否唯一
     */
    private boolean checkConfigKeyUnique(SysOssConfig sysOssConfig) {
        long ossConfigId = ObjectUtil.isNull(sysOssConfig.getOssConfigId()) ? -1L : sysOssConfig.getOssConfigId();
        SysOssConfig info = baseMapper.selectOne(new LambdaQueryWrapper<SysOssConfig>()
            .select(SysOssConfig::getOssConfigId, SysOssConfig::getConfigKey)
            .eq(SysOssConfig::getConfigKey, sysOssConfig.getConfigKey()));
        if (ObjectUtil.isNotNull(info) && info.getOssConfigId() != ossConfigId) {
            return false;
        }
        return true;
    }

    /**
     * 启用禁用状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateOssConfigStatus(SysOssConfigBo bo) {
        SysOssConfig sysOssConfig = MapstructUtils.convert(bo, SysOssConfig.class);
        int row = baseMapper.update(null, new LambdaUpdateWrapper<SysOssConfig>()
            .set(SysOssConfig::getStatus, "1"));
        row += baseMapper.updateById(sysOssConfig);
        if (row > 0) {
            RedisUtils.setCacheObject(OssConstant.DEFAULT_CONFIG_KEY, sysOssConfig.getConfigKey());
        }
        return row;
    }

}
