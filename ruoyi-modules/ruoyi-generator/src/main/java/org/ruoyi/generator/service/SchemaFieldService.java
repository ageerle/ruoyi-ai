package org.ruoyi.generator.service;

import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.generator.domain.bo.SchemaFieldBo;
import org.ruoyi.generator.domain.vo.SchemaFieldVo;

import java.util.Collection;
import java.util.List;

/**
 * 数据模型字段Service接口
 *
 * @author ruoyi
 * @date 2024-01-01
 */
public interface SchemaFieldService {

    /**
     * 查询数据模型字段
     */
    SchemaFieldVo queryById(Long id);

    /**
     * 查询数据模型字段列表
     */
    TableDataInfo<SchemaFieldVo> queryPageList(SchemaFieldBo bo, PageQuery pageQuery);

    /**
     * 新增数据模型字段
     */
    Boolean insertByBo(SchemaFieldBo bo);

    /**
     * 修改数据模型字段
     */
    Boolean updateByBo(SchemaFieldBo bo);

    /**
     * 校验并批量删除数据模型字段信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 批量更新字段配置
     */
    Boolean batchUpdateFieldConfig(List<SchemaFieldBo> fields);

    /**
     * 根据模型ID查询字段列表
     */
    List<SchemaFieldVo> queryListBySchemaId(Long schemaId);

    /**
     * 根据表名获取代码生成元数据
     */
    Object getMetaDataByTableName(String tableName);

    /**
     * 根据表名批量插入字段
     *
     * @param schemaId  模型ID
     * @param tableName 表名
     */
    boolean batchInsertFieldsByTableName(Long schemaId, String tableName);
}