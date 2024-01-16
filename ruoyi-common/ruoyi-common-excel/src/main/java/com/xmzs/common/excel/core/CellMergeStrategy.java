package com.xmzs.common.excel.core;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.write.merge.AbstractMergeStrategy;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import com.xmzs.common.core.utils.reflect.ReflectUtils;
import com.xmzs.common.excel.annotation.CellMerge;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 列值重复合并策略
 *
 * @author Lion Li
 */
@Slf4j
public class CellMergeStrategy extends AbstractMergeStrategy {

	private final List<?> list;
	private final boolean hasTitle;
    private int rowIndex;

    public CellMergeStrategy(List<?> list, boolean hasTitle) {
        this.list = list;
        this.hasTitle = hasTitle;
        // 行合并开始下标
        this.rowIndex = hasTitle ? 1 : 0;
    }

    @Override
	protected void merge(Sheet sheet, Cell cell, Head head, Integer relativeRowIndex) {
		List<CellRangeAddress> cellList = handle(list, hasTitle);
		// judge the list is not null
		if (CollUtil.isNotEmpty(cellList)) {
			// the judge is necessary
			if (cell.getRowIndex() == rowIndex && cell.getColumnIndex() == 0) {
				for (CellRangeAddress item : cellList) {
					sheet.addMergedRegion(item);
				}
			}
		}
	}

	@SneakyThrows
	private List<CellRangeAddress> handle(List<?> list, boolean hasTitle) {
		List<CellRangeAddress> cellList = new ArrayList<>();
		if (CollUtil.isEmpty(list)) {
			return cellList;
		}
        Field[] fields = ReflectUtils.getFields(list.get(0).getClass(), field -> !"serialVersionUID".equals(field.getName()));

		// 有注解的字段
		List<Field> mergeFields = new ArrayList<>();
		List<Integer> mergeFieldsIndex = new ArrayList<>();
		for (int i = 0; i < fields.length; i++) {
			Field field = fields[i];
			if (field.isAnnotationPresent(CellMerge.class)) {
				CellMerge cm = field.getAnnotation(CellMerge.class);
                mergeFields.add(field);
                mergeFieldsIndex.add(cm.index() == -1 ? i : cm.index());
                if (hasTitle) {
                    ExcelProperty property = field.getAnnotation(ExcelProperty.class);
                    rowIndex = Math.max(rowIndex, property.value().length);
                }
			}
		}

		Map<Field, RepeatCell> map = new HashMap<>();
		// 生成两两合并单元格
		for (int i = 0; i < list.size(); i++) {
			for (int j = 0; j < mergeFields.size(); j++) {
				Field field = mergeFields.get(j);
                Object val = ReflectUtils.invokeGetter(list.get(i), field.getName());

				int colNum = mergeFieldsIndex.get(j);
				if (!map.containsKey(field)) {
					map.put(field, new RepeatCell(val, i));
				} else {
					RepeatCell repeatCell = map.get(field);
					Object cellValue = repeatCell.getValue();
					if (cellValue == null || "".equals(cellValue)) {
						// 空值跳过不合并
						continue;
					}
					if (!cellValue.equals(val)) {
						if (i - repeatCell.getCurrent() > 1) {
							cellList.add(new CellRangeAddress(repeatCell.getCurrent() + rowIndex, i + rowIndex - 1, colNum, colNum));
						}
						map.put(field, new RepeatCell(val, i));
					} else if (i == list.size() - 1) {
						if (i > repeatCell.getCurrent()) {
							cellList.add(new CellRangeAddress(repeatCell.getCurrent() + rowIndex, i + rowIndex, colNum, colNum));
						}
					}
				}
			}
		}
		return cellList;
	}

	@Data
	@AllArgsConstructor
	static class RepeatCell {

		private Object value;

		private int current;

	}
}
