package org.ruoyi.service.chat.impl;

import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.service.chat.IChatModelService;
import org.springframework.stereotype.Service;
import org.ruoyi.domain.bo.chat.ChatModelBo;
import org.ruoyi.domain.vo.chat.ChatModelVo;
import org.ruoyi.domain.entity.chat.ChatModel;
import org.ruoyi.mapper.chat.ChatModelMapper;

import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 模型管理Service业务层处理
 *
 * @author ageerle
 * @date 2025-12-14
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ChatModelServiceImpl implements IChatModelService {

    private final ChatModelMapper baseMapper;

    /**
     * 查询模型管理
     *
     * @param id 主键
     * @return 模型管理
     */
    @Override
    public ChatModelVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 根据模型名称查询模型
     *
     * @param modelName 模型名称
     * @return 模型管理
     */
    @Override
    public ChatModelVo selectModelByName(String modelName) {
        LambdaQueryWrapper<ChatModel> lqw = Wrappers.lambdaQuery();
        lqw.eq(ChatModel::getModelName, modelName);
        lqw.last("LIMIT 1");
        return baseMapper.selectVoOne(lqw);
    }

    /**
     * 分页查询模型管理列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 模型管理分页列表
     */
    @Override
    public TableDataInfo<ChatModelVo> queryPageList(ChatModelBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ChatModel> lqw = buildQueryWrapper(bo);
        Page<ChatModelVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询符合条件的模型管理列表
     *
     * @param bo 查询条件
     * @return 模型管理列表
     */
    @Override
    public List<ChatModelVo> queryList(ChatModelBo bo) {
        LambdaQueryWrapper<ChatModel> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<ChatModel> buildQueryWrapper(ChatModelBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<ChatModel> lqw = Wrappers.lambdaQuery();
        lqw.orderByAsc(ChatModel::getId);
        lqw.eq(StringUtils.isNotBlank(bo.getCategory()), ChatModel::getCategory, bo.getCategory());
        lqw.like(StringUtils.isNotBlank(bo.getModelName()), ChatModel::getModelName, bo.getModelName());
        lqw.like(StringUtils.isNotBlank(bo.getProviderCode()), ChatModel::getProviderCode, bo.getProviderCode());
        lqw.eq(StringUtils.isNotBlank(bo.getModelDescribe()), ChatModel::getModelDescribe, bo.getModelDescribe());
        lqw.eq(bo.getModelPrice() != null, ChatModel::getModelPrice, bo.getModelPrice());
        lqw.eq(StringUtils.isNotBlank(bo.getModelType()), ChatModel::getModelType, bo.getModelType());
        lqw.eq(StringUtils.isNotBlank(bo.getModelShow()), ChatModel::getModelShow, bo.getModelShow());
        lqw.eq(StringUtils.isNotBlank(bo.getModelFree()), ChatModel::getModelFree, bo.getModelFree());
        lqw.eq(bo.getPriority() != null, ChatModel::getPriority, bo.getPriority());
        lqw.eq(StringUtils.isNotBlank(bo.getApiHost()), ChatModel::getApiHost, bo.getApiHost());
        lqw.eq(StringUtils.isNotBlank(bo.getApiKey()), ChatModel::getApiKey, bo.getApiKey());
        return lqw;
    }

    /**
     * 新增模型管理
     *
     * @param bo 模型管理
     * @return 是否新增成功
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
     * 修改模型管理
     *
     * @param bo 模型管理
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(ChatModelBo bo) {
        ChatModel update = MapstructUtils.convert(bo, ChatModel.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(ChatModel entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 校验并批量删除模型管理信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteByIds(ids) > 0;
    }
}
