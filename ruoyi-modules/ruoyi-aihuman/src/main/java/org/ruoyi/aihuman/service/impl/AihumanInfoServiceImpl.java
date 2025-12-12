package org.ruoyi.aihuman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.ruoyi.aihuman.domain.AihumanInfo;
import org.ruoyi.aihuman.domain.vo.AihumanInfoVo;
import org.ruoyi.aihuman.mapper.AihumanInfoMapper;
import org.ruoyi.aihuman.service.IAihumanInfoService;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * AI人类交互信息Service业务层处理
 *
 * @author QingYunAI
 */
@RequiredArgsConstructor
@Service
public class AihumanInfoServiceImpl implements IAihumanInfoService {

    private final AihumanInfoMapper baseMapper;

    /**
     * 查询AI人类交互信息
     */
    @Override
    public AihumanInfoVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询AI人类交互信息列表
     */
    @Override
    public TableDataInfo<AihumanInfoVo> queryPageList(AihumanInfo record, PageQuery pageQuery) {
        LambdaQueryWrapper<AihumanInfo> lqw = buildQueryWrapper(record);
        Page<AihumanInfoVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询AI人类交互信息列表
     */
    @Override
    public List<AihumanInfoVo> queryList(AihumanInfo record) {
        LambdaQueryWrapper<AihumanInfo> lqw = buildQueryWrapper(record);
        return baseMapper.selectVoList(lqw);
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<AihumanInfo> buildQueryWrapper(AihumanInfo record) {
        LambdaQueryWrapper<AihumanInfo> lqw = Wrappers.lambdaQuery();
        lqw.eq(record.getId() != null, AihumanInfo::getId, record.getId());
        lqw.like(StringUtils.isNotBlank(record.getName()), AihumanInfo::getName, record.getName());
        lqw.like(StringUtils.isNotBlank(record.getContent()), AihumanInfo::getContent, record.getContent());
        lqw.orderByDesc(AihumanInfo::getCreateTime);
        return lqw;
    }

    /**
     * 新增AI人类交互信息
     */
    @Override
    public int insert(AihumanInfo record) {
        return baseMapper.insert(record);
    }

    /**
     * 修改AI人类交互信息
     */
    @Override
    public int update(AihumanInfo record) {
        return baseMapper.updateById(record);
    }

    /**
     * 批量删除AI人类交互信息
     */
    @Override
    public int deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 如果需要逻辑删除，MyBatis-Plus会自动处理
            // 这里的@TableLogic注解已经在实体类中配置
        }
        return baseMapper.deleteBatchIds(ids);
    }
}