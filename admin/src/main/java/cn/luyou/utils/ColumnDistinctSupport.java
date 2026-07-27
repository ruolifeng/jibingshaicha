package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 表头 Excel 式筛选：列实际去重值的后处理。
 */
public final class ColumnDistinctSupport {

    private ColumnDistinctSupport() {
    }

    /** blank 过滤、trim、distinct、sorted */
    public static List<String> normalize(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}
