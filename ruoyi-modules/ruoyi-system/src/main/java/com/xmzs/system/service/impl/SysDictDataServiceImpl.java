package com.xmzs.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xmzs.common.core.constant.CacheNames;
import com.xmzs.common.core.utils.MapstructUtils;
import com.xmzs.common.mybatis.core.page.PageQuery;
import com.xmzs.system.domain.SysDictData;
import com.xmzs.common.mybatis.core.page.TableDataInfo;
import com.xmzs.common.core.exception.ServiceException;
import com.xmzs.common.core.utils.StringUtils;
import com.xmzs.common.redis.utils.CacheUtils;
import com.xmzs.system.domain.bo.SysDictDataBo;
import com.xmzs.system.domain.vo.SysDictDataVo;
import com.xmzs.system.mapper.SysDictDataMapper;
import com.xmzs.system.service.ISysDictDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 字典 业务层处理
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
public class SysDictDataServiceImpl implements ISysDictDataService {

    private final SysDictDataMapper baseMapper;

    @Override
    public TableDataInfo<SysDictDataVo> selectPageDictDataList(SysDictDataBo dictData, PageQuery pageQuery) {
        LambdaQueryWrapper<SysDictData> lqw = buildQueryWrapper(dictData);
        Page<SysDictDataVo> page = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    /**
     * 根据条件分页查询字典数据
     *
     * @param dictData 字典数据信息
     * @return 字典数据集合信息
     */
    @Override
    public List<SysDictDataVo> selectDictDataList(SysDictDataBo dictData) {
        LambdaQueryWrapper<SysDictData> lqw = buildQueryWrapper(dictData);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<SysDictData> buildQueryWrapper(SysDictDataBo bo) {
        LambdaQueryWrapper<SysDictData> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getDictSort() != null, SysDictData::getDictSort, bo.getDictSort());
        lqw.like(StringUtils.isNotBlank(bo.getDictLabel()), SysDictData::getDictLabel, bo.getDictLabel());
        lqw.eq(StringUtils.isNotBlank(bo.getDictType()), SysDictData::getDictType, bo.getDictType());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), SysDictData::getStatus, bo.getStatus());
        lqw.orderByAsc(SysDictData::getDictSort);
        return lqw;
    }

    /**
     * 根据字典类型和字典键值查询字典数据信息
     *
     * @param dictType  字典类型
     * @param dictValue 字典键值
     * @return 字典标签
     */
    @Override
    public String selectDictLabel(String dictType, String dictValue) {
        return baseMapper.selectOne(new LambdaQueryWrapper<SysDictData>()
                .select(SysDictData::getDictLabel)
                .eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getDictValue, dictValue))
            .getDictLabel();
    }

    /**
     * 根据字典数据ID查询信息
     *
     * @param dictCode 字典数据ID
     * @return 字典数据
     */
    @Override
    public SysDictDataVo selectDictDataById(Long dictCode) {
        return baseMapper.selectVoById(dictCode);
    }

    /**
     * 批量删除字典数据信息
     *
     * @param dictCodes 需要删除的字典数据ID
     */
    @Override
    public void deleteDictDataByIds(Long[] dictCodes) {
        for (Long dictCode : dictCodes) {
            SysDictData data = baseMapper.selectById(dictCode);
            baseMapper.deleteById(dictCode);
            CacheUtils.evict(CacheNames.SYS_DICT, data.getDictType());
        }
    }

    /**
     * 新增保存字典数据信息
     *
     * @param bo 字典数据信息
     * @return 结果
     */
    @CachePut(cacheNames = CacheNames.SYS_DICT, key = "#bo.dictType")
    @Override
    public List<SysDictDataVo> insertDictData(SysDictDataBo bo) {
        SysDictData data = MapstructUtils.convert(bo, SysDictData.class);
        int row = baseMapper.insert(data);
        if (row > 0) {
            return baseMapper.selectDictDataByType(data.getDictType());
        }
        throw new ServiceException("操作失败");
    }

    /**
     * 修改保存字典数据信息
     *
     * @param bo 字典数据信息
     * @return 结果
     */
    @CachePut(cacheNames = CacheNames.SYS_DICT, key = "#bo.dictType")
    @Override
    public List<SysDictDataVo> updateDictData(SysDictDataBo bo) {
        SysDictData data = MapstructUtils.convert(bo, SysDictData.class);
        int row = baseMapper.updateById(data);
        if (row > 0) {
            return baseMapper.selectDictDataByType(data.getDictType());
        }
        throw new ServiceException("操作失败");
    }

}
