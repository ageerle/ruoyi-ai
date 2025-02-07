package org.ruoyi.system.service.impl;

import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.ruoyi.system.domain.bo.WxRobKeywordBo;
import org.ruoyi.system.domain.vo.WxRobKeywordVo;
import org.ruoyi.system.domain.WxRobKeyword;
import org.ruoyi.system.mapper.WxRobKeywordMapper;
import org.ruoyi.system.service.IWxRobKeywordService;

import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 【请填写功能名称】Service业务层处理
 *
 * @author Lion Li
 * @date 2024-05-01
 */
@RequiredArgsConstructor
@Service
public class WxRobKeywordServiceImpl implements IWxRobKeywordService {

    private final WxRobKeywordMapper baseMapper;

    /**
     * 查询【请填写功能名称】
     */
    @Override
    public WxRobKeywordVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询【请填写功能名称】列表
     */
    @Override
    public TableDataInfo<WxRobKeywordVo> queryPageList(WxRobKeywordBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<WxRobKeyword> lqw = buildQueryWrapper(bo);
        Page<WxRobKeywordVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询【请填写功能名称】列表
     */
    @Override
    public List<WxRobKeywordVo> queryList(WxRobKeywordBo bo) {
        LambdaQueryWrapper<WxRobKeyword> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<WxRobKeyword> buildQueryWrapper(WxRobKeywordBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<WxRobKeyword> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getUniqueKey()), WxRobKeyword::getUniqueKey, bo.getUniqueKey());
        lqw.eq(StringUtils.isNotBlank(bo.getKeyData()), WxRobKeyword::getKeyData, bo.getKeyData());
        lqw.eq(StringUtils.isNotBlank(bo.getValueData()), WxRobKeyword::getValueData, bo.getValueData());
        lqw.eq(StringUtils.isNotBlank(bo.getTypeData()), WxRobKeyword::getTypeData, bo.getTypeData());
        lqw.like(StringUtils.isNotBlank(bo.getNickName()), WxRobKeyword::getNickName, bo.getNickName());
        lqw.eq(bo.getToGroup() != null, WxRobKeyword::getToGroup, bo.getToGroup());
        lqw.eq(bo.getEnable() != null, WxRobKeyword::getEnable, bo.getEnable());
        return lqw;
    }

    /**
     * 新增【请填写功能名称】
     */
    @Override
    public Boolean insertByBo(WxRobKeywordBo bo) {
        WxRobKeyword add = MapstructUtils.convert(bo, WxRobKeyword.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改【请填写功能名称】
     */
    @Override
    public Boolean updateByBo(WxRobKeywordBo bo) {
        WxRobKeyword update = MapstructUtils.convert(bo, WxRobKeyword.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(WxRobKeyword entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除【请填写功能名称】
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
