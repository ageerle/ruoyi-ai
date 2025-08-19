package org.ruoyi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.domain.ChatModel;
import org.ruoyi.domain.bo.ChatModelBo;
import org.ruoyi.domain.vo.ChatModelVo;
import org.ruoyi.mapper.ChatModelMapper;
import org.ruoyi.service.IChatModelService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 聊天模型Service业务层处理
 *
 * @author ageerle
 * @date 2025-04-08
 */
@RequiredArgsConstructor
@Service
public class ChatModelServiceImpl implements IChatModelService {

    private final ChatModelMapper baseMapper;


    /**
     * 查询聊天模型
     */
    @Override
    public ChatModelVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询聊天模型列表
     */
    @Override
    public TableDataInfo<ChatModelVo> queryPageList(ChatModelBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ChatModel> lqw = buildQueryWrapper(bo);
        Page<ChatModelVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询聊天模型列表
     */
    @Override
    public List<ChatModelVo> queryList(ChatModelBo bo) {
        LambdaQueryWrapper<ChatModel> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<ChatModel> buildQueryWrapper(ChatModelBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<ChatModel> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getCategory()), ChatModel::getCategory, bo.getCategory());
        lqw.like(StringUtils.isNotBlank(bo.getModelName()), ChatModel::getModelName, bo.getModelName());
        lqw.eq(StringUtils.isNotBlank(bo.getModelDescribe()), ChatModel::getModelDescribe, bo.getModelDescribe());
        lqw.eq(bo.getModelPrice() != null, ChatModel::getModelPrice, bo.getModelPrice());
        lqw.eq(StringUtils.isNotBlank(bo.getModelType()), ChatModel::getModelType, bo.getModelType());
        lqw.eq(StringUtils.isNotBlank(bo.getModelShow()), ChatModel::getModelShow, bo.getModelShow());
        lqw.eq(StringUtils.isNotBlank(bo.getSystemPrompt()), ChatModel::getSystemPrompt, bo.getSystemPrompt());
        lqw.eq(StringUtils.isNotBlank(bo.getApiHost()), ChatModel::getApiHost, bo.getApiHost());
        lqw.eq(StringUtils.isNotBlank(bo.getApiKey()), ChatModel::getApiKey, bo.getApiKey());
        return lqw;
    }

    /**
     * 新增聊天模型
     */
    @Override
    public Boolean insertByBo(ChatModelBo bo) {
        ChatModel add = MapstructUtils.convert(bo, ChatModel.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改聊天模型
     */
    @Override
    public Boolean updateByBo(ChatModelBo bo) {
        ChatModel update = MapstructUtils.convert(bo, ChatModel.class);
        if (update != null) {
            validEntityBeforeSave(update);
        }
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(ChatModel entity){
        // 判断是否包含*号
        if (entity.getApiKey().contains("*")) {
            // 重新设置key信息
            entity.setApiKey(baseMapper.selectById(entity.getId()).getApiKey());
        }
    }

    /**
     * 批量删除聊天模型
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 通过模型名称获取模型信息
     */
    @Override
    public ChatModelVo selectModelByName(String modelName) {
        return baseMapper.selectVoOne(Wrappers.<ChatModel>lambdaQuery().eq(ChatModel::getModelName, modelName));
    }
    /**
     * 通过模型分类获取模型信息
     */
    @Override
    public ChatModelVo selectModelByCategory(String  category) {
        return baseMapper.selectVoOne(Wrappers.<ChatModel>lambdaQuery().eq(ChatModel::getCategory, category));
    }
    
    /**
     * 通过模型分类获取优先级最高的模型信息
     */
    @Override
    public ChatModelVo selectModelByCategoryWithHighestPriority(String category) {
        return baseMapper.selectVoOne(
            Wrappers.<ChatModel>lambdaQuery()
                .eq(ChatModel::getCategory, category)
                .orderByDesc(ChatModel::getPriority)
                .last("LIMIT 1")
        );
    }

    /**
     * 在同一分类下，查找优先级小于当前优先级的最高优先级模型（用于降级）。
     */
    @Override
    public ChatModelVo selectFallbackModelByCategoryAndLessPriority(String category, Integer currentPriority) {
        return baseMapper.selectVoOne(
            Wrappers.<ChatModel>lambdaQuery()
                .eq(ChatModel::getCategory, category)
                .lt(ChatModel::getPriority, currentPriority)
                .orderByDesc(ChatModel::getPriority)
                .last("LIMIT 1")
        );
    }

    @Override
    public ChatModel getPPT() {
        return baseMapper.selectOne(Wrappers.<ChatModel>lambdaQuery().eq(ChatModel::getModelName, "ppt"));
    }
}
