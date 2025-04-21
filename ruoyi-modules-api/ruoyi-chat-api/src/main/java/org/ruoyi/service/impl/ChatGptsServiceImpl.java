package org.ruoyi.service.impl;

import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.ruoyi.domain.ChatGpts;
import org.ruoyi.domain.bo.ChatGptsBo;
import org.ruoyi.domain.vo.ChatGptsVo;
import org.ruoyi.mapper.ChatGptsMapper;
import org.ruoyi.service.IChatGptsService;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 应用管理Service业务层处理
 *
 * @author ageerle
 * @date 2025-04-08
 */
@RequiredArgsConstructor
@Service
public class ChatGptsServiceImpl implements IChatGptsService {

    private final ChatGptsMapper baseMapper;

    /**
     * 查询应用管理
     */
    @Override
    public ChatGptsVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询应用管理列表
     */
    @Override
    public TableDataInfo<ChatGptsVo> queryPageList(ChatGptsBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ChatGpts> lqw = buildQueryWrapper(bo);
        Page<ChatGptsVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询应用管理列表
     */
    @Override
    public List<ChatGptsVo> queryList(ChatGptsBo bo) {
        LambdaQueryWrapper<ChatGpts> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<ChatGpts> buildQueryWrapper(ChatGptsBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<ChatGpts> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getGid()), ChatGpts::getGid, bo.getGid());
        lqw.like(StringUtils.isNotBlank(bo.getName()), ChatGpts::getName, bo.getName());
        lqw.eq(StringUtils.isNotBlank(bo.getLogo()), ChatGpts::getLogo, bo.getLogo());
        lqw.eq(StringUtils.isNotBlank(bo.getInfo()), ChatGpts::getInfo, bo.getInfo());
        lqw.eq(StringUtils.isNotBlank(bo.getAuthorId()), ChatGpts::getAuthorId, bo.getAuthorId());
        lqw.like(StringUtils.isNotBlank(bo.getAuthorName()), ChatGpts::getAuthorName, bo.getAuthorName());
        lqw.eq(bo.getUseCnt() != null, ChatGpts::getUseCnt, bo.getUseCnt());
        lqw.eq(bo.getBad() != null, ChatGpts::getBad, bo.getBad());
        lqw.eq(StringUtils.isNotBlank(bo.getType()), ChatGpts::getType, bo.getType());
        lqw.eq(StringUtils.isNotBlank(bo.getUpdateIp()), ChatGpts::getUpdateIp, bo.getUpdateIp());
        return lqw;
    }

    /**
     * 新增应用管理
     */
    @Override
    public Boolean insertByBo(ChatGptsBo bo) {
        ChatGpts add = MapstructUtils.convert(bo, ChatGpts.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改应用管理
     */
    @Override
    public Boolean updateByBo(ChatGptsBo bo) {
        ChatGpts update = MapstructUtils.convert(bo, ChatGpts.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(ChatGpts entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除应用管理
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
