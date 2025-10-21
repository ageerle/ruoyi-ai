package org.ruoyi.workflow;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;

import java.sql.Types;
import java.util.Collections;

public class CodeGenerator {
    public static void main(String[] args) {
        FastAutoGenerator.create("jdbc:postgres://172.17.30.40:5432/aideepin?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8&tinyInt1isBit=false&allowMultiQueries=true", "postgres", "postgres")
                .globalConfig(builder -> {
                    builder.author("moyz") // 设置作者
                            .enableSwagger() // 开启 swagger 模式
                            .fileOverride() // 覆盖已生成文件
                            .outputDir("D://"); // 指定输出目录
                })
                .dataSourceConfig(builder -> builder.typeConvertHandler((globalConfig, typeRegistry, metaInfo) -> {
                    int typeCode = metaInfo.getJdbcType().TYPE_CODE;
                    if (typeCode == Types.SMALLINT) {
                        // 自定义类型转换
                        return DbColumnType.INTEGER;
                    }
                    return typeRegistry.getColumnType(metaInfo);

                }))
                .packageConfig(builder -> {
                    builder.mapper("com.adi.common.mapper")
                            .parent("")
                            .moduleName("")
                            .entity("po")
                            .serviceImpl("service.impl")
                            .pathInfo(Collections.singletonMap(OutputFile.xml, "D://mybatisplus-generatorcode")); // 设置mapperXml生成路径
                })
                .strategyConfig(builder -> {
                    builder.addInclude("adi_knowledge_base_qa_record") // 设置需要生成的表名
                            .addTablePrefix("adi_");
                    builder.mapperBuilder().enableBaseResultMap().enableMapperAnnotation().build();
                })
                .execute();
    }
}
