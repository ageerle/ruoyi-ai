package org.ruoyi.common.excel.core;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.idev.excel.annotation.ExcelIgnore;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.SneakyThrows;
import org.apache.poi.ss.util.CellRangeAddress;
import org.ruoyi.common.core.utils.reflect.ReflectUtils;
import org.ruoyi.common.excel.annotation.CellMerge;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 单元格合并处理器
 *
 * @author Lion Li
 */
public class CellMergeHandler {

    private final boolean hasTitle;
    private int rowIndex;

    private CellMergeHandler(final boolean hasTitle) {
        this.hasTitle = hasTitle;
        // 行合并开始下标
        this.rowIndex = hasTitle ? 1 : 0;
    }

    @SneakyThrows
    public List<CellRangeAddress> handle(List<?> rows) {
        // 如果入参为空集合则返回空集
        if (CollUtil.isEmpty(rows)) {
            return Collections.emptyList();
        }

        // 获取有合并注解的字段
        Map<Field, FieldColumnIndex> mergeFields = getFieldColumnIndexMap(rows.get(0).getClass());
        // 如果没有需要合并的字段则返回空集
        if (CollUtil.isEmpty(mergeFields)) {
            return Collections.emptyList();
        }

        // 结果集
        List<CellRangeAddress> result = new ArrayList<>();

        // 生成两两合并单元格
        Map<Field, RepeatCell> rowRepeatCellMap = new HashMap<>();
        for (Map.Entry<Field, FieldColumnIndex> item : mergeFields.entrySet()) {
            Field field = item.getKey();
            FieldColumnIndex itemValue = item.getValue();
            int colNum = itemValue.colIndex();
            CellMerge cellMerge = itemValue.cellMerge();

            for (int i = 0; i < rows.size(); i++) {
                // 当前行数据
                Object currentRowObj = rows.get(i);
                // 当前行数据字段值
                Object currentRowObjFieldVal = ReflectUtils.invokeGetter(currentRowObj, field.getName());

                // 空值跳过不处理
                if (currentRowObjFieldVal == null || "".equals(currentRowObjFieldVal)) {
                    continue;
                }

                // 单元格合并Map是否存在数据，如果不存在则添加当前行的字段值
                if (!rowRepeatCellMap.containsKey(field)) {
                    rowRepeatCellMap.put(field, RepeatCell.of(currentRowObjFieldVal, i));
                    continue;
                }

                // 获取 单元格合并Map 中字段值
                RepeatCell repeatCell = rowRepeatCellMap.get(field);
                Object cellValue = repeatCell.value();
                int current = repeatCell.current();

                // 检查是否满足合并条件
                // currentRowObj 当前行数据
                // rows.get(i - 1) 上一行数据 注：由于 if (!rowRepeatCellMap.containsKey(field)) 条件的存在，所以该 i 必不可能小于1
                // cellMerge 当前行字段合并注解
                boolean merge = isMerge(currentRowObj, rows.get(i - 1), cellMerge);

                // 是否添加到结果集
                boolean isAddResult = false;
                // 最新行
                int lastRow = i + rowIndex - 1;

                // 如果当前行字段值和缓存中的字段值不相等，或不满足合并条件，则替换
                if (!currentRowObjFieldVal.equals(cellValue) || !merge) {
                    rowRepeatCellMap.put(field, RepeatCell.of(currentRowObjFieldVal, i));
                    isAddResult = true;
                }

                // 如果最后一行不能合并，检查之前的数据是否需要合并；如果最后一行可以合并，则直接合并到最后
                if (i == rows.size() - 1) {
                    isAddResult = true;
                    if (i > current) {
                        lastRow = i + rowIndex;
                    }
                }

                if (isAddResult && i > current) {
                    result.add(new CellRangeAddress(current + rowIndex, lastRow, colNum, colNum));
                }
            }
        }
        return result;
    }

    /**
     * 获取带有合并注解的字段列索引和合并注解信息Map集
     */
    private Map<Field, FieldColumnIndex> getFieldColumnIndexMap(Class<?> clazz) {
        boolean annotationPresent = clazz.isAnnotationPresent(ExcelIgnoreUnannotated.class);
        Field[] fields = ReflectUtils.getFields(clazz, field -> {
            if ("serialVersionUID".equals(field.getName())) {
                return false;
            }
            if (field.isAnnotationPresent(ExcelIgnore.class)) {
                return false;
            }
            return !annotationPresent || field.isAnnotationPresent(ExcelProperty.class);
        });

        // 有注解的字段
        Map<Field, FieldColumnIndex> mergeFields = new HashMap<>();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            if (!field.isAnnotationPresent(CellMerge.class)) {
                continue;
            }
            CellMerge cm = field.getAnnotation(CellMerge.class);
            int index = cm.index() == -1 ? i : cm.index();
            mergeFields.put(field, FieldColumnIndex.of(index, cm));

            if (hasTitle) {
                ExcelProperty property = field.getAnnotation(ExcelProperty.class);
                rowIndex = Math.max(rowIndex, property.value().length);
            }
        }
        return mergeFields;
    }

    private boolean isMerge(Object currentRow, Object preRow, CellMerge cellMerge) {
        final String[] mergeBy = cellMerge.mergeBy();
        if (StrUtil.isAllNotBlank(mergeBy)) {
            //比对当前行和上一行的各个属性值一一比对 如果全为真 则为真
            for (String fieldName : mergeBy) {
                final Object valCurrent = ReflectUtil.getFieldValue(currentRow, fieldName);
                final Object valPre = ReflectUtil.getFieldValue(preRow, fieldName);
                if (!Objects.equals(valPre, valCurrent)) {
                    //依赖字段如有任一不等值,则标记为不可合并
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 单元格合并
     */
    record RepeatCell(Object value, int current) {
        static RepeatCell of(Object value, int current) {
            return new RepeatCell(value, current);
        }
    }

    /**
     * 字段列索引和合并注解信息
     */
    record FieldColumnIndex(int colIndex, CellMerge cellMerge) {
        static FieldColumnIndex of(int colIndex, CellMerge cellMerge) {
            return new FieldColumnIndex(colIndex, cellMerge);
        }
    }

    /**
     * 创建一个单元格合并处理器实例
     *
     * @param hasTitle 是否合并标题
     * @return 单元格合并处理器
     */
    public static CellMergeHandler of(final boolean hasTitle) {
        return new CellMergeHandler(hasTitle);
    }

    /**
     * 创建一个单元格合并处理器实例（默认不合并标题）
     *
     * @return 单元格合并处理器
     */
    public static CellMergeHandler of() {
        return new CellMergeHandler(false);
    }

}
