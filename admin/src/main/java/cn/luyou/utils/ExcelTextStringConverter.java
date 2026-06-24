package cn.luyou.utils;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;

/**
 * 身份证号、手机号等字段导出为文本，避免 Excel 数值精度丢失。
 */
public class ExcelTextStringConverter implements Converter<String> {

    @Override
    public Class<String> supportJavaTypeKey() {
        return String.class;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return CellDataTypeEnum.STRING;
    }

    @Override
    public String convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty,
                                    GlobalConfiguration globalConfiguration) {
        if (cellData == null) {
            return null;
        }
        if (cellData.getType() == CellDataTypeEnum.NUMBER && cellData.getNumberValue() != null) {
            return cellData.getNumberValue().toPlainString();
        }
        String value = cellData.getStringValue();
        return value == null ? null : value.trim();
    }

    @Override
    public WriteCellData<String> convertToExcelData(String value, ExcelContentProperty contentProperty,
                                                    GlobalConfiguration globalConfiguration) {
        if (value == null || value.isBlank()) {
            return new WriteCellData<>("");
        }
        WriteCellData<String> cellData = new WriteCellData<>(value.trim());
        cellData.setType(CellDataTypeEnum.STRING);
        return cellData;
    }
}
