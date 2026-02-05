package org.ruoyi.demo.service;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 导出下拉框Excel示例
 *
 * @author Emil.Zhang
 */
public interface IExportExcelService {

    /**
     * 导出下拉框
     *
     * @param response /
     */
    void exportWithOptions(HttpServletResponse response);

    /**
     * 自定义导出
     *
     * @param response /
     */
    void customExport(HttpServletResponse response) throws IOException;
}
