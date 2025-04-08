package org.ruoyi.service.impl;

import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.ruoyi.domain.ChatAgentManage;
import org.ruoyi.domain.bo.ChatAgentManageBo;
import org.ruoyi.mapper.ChatAgentManageMapper;
import org.ruoyi.service.IChatAgentManageService;
import org.springframework.stereotype.Service;

import org.ruoyi.system.domain.vo.ChatAgentManageVo;


import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 智能体管理Service业务层处理
 *
 * @author ageerle
 * @date 2025-04-08
 */
@RequiredArgsConstructor
@Service
public class ChatAgentManageServiceImpl implements IChatAgentManageService {

    private final ChatAgentManageMapper baseMapper;

    /**
     * 查询智能体管理
     */
    @Override
    public ChatAgentManageVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询智能体管理列表
     */
    @Override
    public TableDataInfo<ChatAgentManageVo> queryPageList(ChatAgentManageBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ChatAgentManage> lqw = buildQueryWrapper(bo);
        Page<ChatAgentManageVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询智能体管理列表
     */
    @Override
    public List<ChatAgentManageVo> queryList(ChatAgentManageBo bo) {
        LambdaQueryWrapper<ChatAgentManage> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<ChatAgentManage> buildQueryWrapper(ChatAgentManageBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<ChatAgentManage> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getAppName()), ChatAgentManage::getAppName, bo.getAppName());
        lqw.eq(StringUtils.isNotBlank(bo.getAppType()), ChatAgentManage::getAppType, bo.getAppType());
        lqw.eq(StringUtils.isNotBlank(bo.getAppIcon()), ChatAgentManage::getAppIcon, bo.getAppIcon());
        lqw.eq(StringUtils.isNotBlank(bo.getAppDescription()), ChatAgentManage::getAppDescription, bo.getAppDescription());
        lqw.eq(StringUtils.isNotBlank(bo.getIntroduction()), ChatAgentManage::getIntroduction, bo.getIntroduction());
        lqw.eq(StringUtils.isNotBlank(bo.getModel()), ChatAgentManage::getModel, bo.getModel());
        lqw.eq(StringUtils.isNotBlank(bo.getConversationModel()), ChatAgentManage::getConversationModel, bo.getConversationModel());
        lqw.eq(StringUtils.isNotBlank(bo.getApplicationSettings()), ChatAgentManage::getApplicationSettings, bo.getApplicationSettings());
        lqw.eq(StringUtils.isNotBlank(bo.getPluginId()), ChatAgentManage::getPluginId, bo.getPluginId());
        lqw.eq(bo.getKnowledgeId() != null, ChatAgentManage::getKnowledgeId, bo.getKnowledgeId());
        return lqw;
    }

    /**
     * 新增智能体管理
     */
    @Override
    public Boolean insertByBo(ChatAgentManageBo bo) {
        ChatAgentManage add = MapstructUtils.convert(bo, ChatAgentManage.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改智能体管理
     */
    @Override
    public Boolean updateByBo(ChatAgentManageBo bo) {
        ChatAgentManage update = MapstructUtils.convert(bo, ChatAgentManage.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(ChatAgentManage entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除智能体管理
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
