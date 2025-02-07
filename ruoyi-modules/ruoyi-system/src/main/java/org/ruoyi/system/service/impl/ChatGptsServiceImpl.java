package org.ruoyi.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.system.domain.ChatGpts;
import org.ruoyi.system.domain.bo.ChatGptsBo;
import org.ruoyi.system.domain.vo.ChatGptsVo;
import org.ruoyi.system.mapper.ChatGptsMapper;
import org.ruoyi.system.service.IChatGptsService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * gpts管理Service业务层处理
 *
 * @author Lion Li
 * @date 2024-07-09
 */
@RequiredArgsConstructor
@Service
public class ChatGptsServiceImpl implements IChatGptsService {

    private final ChatGptsMapper baseMapper;

    /**
     * 查询gpts管理
     */
    @Override
    public ChatGptsVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询gpts管理列表
     */
    @Override
    public TableDataInfo<ChatGptsVo> queryPageList(ChatGptsBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ChatGpts> lqw = buildQueryWrapper(bo);
        Page<ChatGptsVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询gpts管理列表
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
        lqw.eq(StringUtils.isNotBlank(bo.getUseCnt()), ChatGpts::getUseCnt, bo.getUseCnt());
        lqw.eq(StringUtils.isNotBlank(bo.getBad()), ChatGpts::getBad, bo.getBad());
        lqw.eq(StringUtils.isNotBlank(bo.getType()), ChatGpts::getType, bo.getType());
        lqw.eq(StringUtils.isNotBlank(bo.getUpdateIp()), ChatGpts::getUpdateIp, bo.getUpdateIp());
        return lqw;
    }

    /**
     * 新增gpts管理
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
     * 修改gpts管理
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
     * 批量删除gpts管理
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
