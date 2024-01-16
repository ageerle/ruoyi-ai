package com.xmzs.common.excel.core;

import cn.hutool.core.util.StrUtil;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 默认excel返回对象
 *
 * @author Yjoioooo
 * @author Lion Li
 */
public class DefaultExcelResult<T> implements ExcelResult<T> {

    /**
     * 数据对象list
     */
    @Setter
    private List<T> list;

    /**
     * 错误信息列表
     */
    @Setter
    private List<String> errorList;

    public DefaultExcelResult() {
        this.list = new ArrayList<>();
        this.errorList = new ArrayList<>();
    }

    public DefaultExcelResult(List<T> list, List<String> errorList) {
        this.list = list;
        this.errorList = errorList;
    }

    public DefaultExcelResult(ExcelResult<T> excelResult) {
        this.list = excelResult.getList();
        this.errorList = excelResult.getErrorList();
    }

    @Override
    public List<T> getList() {
        return list;
    }

    @Override
    public List<String> getErrorList() {
        return errorList;
    }

    /**
     * 获取导入回执
     *
     * @return 导入回执
     */
    @Override
    public String getAnalysis() {
        int successCount = list.size();
        int errorCount = errorList.size();
        if (successCount == 0) {
            return "读取失败，未解析到数据";
        } else {
            if (errorCount == 0) {
                return StrUtil.format("恭喜您，全部读取成功！共{}条", successCount);
            } else {
                return "";
            }
        }
    }
}
