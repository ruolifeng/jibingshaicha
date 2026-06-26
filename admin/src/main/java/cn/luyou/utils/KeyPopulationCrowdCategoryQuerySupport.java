package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;
import cn.luyou.mapper.ScreeningKeyPopulationMapper;
import cn.luyou.model.LatentInfection;
import cn.luyou.model.Patient;
import cn.luyou.model.ScreeningKeyPopulation;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 在管总览：重点人群子分类（老年人/糖尿病/双感）筛选支持。
 * 逗号分隔多选，OR 匹配（命中任一子分类即可）。
 */
public final class KeyPopulationCrowdCategoryQuerySupport {

    private static final List<String> SUPPORTED_CATEGORIES = List.of("老年人", "糖尿病", "双感");

    private KeyPopulationCrowdCategoryQuerySupport() {
    }

    public static List<String> parseCategories(String crowdCategory) {
        if (StrUtil.isBlank(crowdCategory)) {
            return Collections.emptyList();
        }
        return Arrays.stream(crowdCategory.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .filter(SUPPORTED_CATEGORIES::contains)
                .distinct()
                .toList();
    }

    public static void applyLatentFilter(LambdaQueryWrapper<LatentInfection> wrapper,
                                         String populationType,
                                         String crowdCategory,
                                         ScreeningKeyPopulationMapper screeningKeyPopulationMapper) {
        if (!"keyPopulation".equals(populationType)) {
            return;
        }
        List<String> categories = parseCategories(crowdCategory);
        if (categories.isEmpty()) {
            return;
        }
        wrapper.and(outer -> {
            boolean first = true;
            for (String category : categories) {
                Set<Long> screeningIds = findScreeningIds(screeningKeyPopulationMapper, category);
                if (first) {
                    outer.nested(sub -> applyLatentCategoryMatch(sub, category, screeningIds));
                    first = false;
                } else {
                    outer.or(sub -> applyLatentCategoryMatch(sub, category, screeningIds));
                }
            }
        });
    }

    public static void applyPatientFilter(LambdaQueryWrapper<Patient> wrapper,
                                          String populationType,
                                          String crowdCategory,
                                          ScreeningKeyPopulationMapper screeningKeyPopulationMapper) {
        if (!"keyPopulation".equals(populationType)) {
            return;
        }
        List<String> categories = parseCategories(crowdCategory);
        if (categories.isEmpty()) {
            return;
        }
        wrapper.and(outer -> {
            boolean first = true;
            for (String category : categories) {
                Set<Long> screeningIds = findScreeningIds(screeningKeyPopulationMapper, category);
                if (first) {
                    outer.nested(sub -> applyPatientCategoryMatch(sub, category, screeningIds));
                    first = false;
                } else {
                    outer.or(sub -> applyPatientCategoryMatch(sub, category, screeningIds));
                }
            }
        });
    }

    private static void applyLatentCategoryMatch(LambdaQueryWrapper<LatentInfection> sub,
                                                 String category,
                                                 Set<Long> screeningIds) {
        sub.and(manual -> manual.isNull(LatentInfection::getScreeningId)
                .and(w -> w.eq(LatentInfection::getCrowdCategory, category)
                        .or()
                        .like(LatentInfection::getCrowdCategory, category + "、")
                        .or()
                        .like(LatentInfection::getCrowdCategory, "、" + category)
                        .or()
                        .like(LatentInfection::getCrowdCategory, "、" + category + "、")));
        if (!screeningIds.isEmpty()) {
            sub.or(screening -> screening.in(LatentInfection::getScreeningId, screeningIds));
        }
    }

    private static void applyPatientCategoryMatch(LambdaQueryWrapper<Patient> sub,
                                                  String category,
                                                  Set<Long> screeningIds) {
        sub.and(manual -> manual.isNull(Patient::getScreeningId)
                .and(w -> w.like(Patient::getEpidemicData, "\"人群分类\":\"" + category + "\"")
                        .or()
                        .like(Patient::getEpidemicData, "\"人群分类\": \"" + category + "\"")
                        .or()
                        .like(Patient::getEpidemicData, category)));
        if (!screeningIds.isEmpty()) {
            sub.or(screening -> screening.in(Patient::getScreeningId, screeningIds));
        }
    }

    private static Set<Long> findScreeningIds(ScreeningKeyPopulationMapper mapper, String category) {
        LambdaQueryWrapper<ScreeningKeyPopulation> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(ScreeningKeyPopulation::getId);
        switch (category) {
            case "老年人" -> wrapper.eq(ScreeningKeyPopulation::getCrowdCategoryElder, "是");
            case "糖尿病" -> wrapper.eq(ScreeningKeyPopulation::getCrowdCategoryDiabetes, "是");
            case "双感" -> wrapper.eq(ScreeningKeyPopulation::getCrowdCategoryDual, "是");
            default -> {
                return Collections.emptySet();
            }
        }
        return mapper.selectList(wrapper).stream()
                .map(ScreeningKeyPopulation::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
