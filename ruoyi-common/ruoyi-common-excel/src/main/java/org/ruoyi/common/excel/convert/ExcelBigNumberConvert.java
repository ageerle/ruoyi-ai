package org.ruoyi.common.excel.convert;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * 大数值转换
 * Excel 数值长度位15位 大于15位的数值转换位字符串
 *
 * @author Lion Li
 */
@Slf4j
public class ExcelBigNumberConvert implements Converter<Long> {

    @Override
    public Class<Long> supportJavaTypeKey() {
        return Long.class;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return CellDataTypeEnum.STRING;
    }

    @Override
    public Long convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) {
        return Convert.toLong(cellData.getData());
    }

    @Override
    public WriteCellData<Object> convertToExcelData(Long object, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) {
        if (ObjectUtil.isNotNull(object)) {
            String str = Convert.toStr(object);
            if (str.length() > 15) {
                return new WriteCellData<>(str);
            }
        }
        WriteCellData<Object> cellData = new WriteCellData<>(new BigDecimal(object));
        cellData.setType(CellDataTypeEnum.NUMBER);
        return cellData;
    }

}
