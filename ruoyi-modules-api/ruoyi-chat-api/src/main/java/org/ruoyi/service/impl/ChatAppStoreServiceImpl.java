package org.ruoyi.service.impl;

import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.ruoyi.domain.ChatAppStore;
import org.ruoyi.domain.bo.ChatAppStoreBo;
import org.ruoyi.mapper.ChatAppStoreMapper;
import org.ruoyi.service.IChatAppStoreService;
import org.springframework.stereotype.Service;
import org.ruoyi.domain.vo.ChatAppStoreVo;
import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 应用商店Service业务层处理
 *
 * @author ageerle
 * @date 2025-04-08
 */
@RequiredArgsConstructor
@Service
public class ChatAppStoreServiceImpl implements IChatAppStoreService {

    private final ChatAppStoreMapper baseMapper;

    /**
     * 查询应用商店
     */
    @Override
    public ChatAppStoreVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询应用商店列表
     */
    @Override
    public TableDataInfo<ChatAppStoreVo> queryPageList(ChatAppStoreBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ChatAppStore> lqw = buildQueryWrapper(bo);
        Page<ChatAppStoreVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询应用商店列表
     */
    @Override
    public List<ChatAppStoreVo> queryList(ChatAppStoreBo bo) {
        LambdaQueryWrapper<ChatAppStore> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<ChatAppStore> buildQueryWrapper(ChatAppStoreBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<ChatAppStore> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getName()), ChatAppStore::getName, bo.getName());
        lqw.eq(StringUtils.isNotBlank(bo.getDescription()), ChatAppStore::getDescription, bo.getDescription());
        lqw.eq(StringUtils.isNotBlank(bo.getAvatar()), ChatAppStore::getAvatar, bo.getAvatar());
        lqw.eq(StringUtils.isNotBlank(bo.getAppUrl()), ChatAppStore::getAppUrl, bo.getAppUrl());
        return lqw;
    }

    /**
     * 新增应用商店
     */
    @Override
    public Boolean insertByBo(ChatAppStoreBo bo) {
        ChatAppStore add = MapstructUtils.convert(bo, ChatAppStore.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改应用商店
     */
    @Override
    public Boolean updateByBo(ChatAppStoreBo bo) {
        ChatAppStore update = MapstructUtils.convert(bo, ChatAppStore.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(ChatAppStore entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除应用商店
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
