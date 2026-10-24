package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;
import cn.luyou.model.ScreeningKeyPopulation;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 重点人群/疫情筛查「人群分类」筛选：
 * <ul>
 *   <li>单选：仅显示该分类为「是」且其余分类均不为「是」的记录（单纯分类人员）</li>
 *   <li>多选：所选分类均为「是」（AND，可命中合并分类人员）</li>
 * </ul>
 * <p>「√ / ✓ / ✔」与「是」等价（历史 Excel 勾选写入）。</p>
 */
public final class ScreeningCrowdCategoryFilterSupport {

    /** 人群分类「是」及历史勾选符号 */
    public static final List<String> YES_EQUIVALENTS = List.of("是", "√", "✓", "✔");

    private static final Set<String> YES_MARKS = Set.of("√", "✓", "✔");

    private static final Map<String, SFunction<ScreeningKeyPopulation, String>> CATEGORY_COLUMNS =
            new LinkedHashMap<>();

    static {
        CATEGORY_COLUMNS.put("密接", ScreeningKeyPopulation::getCrowdCategoryClose);
        CATEGORY_COLUMNS.put("学生", ScreeningKeyPopulation::getCrowdCategoryStudent);
        CATEGORY_COLUMNS.put("教职工", ScreeningKeyPopulation::getCrowdCategoryTeacher);
        CATEGORY_COLUMNS.put("老年人", ScreeningKeyPopulation::getCrowdCategoryElder);
        CATEGORY_COLUMNS.put("糖尿病", ScreeningKeyPopulation::getCrowdCategoryDiabetes);
        CATEGORY_COLUMNS.put("双感", ScreeningKeyPopulation::getCrowdCategoryDual);
        CATEGORY_COLUMNS.put("既往结核史", ScreeningKeyPopulation::getCrowdCategoryTbHist);
        CATEGORY_COLUMNS.put("非重点人群", ScreeningKeyPopulation::getCrowdCategoryNormal);
    }

    private ScreeningCrowdCategoryFilterSupport() {
    }

    public static void applyFilter(LambdaQueryWrapper<ScreeningKeyPopulation> wrapper, String crowdCategory) {
        List<String> selected = parseCategories(crowdCategory);
        if (selected.isEmpty()) {
            return;
        }
        if (selected.size() == 1) {
            applyExclusiveSingleCategory(wrapper, selected.get(0));
            return;
        }
        for (String category : selected) {
            applyYesCategory(wrapper, category);
        }
    }

    private static List<String> parseCategories(String crowdCategory) {
        if (StrUtil.isBlank(crowdCategory)) {
            return List.of();
        }
        // 兼容英文/中文逗号分隔
        String normalized = crowdCategory.replace('，', ',');
        return Arrays.stream(normalized.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .filter(CATEGORY_COLUMNS::containsKey)
                .distinct()
                .toList();
    }

    /** 单选：命中该分类且其余分类均不为「是」 */
    private static void applyExclusiveSingleCategory(LambdaQueryWrapper<ScreeningKeyPopulation> wrapper,
                                                     String category) {
        SFunction<ScreeningKeyPopulation, String> selectedColumn = CATEGORY_COLUMNS.get(category);
        if (selectedColumn == null) {
            return;
        }
        applyYesEquals(wrapper, selectedColumn);
        for (Map.Entry<String, SFunction<ScreeningKeyPopulation, String>> entry : CATEGORY_COLUMNS.entrySet()) {
            if (entry.getKey().equals(category)) {
                continue;
            }
            applyNotYes(wrapper, entry.getValue());
        }
    }

    private static void applyYesCategory(LambdaQueryWrapper<ScreeningKeyPopulation> wrapper, String category) {
        SFunction<ScreeningKeyPopulation, String> column = CATEGORY_COLUMNS.get(category);
        if (column != null) {
            applyYesEquals(wrapper, column);
        }
    }

    /** SQL：字段等于「是」或历史勾选符号 */
    public static <T> void applyYesEquals(LambdaQueryWrapper<T> wrapper, SFunction<T, String> column) {
        wrapper.in(column, YES_EQUIVALENTS);
    }

    /** 非「是」：null、空串、否及其它非勾选值均视为未命中该分类 */
    private static void applyNotYes(LambdaQueryWrapper<ScreeningKeyPopulation> wrapper,
                                    SFunction<ScreeningKeyPopulation, String> column) {
        wrapper.and(w -> w.isNull(column)
                .or()
                .eq(column, "")
                .or()
                .notIn(column, YES_EQUIVALENTS));
    }

    /** 是否视为「是」（含 √ 等勾选符号） */
    public static boolean isYes(String value) {
        String t = StrUtil.trim(value);
        if (StrUtil.isBlank(t)) {
            return false;
        }
        return "是".equals(t) || YES_MARKS.contains(t);
    }

    /** 勾选符号归一为「是」；其它值 trim 后原样返回 */
    public static String normalizeYesMark(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        if (t.isEmpty()) {
            return t;
        }
        if (isYes(t)) {
            return "是";
        }
        return t;
    }

    /**
     * 表头筛选：勾选「是」或「√」时互相命中，统一归入「是」。
     */
    public static String expandYesFilterValue(String value) {
        Collection<String> values = ColumnFilterSupport.splitValues(value);
        if (values.isEmpty()) {
            return value;
        }
        boolean hasYes = values.stream().anyMatch(ScreeningCrowdCategoryFilterSupport::isYes);
        if (!hasYes) {
            return value;
        }
        LinkedHashSet<String> expanded = new LinkedHashSet<>(values);
        expanded.addAll(YES_EQUIVALENTS);
        return String.join(",", expanded);
    }

    /** 去重选项：将勾选符号合并展示为「是」 */
    public static List<String> mergeYesDistinctValues(Collection<String> values) {
        return ColumnDistinctSupport.normalize(values.stream()
                .map(ScreeningCrowdCategoryFilterSupport::normalizeYesMark)
                .toList());
    }
}
