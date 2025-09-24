package org.ruoyi.system.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.MinUsagePeriod;
import org.ruoyi.system.domain.bo.MinUsagePeriodBo;
import org.ruoyi.system.domain.vo.MinUsagePeriodVo;
import org.ruoyi.system.mapper.MinUsagePeriodMapper;
import org.ruoyi.system.service.IMinUsagePeriodService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * 最低使用年限表Service业务层处理
 *
 * @author cass
 * @date 2025-09-24
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class MinUsagePeriodServiceImpl implements IMinUsagePeriodService {

    private final MinUsagePeriodMapper baseMapper;

    /**
     * 查询最低使用年限表
     */
    @Override
    public MinUsagePeriodVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 根据国标代码查询最低使用年限表
     */
    @Override
    public MinUsagePeriod queryByGbCode(String gbCode) {
        LambdaQueryWrapper<MinUsagePeriod> lqw = Wrappers.lambdaQuery();
        lqw.eq(MinUsagePeriod::getGbCode, gbCode);
        return baseMapper.selectOne(lqw);
    }

    /**
     * 查询最低使用年限表列表
     */
    @Override
    public TableDataInfo<MinUsagePeriodVo> queryPageList(MinUsagePeriodBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<MinUsagePeriod> lqw = buildQueryWrapper(bo);
        Page<MinUsagePeriodVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询最低使用年限表列表
     */
    @Override
    public List<MinUsagePeriodVo> queryList(MinUsagePeriodBo bo) {
        LambdaQueryWrapper<MinUsagePeriod> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<MinUsagePeriod> buildQueryWrapper(MinUsagePeriodBo bo) {
        LambdaQueryWrapper<MinUsagePeriod> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getCategory()), MinUsagePeriod::getCategory, bo.getCategory());
        lqw.like(StringUtils.isNotBlank(bo.getContent()), MinUsagePeriod::getContent, bo.getContent());
        lqw.eq(ObjectUtil.isNotNull(bo.getMinYears()), MinUsagePeriod::getMinYears, bo.getMinYears());
        lqw.eq(StringUtils.isNotBlank(bo.getGbCode()), MinUsagePeriod::getGbCode, bo.getGbCode());
        return lqw;
    }

    /**
     * 新增最低使用年限表
     */
    @Override
    public Boolean insertByBo(MinUsagePeriodBo bo) {
        MinUsagePeriod add = MapstructUtils.convert(bo, MinUsagePeriod.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改最低使用年限表
     */
    @Override
    public Boolean updateByBo(MinUsagePeriodBo bo) {
        MinUsagePeriod update = MapstructUtils.convert(bo, MinUsagePeriod.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(MinUsagePeriod entity) {
        // 校验国标代码唯一性
        LambdaQueryWrapper<MinUsagePeriod> lqw = Wrappers.lambdaQuery();
        lqw.eq(MinUsagePeriod::getGbCode, entity.getGbCode());
        if (ObjectUtil.isNotNull(entity.getId())) {
            lqw.ne(MinUsagePeriod::getId, entity.getId());
        }
        boolean exists = baseMapper.exists(lqw);
        if (exists) {
            throw new RuntimeException("国标代码已存在");
        }
    }

    /**
     * 批量删除最低使用年限表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    /**
     * 批量导入最低使用年限表数据
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String importData(List<MinUsagePeriodVo> dataList, Boolean isUpdateSupport, String operName) {
        if (ObjectUtil.isNull(dataList) || dataList.size() == 0) {
            throw new RuntimeException("导入数据不能为空！");
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (MinUsagePeriodVo data : dataList) {
            try {
                // 验证是否存在这个数据
                LambdaQueryWrapper<MinUsagePeriod> lqw = Wrappers.lambdaQuery();
                lqw.eq(MinUsagePeriod::getGbCode, data.getGbCode());
                MinUsagePeriod existData = baseMapper.selectOne(lqw);
                if (ObjectUtil.isNull(existData)) {
                    MinUsagePeriod addData = MapstructUtils.convert(data, MinUsagePeriod.class);
                    baseMapper.insert(addData);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、国标代码 " + data.getGbCode() + " 导入成功");
                } else if (isUpdateSupport) {
                    MapstructUtils.convert(data, existData);
                    baseMapper.updateById(existData);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、国标代码 " + data.getGbCode() + " 更新成功");
                } else {
                    failureNum++;
                    failureMsg.append("<br/>" + failureNum + "、国标代码 " + data.getGbCode() + " 已存在");
                }
            } catch (Exception e) {
                failureNum++;
                String msg = "<br/>" + failureNum + "、国标代码 " + data.getGbCode() + " 导入失败：";
                failureMsg.append(msg + e.getMessage());
                log.error(msg, e);
            }
        }
        if (failureNum > 0) {
            failureMsg.insert(0, "很抱歉，导入失败！共 " + failureNum + " 条数据格式不正确，错误如下：");
            throw new RuntimeException(failureMsg.toString());
        } else {
            successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + successNum + " 条，数据如下：");
        }
        return successMsg.toString();
    }
}
