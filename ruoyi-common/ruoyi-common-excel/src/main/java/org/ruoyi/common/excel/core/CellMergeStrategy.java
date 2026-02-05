package org.ruoyi.common.excel.core;

import cn.hutool.core.collection.CollUtil;
import cn.idev.excel.metadata.Head;
import cn.idev.excel.write.handler.WorkbookWriteHandler;
import cn.idev.excel.write.handler.context.WorkbookWriteHandlerContext;
import cn.idev.excel.write.merge.AbstractMergeStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.*;

/**
 * 列值重复合并策略
 *
 * @author Lion Li
 */
@Slf4j
public class CellMergeStrategy extends AbstractMergeStrategy implements WorkbookWriteHandler {

    private final List<CellRangeAddress> cellList;

    public CellMergeStrategy(List<CellRangeAddress> cellList) {
        this.cellList = cellList;
    }

    public CellMergeStrategy(List<?> list, boolean hasTitle) {
        this.cellList = CellMergeHandler.of(hasTitle).handle(list);
    }

    @Override
    protected void merge(Sheet sheet, Cell cell, Head head, Integer relativeRowIndex) {
        if (CollUtil.isEmpty(cellList)){
            return;
        }
        //单元格写入了,遍历合并区域,如果该Cell在区域内,但非首行,则清空
        final int rowIndex = cell.getRowIndex();
        for (CellRangeAddress cellAddresses : cellList) {
            final int firstRow = cellAddresses.getFirstRow();
            if (cellAddresses.isInRange(cell) && rowIndex != firstRow){
                cell.setBlank();
            }
        }
    }

    @Override
    public void afterWorkbookDispose(final WorkbookWriteHandlerContext context) {
        if (CollUtil.isEmpty(cellList)){
            return;
        }
        //当前表格写完后，统一写入
        for (CellRangeAddress item : cellList) {
            context.getWriteContext().writeSheetHolder().getSheet().addMergedRegion(item);
        }
    }

}
