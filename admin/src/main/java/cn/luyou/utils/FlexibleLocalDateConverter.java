package cn.luyou.utils;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

/**
 * 宽松的 LocalDate 转换器：兼容多种日期格式以及 Excel 数值型日期单元格（序列号），读取失败时返回 null 而非抛异常
 */
public class FlexibleLocalDateConverter implements Converter<LocalDate> {

    private static final List<DateTimeFormatter> FORMATTERS = Arrays.asList(
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyyMMdd"),
            DateTimeFormatter.ofPattern("yyyy年MM月dd日")
    );

    /** Excel 日期序列号基准日（1899-12-30，修正了 Lotus 1-2-3 的闰年 bug） */
    private static final LocalDate EXCEL_EPOCH = LocalDate.of(1899, 12, 30);

    @Override
    public Class<LocalDate> supportJavaTypeKey() {
        return LocalDate.class;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        // 返回 STRING，但 convertToJavaData 中会主动检测并处理 NUMBER 类型单元格
        return CellDataTypeEnum.STRING;
    }

    @Override
    public LocalDate convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty,
            GlobalConfiguration globalConfiguration) {
        // 处理数值型日期单元格（Excel 内部以距 1899-12-30 的天数存储日期）
        if (cellData.getType() == CellDataTypeEnum.NUMBER && cellData.getNumberValue() != null) {
            try {
                long serial = cellData.getNumberValue().longValue();
                if (serial > 0) {
                    return EXCEL_EPOCH.plusDays(serial);
                }
            } catch (Exception ignored) {
                // 序列号转换失败，继续尝试字符串解析
            }
        }
        String value = cellData.getStringValue();
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        for (DateTimeFormatter fmt : FORMATTERS) {
            try {
                return LocalDate.parse(trimmed, fmt);
            } catch (DateTimeParseException ignored) {
                // 尝试下一种格式
            }
        }
        // 所有格式均无法解析，返回 null（不抛异常）
        return null;
    }

    @Override
    public WriteCellData<String> convertToExcelData(LocalDate value, ExcelContentProperty contentProperty,
            GlobalConfiguration globalConfiguration) {
        if (value == null) {
            return new WriteCellData<>("");
        }
        return new WriteCellData<>(value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    }
}
