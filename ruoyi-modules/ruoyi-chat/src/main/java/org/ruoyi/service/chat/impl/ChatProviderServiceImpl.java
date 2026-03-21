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
import org.ruoyi.service.chat.IChatProviderService;
import org.springframework.stereotype.Service;
import org.ruoyi.domain.bo.chat.ChatProviderBo;
import org.ruoyi.domain.vo.chat.ChatProviderVo;
import org.ruoyi.domain.entity.chat.ChatProvider;
import org.ruoyi.mapper.chat.ChatProviderMapper;

import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 厂商管理Service业务层处理
 *
 * @author ageerle
 * @date 2025-12-14
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ChatProviderServiceImpl implements IChatProviderService {

    private final ChatProviderMapper baseMapper;

    /**
     * 查询厂商管理
     *
     * @param id 主键
     * @return 厂商管理
     */
    @Override
    public ChatProviderVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 分页查询厂商管理列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 厂商管理分页列表
     */
    @Override
    public TableDataInfo<ChatProviderVo> queryPageList(ChatProviderBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ChatProvider> lqw = buildQueryWrapper(bo);
        Page<ChatProviderVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询符合条件的厂商管理列表
     *
     * @param bo 查询条件
     * @return 厂商管理列表
     */
    @Override
    public List<ChatProviderVo> queryList(ChatProviderBo bo) {
        LambdaQueryWrapper<ChatProvider> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<ChatProvider> buildQueryWrapper(ChatProviderBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<ChatProvider> lqw = Wrappers.lambdaQuery();
        lqw.orderByAsc(ChatProvider::getId);
        lqw.like(StringUtils.isNotBlank(bo.getProviderName()), ChatProvider::getProviderName, bo.getProviderName());
        lqw.eq(StringUtils.isNotBlank(bo.getProviderCode()), ChatProvider::getProviderCode, bo.getProviderCode());
        lqw.eq(StringUtils.isNotBlank(bo.getProviderIcon()), ChatProvider::getProviderIcon, bo.getProviderIcon());
        lqw.eq(StringUtils.isNotBlank(bo.getProviderDesc()), ChatProvider::getProviderDesc, bo.getProviderDesc());
        lqw.eq(StringUtils.isNotBlank(bo.getApiHost()), ChatProvider::getApiHost, bo.getApiHost());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), ChatProvider::getStatus, bo.getStatus());
        lqw.eq(bo.getSortOrder() != null, ChatProvider::getSortOrder, bo.getSortOrder());
        lqw.eq(StringUtils.isNotBlank(bo.getUpdateIp()), ChatProvider::getUpdateIp, bo.getUpdateIp());
        return lqw;
    }

    /**
     * 新增厂商管理
     *
     * @param bo 厂商管理
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(ChatProviderBo bo) {
        ChatProvider add = MapstructUtils.convert(bo, ChatProvider.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改厂商管理
     *
     * @param bo 厂商管理
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(ChatProviderBo bo) {
        ChatProvider update = MapstructUtils.convert(bo, ChatProvider.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(ChatProvider entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 校验并批量删除厂商管理信息
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
