package cn.luyou.utils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

/**
 * 列表按 Excel 原行号展示。
 * 有 upload_batch 的表：先按批次再按行号；无批次的表仅按行号。
 */
public final class ImportRowOrderSupport {

    public static final String WITH_BATCH =
            "ORDER BY import_row_no IS NULL, upload_batch DESC, import_row_no ASC, id ASC";

    public static final String WITHOUT_BATCH =
            "ORDER BY import_row_no IS NULL, import_row_no ASC, id ASC";

    private ImportRowOrderSupport() {
    }

    public static <T> void applyWithBatch(LambdaQueryWrapper<T> wrapper) {
        wrapper.last(WITH_BATCH);
    }

    public static <T> void applyWithoutBatch(LambdaQueryWrapper<T> wrapper) {
        wrapper.last(WITHOUT_BATCH);
    }
}
