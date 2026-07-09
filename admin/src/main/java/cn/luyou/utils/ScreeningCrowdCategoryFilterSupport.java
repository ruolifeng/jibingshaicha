package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;
import cn.luyou.model.ScreeningKeyPopulation;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 重点人群/疫情筛查「人群分类」筛选：
 * <ul>
 *   <li>单选：仅显示该分类为「是」且其余分类均不为「是」的记录（单纯分类人员）</li>
 *   <li>多选：所选分类均为「是」（AND，可命中合并分类人员）</li>
 * </ul>
 */
public final class ScreeningCrowdCategoryFilterSupport {

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
        wrapper.eq(selectedColumn, "是");
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
            wrapper.eq(column, "是");
        }
    }

    /** 非「是」：null、空串、否及其它值均视为未命中该分类 */
    private static void applyNotYes(LambdaQueryWrapper<ScreeningKeyPopulation> wrapper,
                                    SFunction<ScreeningKeyPopulation, String> column) {
        wrapper.and(w -> w.isNull(column)
                .or()
                .eq(column, "")
                .or()
                .ne(column, "是"));
    }

    public static boolean isYes(String value) {
        return "是".equals(StrUtil.trim(value));
    }
}
