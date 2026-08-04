package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;
import cn.luyou.model.ImportResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 导入 Excel 时检测同一文件内重复身份证号，并提示用户确认。
 */
public final class ImportDuplicateIdSupport {

    private ImportDuplicateIdSupport() {
    }

    public record IdentityRow(int rowNum, String dedupeKey, String idNumber, String name) {
    }

    public static String normalizeIdNumber(String idNumber) {
        String normalized = ImportIdentitySupport.normalizeIdNumber(idNumber);
        if (StrUtil.isBlank(normalized)) {
            return "";
        }
        return normalized.toUpperCase();
    }

    public static <T> List<IdentityRow> collectRows(List<T> items,
                                                    Function<T, String> dedupeKey,
                                                    Function<T, Integer> rowNum,
                                                    Function<T, String> idNumber,
                                                    Function<T, String> name) {
        List<IdentityRow> rows = new ArrayList<>();
        if (items == null) {
            return rows;
        }
        for (T item : items) {
            String key = dedupeKey.apply(item);
            if (StrUtil.isBlank(key)) {
                continue;
            }
            Integer line = rowNum.apply(item);
            rows.add(new IdentityRow(line == null ? 0 : line, key, idNumber.apply(item), name.apply(item)));
        }
        return rows;
    }

    /**
     * 扫描文件内重复身份证；若存在重复且用户尚未确认，则写入结果并阻断导入。
     */
    public static boolean blockIfDuplicateInFile(ImportResult result,
                                                 List<IdentityRow> rows,
                                                 boolean confirmSkipDuplicateInFile) {
        DuplicateScan scan = scan(rows);
        applyScan(result, scan);
        return scan.duplicateInFileCount() > 0 && !confirmSkipDuplicateInFile;
    }

    /**
     * 同一文件内重复身份证保留最后一行，其余行记入提醒。
     */
    public static <T> List<T> dedupeKeepLast(List<T> items,
                                             Function<T, String> dedupeKey,
                                             Function<T, Integer> rowNum,
                                             Function<T, String> name,
                                             ImportResult result) {
        if (items == null || items.isEmpty()) {
            return items == null ? List.of() : items;
        }
        Map<String, T> lastByKey = new LinkedHashMap<>();
        Map<String, List<Integer>> rowsByKey = new LinkedHashMap<>();
        for (T item : items) {
            String key = dedupeKey.apply(item);
            if (StrUtil.isBlank(key)) {
                continue;
            }
            Integer line = rowNum.apply(item);
            int row = line == null ? 0 : line;
            rowsByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
            lastByKey.put(key, item);
        }

        List<T> deduped = new ArrayList<>();
        for (T item : items) {
            String key = dedupeKey.apply(item);
            if (StrUtil.isBlank(key)) {
                deduped.add(item);
                continue;
            }
            T keep = lastByKey.get(key);
            if (keep == item) {
                deduped.add(item);
                List<Integer> lines = rowsByKey.getOrDefault(key, List.of());
                if (lines.size() > 1) {
                    int lastRow = rowNum.apply(item) == null ? 0 : rowNum.apply(item);
                    for (Integer line : lines) {
                        if (!line.equals(lastRow)) {
                            result.addDuplicateInFileWarning(line, name.apply(item), key,
                                    "本表重复身份证，已保留第" + lastRow + "行");
                        }
                    }
                }
            }
        }
        return deduped;
    }

    /**
     * 检测文件内重复身份证；若需阻断返回 null，否则返回去重后的列表（保留最后一行）。
     */
    public static <T> List<T> handleDuplicateInFile(ImportResult result,
                                                    List<T> items,
                                                    Function<T, String> dedupeKey,
                                                    Function<T, Integer> rowNum,
                                                    Function<T, String> idNumber,
                                                    Function<T, String> name,
                                                    boolean confirmSkipDuplicateInFile) {
        List<IdentityRow> rows = collectRows(items, dedupeKey, rowNum, idNumber, name);
        if (blockIfDuplicateInFile(result, rows, confirmSkipDuplicateInFile)) {
            return null;
        }
        if (result.getDuplicateInFileCount() > 0) {
            return dedupeKeepLast(items, dedupeKey, rowNum, name, result);
        }
        return items;
    }

    /**
     * 文件内重复时，返回应跳过的行号 -> 保留行号（保留每组最后一行）。
     */
    public static Map<Integer, Integer> resolveSkipRowsKeepLast(List<IdentityRow> rows) {
        Map<Integer, Integer> skipToKept = new LinkedHashMap<>();
        if (rows == null || rows.isEmpty()) {
            return skipToKept;
        }
        Map<String, List<IdentityRow>> grouped = new LinkedHashMap<>();
        for (IdentityRow row : rows) {
            if (StrUtil.isBlank(row.dedupeKey())) {
                continue;
            }
            grouped.computeIfAbsent(row.dedupeKey(), k -> new ArrayList<>()).add(row);
        }
        for (List<IdentityRow> group : grouped.values()) {
            if (group.size() <= 1) {
                continue;
            }
            int keptRow = group.get(group.size() - 1).rowNum();
            for (int i = 0; i < group.size() - 1; i++) {
                skipToKept.put(group.get(i).rowNum(), keptRow);
            }
        }
        return skipToKept;
    }

    private static DuplicateScan scan(List<IdentityRow> rows) {
        Map<String, List<IdentityRow>> grouped = new LinkedHashMap<>();
        for (IdentityRow row : rows) {
            if (StrUtil.isBlank(row.dedupeKey())) {
                continue;
            }
            grouped.computeIfAbsent(row.dedupeKey(), k -> new ArrayList<>()).add(row);
        }

        int duplicateCount = 0;
        List<String> summaries = new ArrayList<>();
        for (Map.Entry<String, List<IdentityRow>> entry : grouped.entrySet()) {
            List<IdentityRow> group = entry.getValue();
            if (group.size() <= 1) {
                continue;
            }
            duplicateCount += group.size() - 1;
            String displayName = group.stream()
                    .map(IdentityRow::name)
                    .filter(StrUtil::isNotBlank)
                    .findFirst()
                    .orElse("未知");
            String displayId = group.stream()
                    .map(IdentityRow::idNumber)
                    .filter(StrUtil::isNotBlank)
                    .findFirst()
                    .orElse(entry.getKey());
            String rowText = group.stream()
                    .map(IdentityRow::rowNum)
                    .filter(row -> row > 0)
                    .sorted()
                    .map(row -> "第" + row + "行")
                    .reduce((a, b) -> a + "、" + b)
                    .orElse("");
            summaries.add(String.format("身份证号 %s（%s）在本表出现 %d 次：%s",
                    maskId(displayId), displayName, group.size(), rowText));
        }
        return new DuplicateScan(duplicateCount, summaries);
    }

    private static void applyScan(ImportResult result, DuplicateScan scan) {
        result.setDuplicateInFileCount(scan.duplicateInFileCount());
        result.setDuplicateInFileSummaries(scan.summaries());
        if (scan.duplicateInFileCount() > 0) {
            result.setRequireDuplicateInFileConfirm(true);
        }
    }

    private static String maskId(String idNumber) {
        if (StrUtil.isBlank(idNumber) || idNumber.length() < 8) {
            return idNumber;
        }
        return idNumber.substring(0, 4) + "****" + idNumber.substring(idNumber.length() - 4);
    }

    private record DuplicateScan(int duplicateInFileCount, List<String> summaries) {
    }
}
