package cn.luyou.utils;

import cn.luyou.model.ScreeningKeyPopulation;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

/**
 * 重点人群/疫情筛查列表排序：按 Excel 原行号展示。
 * @deprecated 请使用 {@link ImportRowOrderSupport#applyWithBatch}
 */
@Deprecated
public final class ScreeningKeyPopulationOrderSupport {

    private ScreeningKeyPopulationOrderSupport() {
    }

    public static void applyDisplayOrder(LambdaQueryWrapper<ScreeningKeyPopulation> wrapper) {
        ImportRowOrderSupport.applyWithBatch(wrapper);
    }
}
