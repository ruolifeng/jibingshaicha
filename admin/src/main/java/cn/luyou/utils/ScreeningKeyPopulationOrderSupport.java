package cn.luyou.utils;

import cn.luyou.model.ScreeningKeyPopulation;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

/**
 * 重点人群/疫情筛查列表排序：按 Excel 原行号展示。
 */
public final class ScreeningKeyPopulationOrderSupport {

    private static final String DISPLAY_ORDER_SQL =
            "ORDER BY import_row_no IS NULL, upload_batch DESC, import_row_no ASC, id ASC";

    private ScreeningKeyPopulationOrderSupport() {
    }

    public static void applyDisplayOrder(LambdaQueryWrapper<ScreeningKeyPopulation> wrapper) {
        wrapper.last(DISPLAY_ORDER_SQL);
    }
}
