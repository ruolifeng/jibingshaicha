package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 筛查导入批次号：生成可读批次 ID，并为工作台任务下拉提供展示名称。
 */
public final class UploadBatchSupport {

    private static final DateTimeFormatter BATCH_ID_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private UploadBatchSupport() {
    }

    /** 生成可读上传批次号，如：学校筛查_20260702_153045 */
    public static String newBatchId(String prefix) {
        return prefix + "_" + LocalDateTime.now().format(BATCH_ID_TIME);
    }

    public static String buildDisplayLabel(
            String batch,
            Collection<String> populationTypes,
            String year,
            LocalDateTime uploadTime) {
        if (!StrUtil.isNotBlank(batch)) {
            return "";
        }
        String typePart = populationTypes == null ? "" : populationTypes.stream()
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.joining("、"));
        String yearPart = StrUtil.isNotBlank(year) ? year + "年度" : "";
        String datePart = uploadTime != null
                ? uploadTime.toLocalDate().toString()
                : "";

        if (StrUtil.isNotBlank(yearPart) && StrUtil.isNotBlank(typePart) && StrUtil.isNotBlank(datePart)) {
            return yearPart + " " + typePart + "（" + datePart + "）";
        }
        if (StrUtil.isNotBlank(typePart) && StrUtil.isNotBlank(datePart)) {
            return typePart + "（" + datePart + "）";
        }
        if (StrUtil.isNotBlank(typePart)) {
            return typePart;
        }
        if (isUuidLike(batch) && StrUtil.isNotBlank(datePart)) {
            return "筛查任务（" + datePart + "）";
        }
        return batch;
    }

    public static boolean isUuidLike(String value) {
        if (!StrUtil.isNotBlank(value) || value.length() < 32) {
            return false;
        }
        return value.matches("[0-9a-fA-F\\-]+");
    }

    public static class BatchMeta {
        private final Set<String> populationTypes = new LinkedHashSet<>();
        private String year;
        private LocalDateTime uploadTime;
        private long count;

        public void merge(String populationLabel, String yearValue, LocalDateTime time, long rowCount) {
            if (StrUtil.isNotBlank(populationLabel)) {
                populationTypes.add(populationLabel);
            }
            if (StrUtil.isNotBlank(yearValue)) {
                year = yearValue;
            }
            if (time != null && (uploadTime == null || time.isBefore(uploadTime))) {
                uploadTime = time;
            }
            count += rowCount;
        }

        public LocalDateTime getUploadTime() {
            return uploadTime;
        }

        public String toLabel(String batch) {
            return buildDisplayLabel(batch, populationTypes, year, uploadTime);
        }
    }
}
