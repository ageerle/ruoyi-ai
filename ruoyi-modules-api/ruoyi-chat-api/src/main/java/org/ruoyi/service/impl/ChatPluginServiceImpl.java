package org.ruoyi.service.impl;

import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.ruoyi.domain.ChatPlugin;
import org.ruoyi.domain.bo.ChatPluginBo;
import org.ruoyi.domain.vo.ChatPluginVo;
import org.ruoyi.mapper.ChatPluginMapper;
import org.ruoyi.service.IChatPluginService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 插件管理Service业务层处理
 *
 * @author ageerle
 * @date 2025-04-08
 */
@RequiredArgsConstructor
@Service
public class ChatPluginServiceImpl implements IChatPluginService {

    private final ChatPluginMapper baseMapper;

    /**
     * 查询插件管理
     */
    @Override
    public ChatPluginVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询插件管理列表
     */
    @Override
    public TableDataInfo<ChatPluginVo> queryPageList(ChatPluginBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ChatPlugin> lqw = buildQueryWrapper(bo);
        Page<ChatPluginVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询插件管理列表
     */
    @Override
    public List<ChatPluginVo> queryList(ChatPluginBo bo) {
        LambdaQueryWrapper<ChatPlugin> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<ChatPlugin> buildQueryWrapper(ChatPluginBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<ChatPlugin> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getName()), ChatPlugin::getName, bo.getName());
        lqw.eq(StringUtils.isNotBlank(bo.getCode()), ChatPlugin::getCode, bo.getCode());
        return lqw;
    }

    /**
     * 新增插件管理
     */
    @Override
    public Boolean insertByBo(ChatPluginBo bo) {
        ChatPlugin add = MapstructUtils.convert(bo, ChatPlugin.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改插件管理
     */
    @Override
    public Boolean updateByBo(ChatPluginBo bo) {
        ChatPlugin update = MapstructUtils.convert(bo, ChatPlugin.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(ChatPlugin entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除插件管理
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
