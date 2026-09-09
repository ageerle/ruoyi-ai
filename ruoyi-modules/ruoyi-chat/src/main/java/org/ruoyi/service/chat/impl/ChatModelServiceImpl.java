package org.ruoyi.service.chat.impl;

import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.common.chat.domain.bo.chat.ChatModelBo;
import org.ruoyi.common.chat.entity.chat.ChatModel;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.security.ChatModelCredentialPolicy;
import org.ruoyi.common.chat.security.ChatModelSecretReference;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ruoyi.mapper.chat.ChatModelMapper;
import org.ruoyi.domain.bo.chat.ChatProviderBo;
import org.ruoyi.domain.vo.chat.ChatProviderVo;
import org.ruoyi.service.chat.IChatProviderService;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

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
    private final IChatProviderService chatProviderService;

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
        ChatModelVo model = baseMapper.selectVoOne(lqw);
        if (model != null) {
            chatProviderService.requireEnabled(model.getProviderCode());
        }
        return model;
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

    @Override
    public List<ChatModelVo> queryAvailableList(ChatModelBo bo) {
        ChatProviderBo providerQuery = new ChatProviderBo();
        providerQuery.setStatus("0");
        List<String> providerCodes = chatProviderService.queryList(providerQuery).stream()
            .map(ChatProviderVo::getProviderCode)
            .distinct()
            .toList();
        if (providerCodes.isEmpty()) {
            return List.of();
        }
        return baseMapper.selectVoList(buildQueryWrapper(bo)
            .in(ChatModel::getProviderCode, providerCodes));
    }

    private LambdaQueryWrapper<ChatModel> buildQueryWrapper(ChatModelBo bo) {
        LambdaQueryWrapper<ChatModel> lqw = Wrappers.lambdaQuery();
        lqw.orderByAsc(ChatModel::getId);
        lqw.eq(StringUtils.isNotBlank(bo.getCategory()), ChatModel::getCategory, bo.getCategory());
        lqw.like(StringUtils.isNotBlank(bo.getModelName()), ChatModel::getModelName, bo.getModelName());
        lqw.like(StringUtils.isNotBlank(bo.getProviderCode()), ChatModel::getProviderCode, bo.getProviderCode());
        lqw.eq(StringUtils.isNotBlank(bo.getModelDescribe()), ChatModel::getModelDescribe, bo.getModelDescribe());
        lqw.eq(StringUtils.isNotBlank(bo.getModelShow()), ChatModel::getModelShow, bo.getModelShow());
        lqw.eq(StringUtils.isNotBlank(bo.getApiHost()), ChatModel::getApiHost, bo.getApiHost());
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
        ChatModel add = toEntity(bo);
        validateEffectiveConfiguration(add);
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
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(ChatModelBo bo) {
        ChatModel update = toEntity(bo);
        validateRequestedFields(update);
        ChatModel current = selectByIdForUpdate(update.getId());
        if (current == null) {
            return false;
        }
        validateEffectiveConfiguration(mergeEffectiveConfiguration(current, update));
        return baseMapper.updateById(update) > 0;
    }

    private void validateRequestedFields(ChatModel requested) {
        if (requested.getApiKey() != null) {
            ChatModelSecretReference.requirePersistableReference(requested.getApiKey());
        }
        if (requested.getApiHost() != null) {
            ChatModelCredentialPolicy.requireSecureApiHost(requested.getApiHost());
        }
    }

    private void validateEffectiveConfiguration(ChatModel entity) {
        ChatModelCredentialPolicy.requirePersistableConfiguration(
            entity.getProviderCode(), entity.getModelName(), entity.getApiHost(), entity.getApiKey());
        chatProviderService.requireEnabled(entity.getProviderCode());
    }

    private ChatModel selectByIdForUpdate(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Model id is required for update");
        }
        LambdaQueryWrapper<ChatModel> lock = Wrappers.lambdaQuery();
        lock.eq(ChatModel::getId, id).last("FOR UPDATE");
        return baseMapper.selectOne(lock);
    }

    private ChatModel mergeEffectiveConfiguration(ChatModel current, ChatModel requested) {
        ChatModel effective = new ChatModel();
        effective.setProviderCode(firstNonNull(requested.getProviderCode(), current.getProviderCode()));
        effective.setModelName(firstNonNull(requested.getModelName(), current.getModelName()));
        effective.setApiHost(firstNonNull(requested.getApiHost(), current.getApiHost()));
        effective.setApiKey(firstNonNull(requested.getApiKey(), current.getApiKey()));
        return effective;
    }

    private static <T> T firstNonNull(T requested, T current) {
        return requested == null ? current : requested;
    }

    private ChatModel toEntity(ChatModelBo source) {
        if (source == null) {
            throw new IllegalArgumentException("Model command is required");
        }
        ChatModel target = new ChatModel();
        target.setSearchValue(source.getSearchValue());
        target.setCreateDept(source.getCreateDept());
        target.setCreateBy(source.getCreateBy());
        target.setCreateTime(source.getCreateTime());
        target.setUpdateBy(source.getUpdateBy());
        target.setUpdateTime(source.getUpdateTime());
        if (source.getParams() != null) {
            target.setParams(new LinkedHashMap<>(source.getParams()));
        }
        target.setId(source.getId());
        target.setCategory(source.getCategory());
        target.setModelName(source.getModelName());
        target.setProviderCode(source.getProviderCode());
        target.setModelDescribe(source.getModelDescribe());
        target.setModelShow(source.getModelShow());
        target.setModelDimension(source.getModelDimension());
        target.setApiHost(source.getApiHost());
        target.setApiKey(source.getApiKey());
        target.setRemark(source.getRemark());
        return target;
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

    /**
     * 按厂商批量更新密钥
     *
     * @param providerCode 厂商编码
     * @param apiKey       密钥
     * @return 是否更新成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateApiKeyByProvider(String providerCode, String apiKey) {
        if (apiKey == null) {
            throw new IllegalArgumentException("Batch model key reference is required");
        }
        String reference = ChatModelSecretReference.requirePersistableReference(apiKey);
        ChatModelCredentialPolicy.requireProviderReference(providerCode, reference);
        LambdaQueryWrapper<ChatModel> lock = Wrappers.lambdaQuery();
        lock.select(ChatModel::getId, ChatModel::getProviderCode, ChatModel::getModelName, ChatModel::getApiHost)
            .eq(ChatModel::getProviderCode, providerCode)
            .last("FOR UPDATE");
        List<ChatModel> current = baseMapper.selectList(lock);
        if (current.isEmpty()) {
            return false;
        }
        current.forEach(model -> ChatModelCredentialPolicy.requireTrustedConfiguration(
            model.getProviderCode(), model.getModelName(), model.getApiHost(), reference));
        LambdaUpdateWrapper<ChatModel> uw = Wrappers.lambdaUpdate();
        uw.set(ChatModel::getApiKey, reference)
            .eq(ChatModel::getProviderCode, providerCode)
            .in(ChatModel::getId, current.stream().map(ChatModel::getId).toList());
        return baseMapper.update(null, uw) > 0;
    }
}
