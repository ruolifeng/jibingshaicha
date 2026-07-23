package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.Map;
import java.util.Set;

/**
 * 列表服务端排序：仅允许白名单字段，防止 SQL 注入。
 */
public final class ListSortSupport {

    private ListSortSupport() {
    }

    public static void apply(LambdaQueryWrapper<?> wrapper,
                             String sortField,
                             String sortOrder,
                             Map<String, String> columnWhitelist,
                             String defaultOrderSql) {
        if (wrapper == null) {
            return;
        }
        String sql = resolveOrderSql(sortField, sortOrder, columnWhitelist, defaultOrderSql);
        wrapper.last(sql);
    }

    public static String resolveOrderSql(String sortField,
                                         String sortOrder,
                                         Map<String, String> columnWhitelist,
                                         String defaultOrderSql) {
        if (columnWhitelist == null || columnWhitelist.isEmpty() || StrUtil.isBlank(sortField)) {
            return defaultOrderSql;
        }
        if ("importRowNo".equals(sortField) && !"desc".equalsIgnoreCase(sortOrder)) {
            return defaultOrderSql;
        }
        String column = columnWhitelist.get(sortField);
        if (StrUtil.isBlank(column)) {
            return defaultOrderSql;
        }
        boolean desc = "desc".equalsIgnoreCase(sortOrder);
        String nullsPrefix = NULLS_LAST_FIELDS.contains(sortField)
                ? column + " IS NULL, "
                : "";
        return "ORDER BY " + nullsPrefix + column + (desc ? " DESC" : " ASC") + ", id ASC";
    }

    /** 可空字段排序时 NULL 置后 */
    private static final Set<String> NULLS_LAST_FIELDS = Set.of(
            "importRowNo", "screenDate", "birthDate", "chestXrayDate", "age", "registrationNo"
    );
}
