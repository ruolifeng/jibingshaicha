package cn.luyou.utils;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;

import java.time.LocalDate;

/**
 * EasyExcel LocalDate 转换器：委托 {@link FlexibleDateParseUtil} 统一解析多种日期格式。
 */
public class FlexibleLocalDateConverter implements Converter<LocalDate> {

    @Override
    public Class<LocalDate> supportJavaTypeKey() {
        return LocalDate.class;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return CellDataTypeEnum.STRING;
    }

    @Override
    public LocalDate convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty,
            GlobalConfiguration globalConfiguration) {
        if (cellData == null) {
            return null;
        }
        if (cellData.getType() == CellDataTypeEnum.NUMBER && cellData.getNumberValue() != null) {
            return FlexibleDateParseUtil.parse(cellData.getNumberValue());
        }
        return FlexibleDateParseUtil.parseText(cellData.getStringValue());
    }

    @Override
    public WriteCellData<String> convertToExcelData(LocalDate value, ExcelContentProperty contentProperty,
            GlobalConfiguration globalConfiguration) {
        if (value == null) {
            return new WriteCellData<>("");
        }
        return new WriteCellData<>(FlexibleDateParseUtil.format(value));
    }
}
