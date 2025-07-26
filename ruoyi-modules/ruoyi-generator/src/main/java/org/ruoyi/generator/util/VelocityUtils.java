package org.ruoyi.generator.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.ruoyi.helper.DataBaseHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * 模板处理工具类
 *
 * @author ruoyi
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VelocityUtils {


    /**
     * 获取模板信息
     *
     * @return 模板列表
     */
    public static List<String> getTemplateList() {
        List<String> templates = new ArrayList<>();
        templates.add("vm/java/domain.java.vm");
        templates.add("vm/java/vo.java.vm");
        templates.add("vm/java/bo.java.vm");
        templates.add("vm/java/mapper.java.vm");
        templates.add("vm/java/service.java.vm");
        templates.add("vm/java/serviceImpl.java.vm");
        templates.add("vm/java/controller.java.vm");
        templates.add("vm/xml/mapper.xml.vm");
        if (DataBaseHelper.isOracle()) {
            templates.add("vm/sql/oracle/sql.vm");
        } else if (DataBaseHelper.isPostgerSql()) {
            templates.add("vm/sql/postgres/sql.vm");
        } else if (DataBaseHelper.isSqlServer()) {
            templates.add("vm/sql/sqlserver/sql.vm");
        } else {
            templates.add("vm/sql/sql.vm");
        }
        return templates;
    }


}
