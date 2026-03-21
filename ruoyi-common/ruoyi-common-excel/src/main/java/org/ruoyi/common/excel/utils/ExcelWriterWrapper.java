package org.ruoyi.common.excel.utils;

import cn.idev.excel.ExcelWriter;
import cn.idev.excel.FastExcel;
import cn.idev.excel.context.WriteContext;
import cn.idev.excel.write.builder.ExcelWriterSheetBuilder;
import cn.idev.excel.write.builder.ExcelWriterTableBuilder;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.idev.excel.write.metadata.WriteTable;
import cn.idev.excel.write.metadata.fill.FillConfig;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * ExcelWriterWrapper Excel写出包装器
 * <br>
 * 提供了一组与 ExcelWriter 一一对应的写出方法，避免直接提供 ExcelWriter 而导致的一些不可控问题（比如提前关闭了IO流等）
 *
 * @author 秋辞未寒
 * @see ExcelWriter
 */
public record ExcelWriterWrapper<T>(ExcelWriter excelWriter) {

    public void write(Collection<T> data, WriteSheet writeSheet) {
        excelWriter.write(data, writeSheet);
    }

    public void write(Supplier<Collection<T>> supplier, WriteSheet writeSheet) {
        excelWriter.write(supplier.get(), writeSheet);
    }

    public void write(Collection<T> data, WriteSheet writeSheet, WriteTable writeTable) {
        excelWriter.write(data, writeSheet, writeTable);
    }

    public void write(Supplier<Collection<T>> supplier, WriteSheet writeSheet, WriteTable writeTable) {
        excelWriter.write(supplier.get(), writeSheet, writeTable);
    }

    public void fill(Object data, WriteSheet writeSheet) {
        excelWriter.fill(data, writeSheet);
    }

    public void fill(Object data, FillConfig fillConfig, WriteSheet writeSheet) {
        excelWriter.fill(data, fillConfig, writeSheet);
    }

    public void fill(Supplier<Object> supplier, WriteSheet writeSheet) {
        excelWriter.fill(supplier, writeSheet);
    }

    public void fill(Supplier<Object> supplier, FillConfig fillConfig, WriteSheet writeSheet) {
        excelWriter.fill(supplier, fillConfig, writeSheet);
    }

    public WriteContext writeContext() {
        return excelWriter.writeContext();
    }

    /**
     * 创建一个 ExcelWriterWrapper
     *
     * @param excelWriter ExcelWriter
     * @return ExcelWriterWrapper
     */
    public static  <T> ExcelWriterWrapper<T> of(ExcelWriter excelWriter) {
        return new ExcelWriterWrapper<>(excelWriter);
    }

    // -------------------------------- sheet start

    public static WriteSheet buildSheet(Integer sheetNo, String sheetName) {
        return sheetBuilder(sheetNo, sheetName).build();
    }

    public static WriteSheet buildSheet(Integer sheetNo) {
        return sheetBuilder(sheetNo).build();
    }

    public static WriteSheet buildSheet(String sheetName) {
        return sheetBuilder(sheetName).build();
    }

    public static WriteSheet buildSheet() {
        return sheetBuilder().build();
    }

    public static ExcelWriterSheetBuilder sheetBuilder(Integer sheetNo, String sheetName) {
        return FastExcel.writerSheet(sheetNo, sheetName);
    }

    public static ExcelWriterSheetBuilder sheetBuilder(Integer sheetNo) {
        return FastExcel.writerSheet(sheetNo);
    }

    public static ExcelWriterSheetBuilder sheetBuilder(String sheetName) {
        return FastExcel.writerSheet(sheetName);
    }

    public static ExcelWriterSheetBuilder sheetBuilder() {
        return FastExcel.writerSheet();
    }

    // -------------------------------- sheet end

    // -------------------------------- table start

    public static WriteTable buildTable(Integer tableNo) {
        return tableBuilder(tableNo).build();
    }

    public static WriteTable buildTable() {
        return tableBuilder().build();
    }

    public static ExcelWriterTableBuilder tableBuilder(Integer tableNo) {
        return FastExcel.writerTable(tableNo);
    }

    public static ExcelWriterTableBuilder tableBuilder() {
        return FastExcel.writerTable();
    }

    // -------------------------------- table end

}
