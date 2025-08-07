package org.ruoyi.generator.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.ruoyi.common.core.constant.Constants;
import org.ruoyi.generator.config.GenConfig;
import org.ruoyi.generator.domain.vo.SchemaFieldVo;
import org.ruoyi.generator.domain.vo.SchemaVo;
import org.ruoyi.generator.service.IGenTableService;
import org.ruoyi.generator.service.SchemaFieldService;
import org.ruoyi.generator.service.SchemaService;
import org.ruoyi.generator.util.VelocityInitializer;
import org.ruoyi.generator.util.VelocityUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 业务 服务层实现
 *
 * @author Lion Li
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class GenTableServiceImpl implements IGenTableService {

    private final SchemaService schemaService;
    private final SchemaFieldService schemaFieldService;

    /**
     * 基于表名称批量生成代码到classpath路径
     *
     * @param tableName 表名称数组
     */
    @Override
    public void generateCodeToClasspathByTableNames(String tableName) {
        try {
            generateSchemaCodeToClasspathByTableName(tableName);
        } catch (Exception e) {
            throw new RuntimeException("基于表名称生成代码失败", e);
        }
    }

    /**
     * 根据表名称生成代码到classpath
     */
    private void generateSchemaCodeToClasspathByTableName(String tableName) {
        // 查询Schema信息
        SchemaVo schema = schemaService.queryByTableName(tableName);
        if (Objects.isNull(schema)) {
            log.warn("Schema不存在，表名: {}", tableName);
            return;
        }

        // 查询Schema字段信息
        List<SchemaFieldVo> fields = schemaFieldService.queryListByTableName(tableName);
        if (CollUtil.isEmpty(fields)) {
            log.warn("Schema字段为空，表名: {}", tableName);
            return;
        }
        generateCodeFromSchemaData(schema, fields);
    }

    /**
     * 根据Schema数据生成代码
     */
    private void generateCodeFromSchemaData(SchemaVo schema, List<SchemaFieldVo> fields) {

        // 初始化Velocity
        VelocityInitializer.initVelocity();

        // 准备Velocity上下文 - 直接基于Schema数据
        VelocityContext context = prepareSchemaContext(schema, fields);

        // 获取模板列表
        List<String> templates = VelocityUtils.getTemplateList();

        // 获取项目根路径
        String projectPath = getProjectRootPath();

        for (String template : templates) {
            try {
                // 渲染模板
                StringWriter sw = new StringWriter();
                Template tpl = Velocity.getTemplate(template, Constants.UTF8);
                tpl.merge(context, sw);

                // 获取文件路径 - 直接基于Schema数据
                String fileName = getSchemaFileName(template, schema);
                String fullPath = projectPath + File.separator + fileName;

                // 创建目录
                File file = new File(fullPath);
                File parentDir = file.getParentFile();
                if (!parentDir.exists()) {
                    parentDir.mkdirs();
                }
                // 写入文件
                try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                    writer.write(sw.toString());
                }
                log.info("生成文件: {}", fullPath);
            } catch (Exception e) {
                log.error("生成文件失败，模板: {}, Schema: {}", template, schema.getName(), e);
            }
        }
    }

    /**
     * 准备基于Schema的Velocity上下文
     */
    private VelocityContext prepareSchemaContext(SchemaVo schema, List<SchemaFieldVo> fields) {
        VelocityContext context = new VelocityContext();
        
        // 从配置文件读取基本配置
        String packageName = GenConfig.getPackageName();
        String author = GenConfig.getAuthor();
        String tablePrefix = GenConfig.getTablePrefix();
        boolean autoRemovePre = GenConfig.getAutoRemovePre();
        
        // 处理表名和类名
        String tableName = schema.getTableName();
        String baseClassName = schema.getTableName();
        
        // 自动去除表前缀
        if (autoRemovePre && StrUtil.isNotBlank(tablePrefix)) {
            String[] prefixes = tablePrefix.split(",");
            for (String prefix : prefixes) {
                if (baseClassName.startsWith(prefix.trim())) {
                    baseClassName = baseClassName.substring(prefix.trim().length());
                    break;
                }
            }
        }
        
        String className = toCamelCase(baseClassName, true);  // 首字母大写的类名，如：SysRole
        String classname = toCamelCase(baseClassName, false); // 首字母小写的类名，如：sysRole
        String businessName = toCamelCase(baseClassName, false);
        String moduleName = getModuleName(packageName);
        
        // 基本信息
        context.put("tableName", tableName);
        context.put("tableComment", schema.getComment());
        context.put("className", classname);  // 首字母小写
        context.put("classname", classname);  // 首字母小写（兼容性）
        context.put("ClassName", className);  // 首字母大写
        context.put("functionName", schema.getName());
        context.put("functionAuthor", author);
        context.put("author", author);
        context.put("datetime", new Date());
        context.put("packageName", packageName);
        context.put("moduleName", moduleName);
        context.put("businessName", businessName);
        
        // 权限相关
        context.put("permissionPrefix", moduleName + ":" + businessName);
        context.put("parentMenuId", "2000");  // 默认父菜单ID，可配置
        
        // 生成菜单ID
        List<Long> menuIds = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            menuIds.add(IdUtil.getSnowflakeNextId());
        }
        context.put("menuIds", menuIds);
        
        // 创建table对象，包含menuIds等信息和方法
        Map<String, Object> table = new HashMap<>();
        table.put("menuIds", menuIds);
        table.put("tableName", tableName);
        table.put("tableComment", schema.getComment());
        table.put("className", className);
        table.put("classname", classname);
        table.put("functionName", schema.getName());
        
        // 添加表类型属性（默认为crud类型）
        table.put("crud", true);
        table.put("sub", false);
        table.put("tree", false);
        
        // 添加isSuperColumn方法
        table.put("isSuperColumn", new Object() {
            public boolean isSuperColumn(String javaField) {
                // 定义超类字段（BaseEntity中的字段）
                return "createBy".equals(javaField) || "createTime".equals(javaField) 
                    || "updateBy".equals(javaField) || "updateTime".equals(javaField)
                    || "remark".equals(javaField) || "tenantId".equals(javaField);
            }
        });
        
        context.put("table", table);
        
        // 处理字段信息
        List<Map<String, Object>> columns = new ArrayList<>();
        Map<String, Object> pkColumn = null;
        Set<String> importList = new HashSet<>();
        
        // 添加基础导入
        importList.add("java.io.Serializable");

        for (SchemaFieldVo field : fields) {
            Map<String, Object> column = new HashMap<>();
            String javaType = getJavaType(field.getType());
            String javaField = StrUtil.toCamelCase(field.getCode());
            
            column.put("columnName", field.getCode());
            column.put("columnComment", field.getName());
            column.put("comment", field.getName());  // 添加comment别名
            column.put("columnType", field.getType());
            column.put("javaType", javaType);
            column.put("javaField", javaField);
            column.put("capJavaField", toCamelCase(field.getCode(), true));
            
            // 布尔值属性（兼容两种格式）
            boolean isPk = "1".equals(field.getIsPk());
            boolean isRequired = "1".equals(field.getIsRequired());
            boolean isInsert = "1".equals(field.getIsInsert());
            boolean isEdit = "1".equals(field.getIsEdit());
            boolean isList = "1".equals(field.getIsList());
            boolean isQuery = "1".equals(field.getIsQuery());
            
            column.put("isPk", isPk ? 1 : 0);
            column.put("pk", isPk);  // 添加pk别名
            column.put("isRequired", isRequired);
            column.put("required", isRequired);  // 添加required别名
            column.put("isInsert", isInsert);
            column.put("insert", isInsert);  // 添加insert别名
            column.put("isEdit", isEdit);
            column.put("edit", isEdit);  // 添加edit别名
            column.put("isList", isList);
            column.put("list", isList);  // 添加list别名
            column.put("isQuery", isQuery);
            column.put("query", isQuery);  // 添加query别名
            
            column.put("queryType", field.getQueryType());
            column.put("htmlType", field.getHtmlType());
            column.put("dictType", field.getDictType());
            column.put("sort", field.getSort());
            
            // 添加readConverterExp方法
            column.put("readConverterExp", new Object() {
            });
            
            // 根据Java类型添加相应的导入
            addImportForJavaType(javaType, importList);
            
            columns.add(column);
            
            // 设置主键列
            if (isPk) {
                pkColumn = column;
            }
        }
        
        // 如果没有主键，使用第一个字段作为主键
        if (pkColumn == null && !columns.isEmpty()) {
            pkColumn = columns.get(0);
            // 将第一个字段设置为主键
            pkColumn.put("isPk", 1);
            pkColumn.put("pk", true);
        }
        
        context.put("columns", columns);
        context.put("pkColumn", pkColumn);
        context.put("importList", new ArrayList<>(importList));
        
        return context;
    }
    
    /**
      * 根据Java类型添加相应的导入
      */
     private void addImportForJavaType(String javaType, Set<String> importList) {
         switch (javaType) {
             case "BigDecimal" -> importList.add("java.math.BigDecimal");
             case "Date" -> importList.add("java.util.Date");
             case "LocalDateTime" -> importList.add("java.time.LocalDateTime");
             case "LocalDate" -> importList.add("java.time.LocalDate");
             case "LocalTime" -> importList.add("java.time.LocalTime");
             default -> {}
         }
     }

    /**
     * 从包名中提取模块名
     */
    private String getModuleName(String packageName) {
        if (StrUtil.isBlank(packageName)) {
            return "generator";
        }
        String[] parts = packageName.split("\\.");
        if (parts.length >= 3) {
            return parts[2]; // org.ruoyi.system -> system
        }
        return "generator";
    }

    /**
     * 获取基于Schema的文件名
     */
    private String getSchemaFileName(String template, SchemaVo schema) {
        // 从配置文件读取配置
        String packageName = GenConfig.getPackageName();
        String tablePrefix = GenConfig.getTablePrefix();
        boolean autoRemovePre = GenConfig.getAutoRemovePre();
        
        // 处理类名
        String baseClassName = schema.getTableName();
        
        // 自动去除表前缀
        if (autoRemovePre && StrUtil.isNotBlank(tablePrefix)) {
            String[] prefixes = tablePrefix.split(",");
            for (String prefix : prefixes) {
                if (baseClassName.startsWith(prefix.trim())) {
                    baseClassName = baseClassName.substring(prefix.trim().length());
                    break;
                }
            }
        }
        
        String className = toCamelCase(baseClassName, true);   // 首字母大写，如：SysRole
        // 首字母小写，如：sysRole
        String moduleName = getModuleName(packageName);
        String javaPath = "src/main/java/";
        String mybatisPath = "src/main/resources/mapper/";
        
        if (template.contains("domain.java.vm")) {
            return javaPath + packageName.replace(".", "/") + "/domain/" + className + ".java";
        } else if (template.contains("mapper.java.vm")) {
            return javaPath + packageName.replace(".", "/") + "/mapper/" + className + "Mapper.java";
        } else if (template.contains("service.java.vm")) {
            return javaPath + packageName.replace(".", "/") + "/service/" + className + "Service.java";
        } else if (template.contains("serviceImpl.java.vm")) {
            return javaPath + packageName.replace(".", "/") + "/service/impl/" + className + "ServiceImpl.java";
        } else if (template.contains("controller.java.vm")) {
            return javaPath + packageName.replace(".", "/") + "/controller/" + className + "Controller.java";
        } else if (template.contains("vo.java.vm")) {
            return javaPath + packageName.replace(".", "/") + "/domain/vo/" + className + "Vo.java";
        } else if (template.contains("bo.java.vm")) {
            return javaPath + packageName.replace(".", "/") + "/domain/bo/" + className + "Bo.java";
        } else if (template.contains("mapper.xml.vm")) {
            return mybatisPath + moduleName + "/" + className + "Mapper.xml";
        } else if (template.contains("sql.vm")) {
            return javaPath + packageName.replace(".", "/") + "/sql/" + baseClassName + "_menu.sql";
        }
        return template.replace(".vm", "");
    }

    /**
     * 获取项目根路径
     */
    private String getProjectRootPath() {
        // 获取当前类的路径，然后向上查找到项目根目录
        String classPath = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        File currentDir = new File(classPath).getParentFile();

        // 向上查找，直到找到包含src目录的项目根目录
        while (currentDir != null && currentDir.exists()) {
            File srcDir = new File(currentDir, "src");
            if (srcDir.exists() && srcDir.isDirectory()) {
                return currentDir.getAbsolutePath();
            }
            currentDir = currentDir.getParentFile();
        }

        // 如果找不到，使用当前工作目录
        return System.getProperty("user.dir");
    }

    /**
     * 转换为驼峰命名
     */
    private String toCamelCase(String str, boolean firstUpperCase) {
        if (StrUtil.isBlank(str)) {
            return str;
        }

        StringBuilder result = new StringBuilder();
        String[] parts = str.split("_");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].toLowerCase();
            if (i == 0 && !firstUpperCase) {
                result.append(part);
            } else {
                result.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
            }
        }
        return result.toString();
    }

    /**
     * 获取Java类型
     */
    private String getJavaType(String dbType) {
        if (StrUtil.isBlank(dbType)) {
            return "String";
        }
        String type = dbType.toLowerCase();
        if (type.contains("int") || type.contains("tinyint") || type.contains("smallint")) {
            return "Integer";
        } else if (type.contains("bigint")) {
            return "Long";
        } else if (type.contains("decimal") || type.contains("numeric") || type.contains("float") || type.contains(
                "double")) {
            return "BigDecimal";
        } else if (type.contains("date") || type.contains("time")) {
            return "Date";
        } else if (type.contains("bit") || type.contains("boolean")) {
            return "Boolean";
        } else {
            return "String";
        }
    }

}

