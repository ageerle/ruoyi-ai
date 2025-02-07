package org.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.ruoyi.system.domain.SysUser;
import org.ruoyi.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.ruoyi.system.domain.bo.WxRobConfigBo;
import org.ruoyi.system.domain.vo.WxRobConfigVo;
import org.ruoyi.system.domain.WxRobConfig;
import org.ruoyi.system.mapper.WxRobConfigMapper;
import org.ruoyi.system.service.IWxRobConfigService;

import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * 【请填写功能名称】Service业务层处理
 *
 * @author Lion Li
 * @date 2024-05-01
 */
@RequiredArgsConstructor
@Service
public class WxRobConfigServiceImpl implements IWxRobConfigService {

    private final WxRobConfigMapper baseMapper;

    private final SysUserMapper sysUserMapper;

    /**
     * 查询【请填写功能名称】
     */
    @Override
    public WxRobConfigVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询【请填写功能名称】列表
     */
    @Override
    public TableDataInfo<WxRobConfigVo> queryPageList(WxRobConfigBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<WxRobConfig> lqw = buildQueryWrapper(bo);
        Page<WxRobConfigVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        if(CollectionUtil.isEmpty(result.getRecords())){
            return TableDataInfo.build(result);
        }
        // 获取所有userId
        List<Long> userIds = result.getRecords().stream()
            .map(WxRobConfigVo::getUserId)
            .collect(Collectors.toList());
        // 一次性查询所有userName
        Map<Long, String> userIdToUserNameMap = getUserNamesByUserIds(userIds);
        // 设置userName
        result.getRecords().forEach(wxRobConfigVo -> {
            wxRobConfigVo.setUserName(userIdToUserNameMap.get(wxRobConfigVo.getUserId()));
        });
        return TableDataInfo.build(result);
    }

    private Map<Long, String> getUserNamesByUserIds(List<Long> userIds) {
        // 实现批量查询userName的逻辑，例如通过sysUserMapper查询sys_user表
        List<SysUser> sysUsers = sysUserMapper.selectBatchIds(userIds);
        return sysUsers.stream()
            .collect(Collectors.toMap(SysUser::getUserId, SysUser::getUserName));
    }
    /**
     * 查询【请填写功能名称】列表
     */
    @Override
    public List<WxRobConfigVo> queryList(WxRobConfigBo bo) {
        LambdaQueryWrapper<WxRobConfig> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<WxRobConfig> buildQueryWrapper(WxRobConfigBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<WxRobConfig> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getUserId() != null, WxRobConfig::getUserId, bo.getUserId());
        lqw.eq(StringUtils.isNotBlank(bo.getUniqueKey()), WxRobConfig::getUniqueKey, bo.getUniqueKey());
        lqw.eq(bo.getDefaultFriend() != null, WxRobConfig::getDefaultFriend, bo.getDefaultFriend());
        lqw.eq(bo.getDefaultGroup() != null, WxRobConfig::getDefaultGroup, bo.getDefaultGroup());
        lqw.eq(bo.getEnable() != null, WxRobConfig::getEnable, bo.getEnable());
        return lqw;
    }

    /**
     * 新增【请填写功能名称】
     */
    @Override
    public Boolean insertByBo(WxRobConfigBo bo) {
        WxRobConfig add = MapstructUtils.convert(bo, WxRobConfig.class);
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
    public Boolean updateByBo(WxRobConfigBo bo) {
        WxRobConfig update = MapstructUtils.convert(bo, WxRobConfig.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(WxRobConfig entity){
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
