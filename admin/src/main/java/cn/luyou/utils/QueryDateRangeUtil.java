package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 列表查询日期区间解析（前端 date-picker 传 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss）
 */
public final class QueryDateRangeUtil {

    private QueryDateRangeUtil() {
    }

    public static LocalDate parseLocalDate(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        String val = text.trim();
        if (val.length() >= 10) {
            String datePart = val.substring(0, 10).replace('/', '-');
            try {
                return LocalDate.parse(datePart, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (Exception ignored) {
            }
        }
        for (String pattern : new String[]{"yyyy-MM-dd", "yyyy/MM/dd"}) {
            try {
                return LocalDate.parse(val, DateTimeFormatter.ofPattern(pattern));
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public static LocalDateTime parseDateTimeFrom(String text) {
        return parseDateTime(text);
    }

    /** 结束日期若为 00:00:00，则扩展至当日 23:59:59 */
    public static LocalDateTime parseDateTimeTo(String text) {
        LocalDateTime dt = parseDateTime(text);
        if (dt == null) {
            return null;
        }
        if (dt.toLocalTime().equals(java.time.LocalTime.MIDNIGHT)) {
            return dt.plusDays(1).minusSeconds(1);
        }
        return dt;
    }

    private static LocalDateTime parseDateTime(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        String val = text.trim();
        LocalDate dateOnly = parseLocalDate(val);
        if (dateOnly != null && !val.contains(":")) {
            return dateOnly.atStartOfDay();
        }
        for (String pattern : new String[]{"yyyy-MM-dd HH:mm:ss", "yyyy/MM/dd HH:mm:ss", "yyyy-MM-dd", "yyyy/MM/dd"}) {
            try {
                if (pattern.contains("HH")) {
                    return LocalDateTime.parse(val, DateTimeFormatter.ofPattern(pattern));
                }
                LocalDate date = LocalDate.parse(val, DateTimeFormatter.ofPattern(pattern));
                return date.atStartOfDay();
            } catch (Exception ignored) {
            }
        }
        if (dateOnly != null) {
            return dateOnly.atStartOfDay();
        }
        return null;
    }
}
