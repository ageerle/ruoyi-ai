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
import org.ruoyi.service.chat.IChatConfigService;
import org.springframework.stereotype.Service;
import org.ruoyi.domain.bo.chat.ChatConfigBo;
import org.ruoyi.domain.vo.chat.ChatConfigVo;
import org.ruoyi.domain.entity.chat.ChatConfig;
import org.ruoyi.mapper.chat.ChatConfigMapper;

import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 配置信息Service业务层处理
 *
 * @author ageerle
 * @date 2025-12-14
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ChatConfigServiceImpl implements IChatConfigService {

    private final ChatConfigMapper baseMapper;

    /**
     * 查询配置信息
     *
     * @param id 主键
     * @return 配置信息
     */
    @Override
    public ChatConfigVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 分页查询配置信息列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 配置信息分页列表
     */
    @Override
    public TableDataInfo<ChatConfigVo> queryPageList(ChatConfigBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ChatConfig> lqw = buildQueryWrapper(bo);
        Page<ChatConfigVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询符合条件的配置信息列表
     *
     * @param bo 查询条件
     * @return 配置信息列表
     */
    @Override
    public List<ChatConfigVo> queryList(ChatConfigBo bo) {
        LambdaQueryWrapper<ChatConfig> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<ChatConfig> buildQueryWrapper(ChatConfigBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<ChatConfig> lqw = Wrappers.lambdaQuery();
        lqw.orderByAsc(ChatConfig::getId);
        lqw.eq(StringUtils.isNotBlank(bo.getCategory()), ChatConfig::getCategory, bo.getCategory());
        lqw.like(StringUtils.isNotBlank(bo.getConfigName()), ChatConfig::getConfigName, bo.getConfigName());
        lqw.eq(StringUtils.isNotBlank(bo.getConfigValue()), ChatConfig::getConfigValue, bo.getConfigValue());
        lqw.eq(StringUtils.isNotBlank(bo.getConfigDict()), ChatConfig::getConfigDict, bo.getConfigDict());
        lqw.eq(StringUtils.isNotBlank(bo.getUpdateIp()), ChatConfig::getUpdateIp, bo.getUpdateIp());
        return lqw;
    }

    /**
     * 新增配置信息
     *
     * @param bo 配置信息
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(ChatConfigBo bo) {
        ChatConfig add = MapstructUtils.convert(bo, ChatConfig.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改配置信息
     *
     * @param bo 配置信息
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(ChatConfigBo bo) {
        ChatConfig update = MapstructUtils.convert(bo, ChatConfig.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(ChatConfig entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 校验并批量删除配置信息信息
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
