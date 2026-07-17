package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;

/**
 * Excel 上传等场景下的统一日期解析与格式化工具。
 * <p>
 * 支持多种常见日期文本格式、Excel 数值型日期序列号，以及 Date / LocalDate / LocalDateTime 等类型。
 * 统一输出格式：yyyy-MM-dd
 */
public final class FlexibleDateParseUtil {

    public static final String STANDARD_DATE_PATTERN = "yyyy-MM-dd";
    private static final DateTimeFormatter STANDARD_FORMATTER = DateTimeFormatter.ofPattern(STANDARD_DATE_PATTERN);

    /** Excel 日期序列号基准日（1899-12-30，兼容 Lotus 1-2-3 闰年缺陷） */
    private static final LocalDate EXCEL_EPOCH = LocalDate.of(1899, 12, 30);

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyy-M-d"),
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyyMMdd"),
            DateTimeFormatter.ofPattern("yyyy年MM月dd日")
    );

    private FlexibleDateParseUtil() {
    }

    /** 判断字段名是否疑似日期字段（用于 Map/POI 导入时自动归一化） */
    public static boolean isDateFieldKey(String fieldKey) {
        if (StrUtil.isBlank(fieldKey)) {
            return false;
        }
        return fieldKey.endsWith("Date") || fieldKey.endsWith("日期");
    }

    /** 将任意可识别日期值格式化为 yyyy-MM-dd；无法识别时返回去空白后的原文本 */
    public static String normalizeToStandardString(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return "";
        }
        LocalDate date = parse(value);
        return date != null ? format(date) : text;
    }

    public static String format(LocalDate date) {
        return date == null ? null : date.format(STANDARD_FORMATTER);
    }

    public static LocalDate parse(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }
        if (value instanceof Date date) {
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        if (value instanceof Number number) {
            return parseNumber(number.doubleValue());
        }
        return parseText(value.toString());
    }

    /**
     * 数值型日期：优先识别 8 位 yyyyMMdd（如 19390624），
     * 再回退 Excel 序列号。避免把身份证式出生日期当成序列号算出公元五万多年。
     */
    private static LocalDate parseNumber(double number) {
        if (Double.isNaN(number) || Double.isInfinite(number)) {
            return null;
        }
        if (number == Math.rint(number)) {
            long n = (long) number;
            String digits = Long.toString(Math.abs(n));
            if (n > 0 && digits.length() == 8) {
                LocalDate ymd = parseByFormatters(digits);
                if (ymd != null) {
                    return ymd;
                }
            }
        }
        return parseExcelSerial(number);
    }

    public static LocalDate parseText(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        String val = text.trim();

        LocalDate byFormatter = parseByFormatters(val);
        if (byFormatter != null) {
            return byFormatter;
        }

        if (val.length() >= 10) {
            String datePart = val.substring(0, 10).replace('/', '-').replace('.', '-');
            try {
                return LocalDate.parse(datePart, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException ignored) {
                // fall through
            }
        }

        return parseExcelSerialString(val);
    }

    private static LocalDate parseByFormatters(String val) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(val, formatter);
            } catch (DateTimeParseException ignored) {
                // try next pattern
            }
        }
        return null;
    }

    private static LocalDate parseExcelSerialString(String val) {
        if (!val.matches("^\\d+(\\.\\d+)?$")) {
            return null;
        }
        // 4 位纯数字更可能是年份字段，避免误当作 Excel 序列号
        if (val.matches("^\\d{4}$")) {
            return null;
        }
        // 8 位纯数字优先按 yyyyMMdd（parseText 已尝试；此处兜底避免再当序列号）
        if (val.matches("^\\d{8}$")) {
            LocalDate ymd = parseByFormatters(val);
            if (ymd != null) {
                return ymd;
            }
            return null;
        }
        try {
            return parseExcelSerial(Double.parseDouble(val));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static LocalDate parseExcelSerial(double serial) {
        // Excel 常用日期序列约 1～60000（对应 ~1900–2064）；过大必是误判
        if (serial <= 59 || serial > 100000) {
            return null;
        }
        LocalDate date = EXCEL_EPOCH.plusDays((long) Math.floor(serial));
        int year = date.getYear();
        if (year < 1900 || year > 2100) {
            return null;
        }
        return date;
    }
}
