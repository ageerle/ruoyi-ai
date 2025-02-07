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
import org.ruoyi.system.domain.bo.ChatVisitorUsageBo;
import org.ruoyi.system.domain.vo.ChatVisitorUsageVo;
import org.ruoyi.system.domain.ChatVisitorUsage;
import org.ruoyi.system.mapper.ChatVisitorUsageMapper;
import org.ruoyi.system.service.IChatVisitorUsageService;

import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 访客管理Service业务层处理
 *
 * @author Lion Li
 * @date 2024-07-14
 */
@RequiredArgsConstructor
@Service
public class ChatVisitorUsageServiceImpl implements IChatVisitorUsageService {

    private final ChatVisitorUsageMapper baseMapper;

    /**
     * 查询访客管理
     */
    @Override
    public ChatVisitorUsageVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询访客管理列表
     */
    @Override
    public TableDataInfo<ChatVisitorUsageVo> queryPageList(ChatVisitorUsageBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ChatVisitorUsage> lqw = buildQueryWrapper(bo);
        Page<ChatVisitorUsageVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询访客管理列表
     */
    @Override
    public List<ChatVisitorUsageVo> queryList(ChatVisitorUsageBo bo) {
        LambdaQueryWrapper<ChatVisitorUsage> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<ChatVisitorUsage> buildQueryWrapper(ChatVisitorUsageBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<ChatVisitorUsage> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getFingerprint()), ChatVisitorUsage::getFingerprint, bo.getFingerprint());
        lqw.eq(StringUtils.isNotBlank(bo.getUsageCount()), ChatVisitorUsage::getUsageCount, bo.getUsageCount());
        lqw.eq(StringUtils.isNotBlank(bo.getIpAddress()), ChatVisitorUsage::getIpAddress, bo.getIpAddress());
        lqw.eq(StringUtils.isNotBlank(bo.getUpdateIp()), ChatVisitorUsage::getUpdateIp, bo.getUpdateIp());
        return lqw;
    }

    /**
     * 新增访客管理
     */
    @Override
    public Boolean insertByBo(ChatVisitorUsageBo bo) {
        ChatVisitorUsage add = MapstructUtils.convert(bo, ChatVisitorUsage.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改访客管理
     */
    @Override
    public Boolean updateByBo(ChatVisitorUsageBo bo) {
        ChatVisitorUsage update = MapstructUtils.convert(bo, ChatVisitorUsage.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(ChatVisitorUsage entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除访客管理
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
