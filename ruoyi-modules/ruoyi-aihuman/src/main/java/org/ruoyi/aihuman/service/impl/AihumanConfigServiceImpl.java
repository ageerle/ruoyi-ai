package org.ruoyi.aihuman.service.impl;

import org.ruoyi.common.core.utils.MapstructUtils;
    import org.ruoyi.core.page.TableDataInfo;
    import org.ruoyi.core.page.PageQuery;
    import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.ruoyi.aihuman.domain.bo.AihumanConfigBo;
import org.ruoyi.aihuman.domain.vo.AihumanConfigVo;
import org.ruoyi.aihuman.domain.AihumanConfig;
import org.ruoyi.aihuman.mapper.AihumanConfigMapper;
import org.ruoyi.aihuman.service.AihumanConfigService;
import org.ruoyi.common.core.utils.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 交互数字人配置Service业务层处理
 *
 * @author ageerle
 * @date Fri Sep 26 22:27:00 GMT+08:00 2025
 */
@RequiredArgsConstructor
@Service
public class AihumanConfigServiceImpl implements AihumanConfigService {

    private final AihumanConfigMapper baseMapper;

    /**
     * 查询交互数字人配置
     */
    @Override
    public AihumanConfigVo queryById(Integer id) {
        return baseMapper.selectVoById(id);
    }

        /**
         * 查询交互数字人配置列表
         */
        @Override
        public TableDataInfo<AihumanConfigVo> queryPageList(AihumanConfigBo bo, PageQuery pageQuery) {
            LambdaQueryWrapper<AihumanConfig> lqw = buildQueryWrapper(bo);
            Page<AihumanConfigVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
            return TableDataInfo.build(result);
        }

    /**
     * 查询交互数字人配置列表
     */
    @Override
    public List<AihumanConfigVo> queryList(AihumanConfigBo bo) {
        LambdaQueryWrapper<AihumanConfig> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<AihumanConfig> buildQueryWrapper(AihumanConfigBo bo) {
        LambdaQueryWrapper<AihumanConfig> lqw = Wrappers.lambdaQuery();
                    lqw.eq(StringUtils.isNotBlank(bo.getName()), AihumanConfig::getName, bo.getName());
                    lqw.eq(StringUtils.isNotBlank(bo.getModelName()), AihumanConfig::getModelName, bo.getModelName());
                    lqw.eq(StringUtils.isNotBlank(bo.getModelPath()), AihumanConfig::getModelPath, bo.getModelPath());
                    lqw.eq(StringUtils.isNotBlank(bo.getModelParams()), AihumanConfig::getModelParams, bo.getModelParams());
                    lqw.eq(StringUtils.isNotBlank(bo.getAgentParams()), AihumanConfig::getAgentParams, bo.getAgentParams());
                    lqw.eq(bo.getCreateTime() != null, AihumanConfig::getCreateTime, bo.getCreateTime());
                    lqw.eq(bo.getUpdateTime() != null, AihumanConfig::getUpdateTime, bo.getUpdateTime());
                    lqw.eq(bo.getStatus() != null, AihumanConfig::getStatus, bo.getStatus());
                    lqw.eq(bo.getPublish() != null, AihumanConfig::getPublish, bo.getPublish());
        return lqw;
    }

    /**
     * 新增交互数字人配置
     */
    @Override
    public Boolean insertByBo(AihumanConfigBo bo) {
        AihumanConfig add = MapstructUtils.convert(bo, AihumanConfig. class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改交互数字人配置
     */
    @Override
    public Boolean updateByBo(AihumanConfigBo bo) {
        AihumanConfig update = MapstructUtils.convert(bo, AihumanConfig. class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(AihumanConfig entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除交互数字人配置
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Integer> ids, Boolean isValid) {
        if (isValid) {
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
