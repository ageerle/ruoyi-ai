package org.ruoyi.generator.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.anyline.metadata.Column;
import org.anyline.metadata.Table;
import org.anyline.proxy.ServiceProxy;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.ruoyi.common.core.constant.Constants;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.common.core.utils.StreamUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.core.utils.file.FileUtils;
import org.ruoyi.common.json.utils.JsonUtils;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.generator.constant.GenConstants;
import org.ruoyi.generator.domain.GenTable;
import org.ruoyi.generator.domain.GenTableColumn;
import org.ruoyi.generator.mapper.GenTableColumnMapper;
import org.ruoyi.generator.mapper.GenTableMapper;
import org.ruoyi.generator.util.GenUtils;
import org.ruoyi.generator.util.VelocityInitializer;
import org.ruoyi.generator.util.VelocityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 业务 服务层实现
 *
 * @author Lion Li
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class GenTableServiceImpl implements IGenTableService {

    private final GenTableMapper baseMapper;
    private final GenTableColumnMapper genTableColumnMapper;
    private final IdentifierGenerator identifierGenerator;

    private static final String[] TABLE_IGNORE = new String[]{"sj_", "flow_", "gen_"};

    /**
     * 查询业务字段列表
     *
     * @param tableId 业务字段编号
     * @return 业务字段集合
     */
    @Override
    public List<GenTableColumn> selectGenTableColumnListByTableId(Long tableId) {
        return genTableColumnMapper.selectList(new LambdaQueryWrapper<GenTableColumn>()
            .eq(GenTableColumn::getTableId, tableId)
            .orderByAsc(GenTableColumn::getSort));
    }

    /**
     * 查询业务信息
     *
     * @param id 业务ID
     * @return 业务信息
     */
    @Override
    public GenTable selectGenTableById(Long id) {
        GenTable genTable = baseMapper.selectGenTableById(id);
        setTableFromOptions(genTable);
        return genTable;
    }

    @Override
    public TableDataInfo<GenTable> selectPageGenTableList(GenTable genTable, PageQuery pageQuery) {
        Page<GenTable> page = baseMapper.selectPage(pageQuery.build(), this.buildGenTableQueryWrapper(genTable));
        return TableDataInfo.build(page);
    }

    private QueryWrapper<GenTable> buildGenTableQueryWrapper(GenTable genTable) {
        Map<String, Object> params = genTable.getParams();
        QueryWrapper<GenTable> wrapper = Wrappers.query();
        wrapper
            .eq(StringUtils.isNotEmpty(genTable.getDataName()), "data_name", genTable.getDataName())
            .like(StringUtils.isNotBlank(genTable.getTableName()), "lower(table_name)", StringUtils.lowerCase(genTable.getTableName()))
            .like(StringUtils.isNotBlank(genTable.getTableComment()), "lower(table_comment)", StringUtils.lowerCase(genTable.getTableComment()))
            .between(params.get("beginTime") != null && params.get("endTime") != null,
                "create_time", params.get("beginTime"), params.get("endTime"))
            .orderByDesc("update_time");
        return wrapper;
    }

    /**
     * 查询数据库列表
     *
     * @param genTable  包含查询条件的GenTable对象
     * @param pageQuery 包含分页信息的PageQuery对象
     * @return 包含分页结果的TableDataInfo对象
     */
    @DS("#genTable.dataName")
    @Override
    public TableDataInfo<GenTable> selectPageDbTableList(GenTable genTable, PageQuery pageQuery) {
        // 获取查询条件
        String tableName = genTable.getTableName();
        String tableComment = genTable.getTableComment();

        LinkedHashMap<String, Table<?>> tablesMap = ServiceProxy.metadata().tables();
        if (CollUtil.isEmpty(tablesMap)) {
            return TableDataInfo.build();
        }
        List<String> tableNames = baseMapper.selectTableNameList(genTable.getDataName());
        String[] tableArrays;
        if (CollUtil.isNotEmpty(tableNames)) {
            tableArrays = tableNames.toArray(new String[0]);
        } else {
            tableArrays = new String[0];
        }
        // 过滤并转换表格数据
        List<GenTable> tables = tablesMap.values().stream()
            .filter(x -> !StringUtils.startWithAnyIgnoreCase(x.getName(), TABLE_IGNORE))
            .filter(x -> {
                if (CollUtil.isEmpty(tableNames)) {
                    return true;
                }
                return !StringUtils.equalsAnyIgnoreCase(x.getName(), tableArrays);
            })
            .filter(x -> {
                boolean nameMatches = true;
                boolean commentMatches = true;
                // 进行表名称的模糊查询
                if (StringUtils.isNotBlank(tableName)) {
                    nameMatches = StringUtils.containsIgnoreCase(x.getName(), tableName);
                }
                // 进行表描述的模糊查询
                if (StringUtils.isNotBlank(tableComment)) {
                    commentMatches = StringUtils.containsIgnoreCase(x.getComment(), tableComment);
                }
                // 同时匹配名称和描述
                return nameMatches && commentMatches;
            })
            .map(x -> {
                GenTable gen = new GenTable();
                gen.setTableName(x.getName());
                gen.setTableComment(x.getComment());
                // postgresql的表元数据没有创建时间这个东西(好奇葩) 只能new Date代替
                gen.setCreateTime(ObjectUtil.defaultIfNull(x.getCreateTime(), new Date()));
                gen.setUpdateTime(x.getUpdateTime());
                return gen;
            }).sorted(Comparator.comparing(GenTable::getCreateTime).reversed())
            .toList();
        return TableDataInfo.build(tables, pageQuery.build());
    }

    /**
     * 查询据库列表
     *
     * @param tableNames 表名称组
     * @param dataName   数据源名称
     * @return 数据库表集合
     */
    @DS("#dataName")
    @Override
    public List<GenTable> selectDbTableListByNames(String[] tableNames, String dataName) {
        Set<String> tableNameSet = new HashSet<>(List.of(tableNames));
        LinkedHashMap<String, Table<?>> tablesMap = ServiceProxy.metadata().tables();

        if (CollUtil.isEmpty(tablesMap)) {
            return new ArrayList<>();
        }

        List<Table<?>> tableList = tablesMap.values().stream()
            .filter(x -> !StringUtils.startWithAnyIgnoreCase(x.getName(), TABLE_IGNORE))
            .filter(x -> tableNameSet.contains(x.getName())).toList();

        if (CollUtil.isEmpty(tableList)) {
            return new ArrayList<>();
        }
        return tableList.stream().map(x -> {
            GenTable gen = new GenTable();
            gen.setDataName(dataName);
            gen.setTableName(x.getName());
            gen.setTableComment(x.getComment());
            gen.setCreateTime(x.getCreateTime());
            gen.setUpdateTime(x.getUpdateTime());
            return gen;
        }).toList();
    }

    /**
     * 查询所有表信息
     *
     * @return 表信息集合
     */
    @Override
    public List<GenTable> selectGenTableAll() {
        return baseMapper.selectGenTableAll();
    }

    /**
     * 修改业务
     *
     * @param genTable 业务信息
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateGenTable(GenTable genTable) {
        String options = JsonUtils.toJsonString(genTable.getParams());
        genTable.setOptions(options);
        int row = baseMapper.updateById(genTable);
        if (row > 0) {
            for (GenTableColumn cenTableColumn : genTable.getColumns()) {
                genTableColumnMapper.updateById(cenTableColumn);
            }
        }
    }

    /**
     * 删除业务对象
     *
     * @param tableIds 需要删除的数据ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteGenTableByIds(Long[] tableIds) {
        List<Long> ids = Arrays.asList(tableIds);
        baseMapper.deleteByIds(ids);
        genTableColumnMapper.delete(new LambdaQueryWrapper<GenTableColumn>().in(GenTableColumn::getTableId, ids));
    }

    /**
     * 导入表结构
     *
     * @param tableList 导入表列表
     * @param dataName  数据源名称
     */
    @DSTransactional
    @Override
    public void importGenTable(List<GenTable> tableList, String dataName) {
        try {
            for (GenTable table : tableList) {
                String tableName = table.getTableName();
                GenUtils.initTable(table);
                table.setDataName(dataName);
                int row = baseMapper.insert(table);
                if (row > 0) {
                    // 保存列信息
                    List<GenTableColumn> genTableColumns = SpringUtils.getAopProxy(this).selectDbTableColumnsByName(tableName, dataName);
                    List<GenTableColumn> saveColumns = new ArrayList<>();
                    for (GenTableColumn column : genTableColumns) {
                        GenUtils.initColumnField(column, table);
                        saveColumns.add(column);
                    }
                    if (CollUtil.isNotEmpty(saveColumns)) {
                        genTableColumnMapper.insertBatch(saveColumns);
                    }
                }
            }
        } catch (Exception e) {
            throw new ServiceException("导入失败：" + e.getMessage());
        }
    }

    /**
     * 根据表名称查询列信息
     *
     * @param tableName 表名称
     * @param dataName  数据源名称
     * @return 列信息
     */
    @DS("#dataName")
    @Override
    public List<GenTableColumn> selectDbTableColumnsByName(String tableName, String dataName) {
        Table<?> table = ServiceProxy.metadata().table(tableName);
        if (ObjectUtil.isNull(table)) {
            return new ArrayList<>();
        }
        LinkedHashMap<String, Column> columns = table.getColumns();
        List<GenTableColumn> tableColumns = new ArrayList<>();
        columns.forEach((columnName, column) -> {
            GenTableColumn tableColumn = new GenTableColumn();
            tableColumn.setIsPk(column.isPrimaryKey() ? "1" : "0");
            tableColumn.setColumnName(column.getName());
            tableColumn.setColumnComment(column.getComment());
            tableColumn.setColumnType(column.getOriginType().toLowerCase());
            tableColumn.setSort(column.getPosition());
            tableColumn.setIsRequired(column.isNullable() ? "0" : "1");
            tableColumn.setIsIncrement(column.isAutoIncrement() ? "1" : "0");
            tableColumns.add(tableColumn);
        });
        return tableColumns;
    }

    /**
     * 预览代码
     *
     * @param tableId 表编号
     * @return 预览数据列表
     */
    @Override
    public Map<String, String> previewCode(Long tableId) {
        Map<String, String> dataMap = new LinkedHashMap<>();
        // 查询表信息
        GenTable table = baseMapper.selectGenTableById(tableId);
        List<Long> menuIds = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            menuIds.add(identifierGenerator.nextId(null).longValue());
        }
        table.setMenuIds(menuIds);
        // 设置主键列信息
        setPkColumn(table);
        VelocityInitializer.initVelocity();

        VelocityContext context = VelocityUtils.prepareContext(table);

        // 获取模板列表
        List<String> templates = VelocityUtils.getTemplateList(table.getTplCategory());
        for (String template : templates) {
            // 渲染模板
            StringWriter sw = new StringWriter();
            Template tpl = Velocity.getTemplate(template, Constants.UTF8);
            tpl.merge(context, sw);
            dataMap.put(template, sw.toString());
        }
        return dataMap;
    }

    /**
     * 生成代码（下载方式）
     *
     * @param tableId 表名称
     * @return 数据
     */
    @Override
    public byte[] downloadCode(Long tableId) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(outputStream);
        generatorCode(tableId, zip);
        IoUtil.close(zip);
        return outputStream.toByteArray();
    }

    /**
     * 生成代码（自定义路径）
     *
     * @param tableId 表名称
     */
    @Override
    public void generatorCode(Long tableId) {
        // 查询表信息
        GenTable table = baseMapper.selectGenTableById(tableId);
        // 设置主键列信息
        setPkColumn(table);

        VelocityInitializer.initVelocity();

        VelocityContext context = VelocityUtils.prepareContext(table);

        // 获取模板列表
        List<String> templates = VelocityUtils.getTemplateList(table.getTplCategory());
        for (String template : templates) {
            if (!StringUtils.containsAny(template, "sql.vm", "api.ts.vm", "types.ts.vm", "index.vue.vm", "index-tree.vue.vm")) {
                // 渲染模板
                StringWriter sw = new StringWriter();
                Template tpl = Velocity.getTemplate(template, Constants.UTF8);
                tpl.merge(context, sw);
                try {
                    String path = getGenPath(table, template);
                    FileUtils.writeUtf8String(sw.toString(), path);
                } catch (Exception e) {
                    throw new ServiceException("渲染模板失败，表名：" + table.getTableName());
                }
            }
        }
    }

    /**
     * 同步数据库
     *
     * @param tableId 表名称
     */
    @DSTransactional
    @Override
    public void synchDb(Long tableId) {
        GenTable table = baseMapper.selectGenTableById(tableId);
        List<GenTableColumn> tableColumns = table.getColumns();
        Map<String, GenTableColumn> tableColumnMap = StreamUtils.toIdentityMap(tableColumns, GenTableColumn::getColumnName);

        List<GenTableColumn> dbTableColumns = SpringUtils.getAopProxy(this).selectDbTableColumnsByName(table.getTableName(), table.getDataName());
        if (CollUtil.isEmpty(dbTableColumns)) {
            throw new ServiceException("同步数据失败，原表结构不存在");
        }
        List<String> dbTableColumnNames = StreamUtils.toList(dbTableColumns, GenTableColumn::getColumnName);

        List<GenTableColumn> saveColumns = new ArrayList<>();
        dbTableColumns.forEach(column -> {
            GenUtils.initColumnField(column, table);
            if (tableColumnMap.containsKey(column.getColumnName())) {
                GenTableColumn prevColumn = tableColumnMap.get(column.getColumnName());
                column.setColumnId(prevColumn.getColumnId());
                if (column.isList()) {
                    // 如果是列表，继续保留查询方式/字典类型选项
                    column.setDictType(prevColumn.getDictType());
                    column.setQueryType(prevColumn.getQueryType());
                }
                if (StringUtils.isNotEmpty(prevColumn.getIsRequired()) && !column.isPk()
                    && (column.isInsert() || column.isEdit())
                    && ((column.isUsableColumn()) || (!column.isSuperColumn()))) {
                    // 如果是(新增/修改&非主键/非忽略及父属性)，继续保留必填/显示类型选项
                    column.setIsRequired(prevColumn.getIsRequired());
                    column.setHtmlType(prevColumn.getHtmlType());
                }
            }
            saveColumns.add(column);
        });
        if (CollUtil.isNotEmpty(saveColumns)) {
            genTableColumnMapper.insertOrUpdateBatch(saveColumns);
        }
        List<GenTableColumn> delColumns = StreamUtils.filter(tableColumns, column -> !dbTableColumnNames.contains(column.getColumnName()));
        if (CollUtil.isNotEmpty(delColumns)) {
            List<Long> ids = StreamUtils.toList(delColumns, GenTableColumn::getColumnId);
            if (CollUtil.isNotEmpty(ids)) {
                genTableColumnMapper.deleteByIds(ids);
            }
        }
    }

    /**
     * 批量生成代码（下载方式）
     *
     * @param tableIds 表ID数组
     * @return 数据
     */
    @Override
    public byte[] downloadCode(String[] tableIds) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(outputStream);
        for (String tableId : tableIds) {
            generatorCode(Long.parseLong(tableId), zip);
        }
        IoUtil.close(zip);
        return outputStream.toByteArray();
    }

    /**
     * 查询表信息并生成代码
     */
    private void generatorCode(Long tableId, ZipOutputStream zip) {
        // 查询表信息
        GenTable table = baseMapper.selectGenTableById(tableId);
        List<Long> menuIds = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            menuIds.add(identifierGenerator.nextId(null).longValue());
        }
        table.setMenuIds(menuIds);
        // 设置主键列信息
        setPkColumn(table);

        VelocityInitializer.initVelocity();

        VelocityContext context = VelocityUtils.prepareContext(table);

        // 获取模板列表
        List<String> templates = VelocityUtils.getTemplateList(table.getTplCategory());
        for (String template : templates) {
            // 渲染模板
            StringWriter sw = new StringWriter();
            Template tpl = Velocity.getTemplate(template, Constants.UTF8);
            tpl.merge(context, sw);
            try {
                // 添加到zip
                zip.putNextEntry(new ZipEntry(VelocityUtils.getFileName(template, table)));
                IoUtil.write(zip, StandardCharsets.UTF_8, false, sw.toString());
                IoUtil.close(sw);
                zip.flush();
                zip.closeEntry();
            } catch (IOException e) {
                log.error("渲染模板失败，表名：" + table.getTableName(), e);
            }
        }
    }

    /**
     * 修改保存参数校验
     *
     * @param genTable 业务信息
     */
    @Override
    public void validateEdit(GenTable genTable) {
        if (GenConstants.TPL_TREE.equals(genTable.getTplCategory())) {
            String options = JsonUtils.toJsonString(genTable.getParams());
            Dict paramsObj = JsonUtils.parseMap(options);
            if (StringUtils.isEmpty(paramsObj.getStr(GenConstants.TREE_CODE))) {
                throw new ServiceException("树编码字段不能为空");
            } else if (StringUtils.isEmpty(paramsObj.getStr(GenConstants.TREE_PARENT_CODE))) {
                throw new ServiceException("树父编码字段不能为空");
            } else if (StringUtils.isEmpty(paramsObj.getStr(GenConstants.TREE_NAME))) {
                throw new ServiceException("树名称字段不能为空");
            }
        }
    }

    /**
     * 设置主键列信息
     *
     * @param table 业务表信息
     */
    public void setPkColumn(GenTable table) {
        for (GenTableColumn column : table.getColumns()) {
            if (column.isPk()) {
                table.setPkColumn(column);
                break;
            }
        }
        if (ObjectUtil.isNull(table.getPkColumn())) {
            table.setPkColumn(table.getColumns().get(0));
        }

    }

    /**
     * 设置代码生成其他选项值
     *
     * @param genTable 设置后的生成对象
     */
    public void setTableFromOptions(GenTable genTable) {
        Dict paramsObj = JsonUtils.parseMap(genTable.getOptions());
        if (ObjectUtil.isNotNull(paramsObj)) {
            String treeCode = paramsObj.getStr(GenConstants.TREE_CODE);
            String treeParentCode = paramsObj.getStr(GenConstants.TREE_PARENT_CODE);
            String treeName = paramsObj.getStr(GenConstants.TREE_NAME);
            Long parentMenuId = paramsObj.getLong(GenConstants.PARENT_MENU_ID);
            String parentMenuName = paramsObj.getStr(GenConstants.PARENT_MENU_NAME);

            genTable.setTreeCode(treeCode);
            genTable.setTreeParentCode(treeParentCode);
            genTable.setTreeName(treeName);
            genTable.setParentMenuId(parentMenuId);
            genTable.setParentMenuName(parentMenuName);
        }
    }

    /**
     * 获取代码生成地址
     *
     * @param table    业务表信息
     * @param template 模板文件路径
     * @return 生成地址
     */
    public static String getGenPath(GenTable table, String template) {
        String genPath = table.getGenPath();
        if (StringUtils.equals(genPath, "/")) {
            return System.getProperty("user.dir") + File.separator + "src" + File.separator + VelocityUtils.getFileName(template, table);
        }
        return genPath + File.separator + VelocityUtils.getFileName(template, table);
    }
}

