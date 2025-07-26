package org.ruoyi.generator.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.generator.service.SchemaService;
import org.ruoyi.generator.domain.Schema;
import org.ruoyi.generator.domain.bo.SchemaBo;
import org.ruoyi.generator.domain.vo.SchemaVo;
import org.ruoyi.generator.event.SchemaAddedEvent;
import org.ruoyi.generator.mapper.SchemaMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 数据模型Service业务层处理
 *
 * @author ruoyi
 */
@RequiredArgsConstructor
@Service
public class SchemaServiceImpl implements SchemaService {

    private final SchemaMapper baseMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 查询数据模型
     */
    @Override
    public SchemaVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询数据模型列表
     */
    @Override
    public TableDataInfo<SchemaVo> queryPageList(SchemaBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<Schema> lqw = buildQueryWrapper(bo);
        Page<SchemaVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询数据模型列表
     */
    @Override
    public List<SchemaVo> queryList(SchemaBo bo) {
        LambdaQueryWrapper<Schema> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<Schema> buildQueryWrapper(SchemaBo bo) {
        LambdaQueryWrapper<Schema> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getSchemaGroupId() != null, Schema::getSchemaGroupId, bo.getSchemaGroupId());
        lqw.like(StringUtils.isNotBlank(bo.getName()), Schema::getName, bo.getName());
        lqw.eq(StringUtils.isNotBlank(bo.getCode()), Schema::getCode, bo.getCode());
        lqw.eq(StringUtils.isNotBlank(bo.getTableName()), Schema::getTableName, bo.getTableName());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), Schema::getStatus, bo.getStatus());
        lqw.orderByAsc(Schema::getSort);
        return lqw;
    }

    /**
     * 新增数据模型
     */
    @Override
    public Boolean insertByBo(SchemaBo bo) {
        Schema add = MapstructUtils.convert(bo, Schema.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());

            // 发布数据模型添加事件，由事件监听器处理字段插入
            if (StringUtils.isNotBlank(bo.getTableName())) {
                eventPublisher.publishEvent(new SchemaAddedEvent(this, add.getId(), bo.getTableName()));
            }
        }
        return flag;
    }

    /**
     * 修改数据模型
     */
    @Override
    public Boolean updateByBo(SchemaBo bo) {
        Schema update = MapstructUtils.convert(bo, Schema.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(Schema entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除数据模型
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 根据表名查询数据模型
     */
    @Override
    public SchemaVo queryByTableName(String tableName) {
        LambdaQueryWrapper<Schema> lqw = Wrappers.lambdaQuery();
        lqw.eq(Schema::getTableName, tableName);
        // 只查询正常状态的模型
        lqw.eq(Schema::getStatus, "0");
        return baseMapper.selectVoOne(lqw);
    }
}