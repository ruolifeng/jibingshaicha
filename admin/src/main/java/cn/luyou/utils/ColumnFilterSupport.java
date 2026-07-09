package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * 表头筛选：解析 columnFilters JSON，按白名单落到 QueryWrapper（与顶部筛选 AND 叠加）。
 * JSON 示例：{"district":"城关","gender":"男","name":"张"}
 * 多选枚举可用逗号分隔：{"diagnosisFirst":"排除,潜伏感染者"}
 */
@Slf4j
public final class ColumnFilterSupport {

    private ColumnFilterSupport() {
    }

    public static Map<String, String> parse(String columnFiltersJson) {
        Map<String, String> result = new LinkedHashMap<>();
        if (StrUtil.isBlank(columnFiltersJson)) {
            return result;
        }
        try {
            JSONObject obj = JSONUtil.parseObj(columnFiltersJson);
            for (String key : obj.keySet()) {
                Object val = obj.get(key);
                if (val == null) {
                    continue;
                }
                String text = String.valueOf(val).trim();
                if (StrUtil.isNotBlank(text)) {
                    result.put(key, text);
                }
            }
        } catch (Exception e) {
            log.warn("解析 columnFilters 失败: {}", e.getMessage());
        }
        return result;
    }

    /**
     * 对白名单内字段应用 like（文本）或 eq/in（枚举，值含逗号时按 in）。
     *
     * @param wrapper   QueryWrapper（非 Lambda，便于按列名动态拼）
     * @param filters   已解析的筛选 map（camelCase 字段名）
     * @param whitelist 允许筛选的 camelCase 字段名
     * @param eqFields  使用精确匹配（eq/in）的字段；其余用 like
     */
    public static void apply(QueryWrapper<?> wrapper,
                             Map<String, String> filters,
                             Set<String> whitelist,
                             Set<String> eqFields) {
        if (filters == null || filters.isEmpty() || whitelist == null || whitelist.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            String field = entry.getKey();
            if (!whitelist.contains(field)) {
                continue;
            }
            String value = entry.getValue();
            if (StrUtil.isBlank(value)) {
                continue;
            }
            String column = camelToUnderline(field);
            boolean exact = eqFields != null && eqFields.contains(field);
            if (exact) {
                if (value.contains(",")) {
                    String[] parts = value.split(",");
                    wrapper.in(column, (Object[]) parts);
                } else {
                    wrapper.eq(column, value);
                }
            } else {
                wrapper.like(column, value);
            }
        }
    }

    /**
     * Lambda 版：由调用方按字段名注册 apply 函数，避免动态列名。
     */
    public static <T> void applyLambda(Map<String, String> filters,
                                       Set<String> whitelist,
                                       BiConsumer<String, String> applier) {
        if (filters == null || filters.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            if (whitelist != null && !whitelist.contains(entry.getKey())) {
                continue;
            }
            if (StrUtil.isNotBlank(entry.getValue())) {
                applier.accept(entry.getKey(), entry.getValue());
            }
        }
    }

    public static String camelToUnderline(String camel) {
        if (StrUtil.isBlank(camel)) {
            return camel;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append('_').append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** 逗号分隔多值 → Collection，单值也返回单元素集合 */
    public static Collection<String> splitValues(String value) {
        if (StrUtil.isBlank(value)) {
            return java.util.List.of();
        }
        if (!value.contains(",")) {
            return java.util.List.of(value.trim());
        }
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .toList();
    }

    public static <T, R> void like(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<T> w,
                                   SFunction<T, R> col, String value) {
        if (StrUtil.isNotBlank(value)) {
            w.like(col, value);
        }
    }

    public static <T, R> void eqOrIn(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<T> w,
                                     SFunction<T, R> col, String value) {
        if (StrUtil.isBlank(value)) {
            return;
        }
        Collection<String> values = splitValues(value);
        if (values.size() == 1) {
            w.eq(col, values.iterator().next());
        } else if (!values.isEmpty()) {
            w.in(col, values);
        }
    }
}
