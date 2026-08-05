package cn.luyou.service.export;

import cn.luyou.model.Question;
import cn.luyou.model.QuestionnaireAnswer;
import cn.luyou.model.QuestionnaireResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 问卷数据导出：三行表头（题干 / 变量名 / 子项）、JSON 拆解。
 * LABEL 模式导出选项文字/填答内容；SPSS 模式导出序号与哑变量编码。
 */
@Component
@RequiredArgsConstructor
public class QuestionnaireSpssExportHelper {

    public enum ExportValueFormat {
        /** 可读文本：选项标签、填空内容 */
        LABEL,
        /** SPSS 编码：选项序号、0/1 哑变量 */
        SPSS;

        public static ExportValueFormat parse(String raw) {
            if (raw != null && "spss".equalsIgnoreCase(raw.trim())) {
                return SPSS;
            }
            return LABEL;
        }

        public boolean isLabel() {
            return this == LABEL;
        }
    }

    private static final Pattern TITLE_CODE = Pattern.compile("^([A-Za-z][A-Za-z0-9]*)\\b");
    /** 与问卷设计器 Q 编号一致：仅跳过分割线、分页（description/image_display 占题号） */
    private static final Set<String> SKIP_TYPES = Set.of("divider", "page_break");

    private final ObjectMapper objectMapper;

    /** varName=变量名；groupLabel=题干（同题多列时合并）；subLabel=子项标签（填空字段/矩阵行等） */
    public record ExportColumn(String varName, String groupLabel, String subLabel, String valueKey) {
    }

    public record ExportSchema(
            List<ExportColumn> columns,
            Map<Long, String> questionVarBase,
            Map<Long, Integer> dynamicTableMaxRows,
            ExportValueFormat format) {
        /** 三行表头：题干 / 变量名(Q几) / 子项 */
        public List<List<String>> buildHeadRows() {
            List<List<String>> head = new ArrayList<>(columns.size());
            for (ExportColumn c : columns) {
                head.add(List.of(c.groupLabel(), c.varName(), c.subLabel()));
            }
            return head;
        }
    }

    public ExportSchema buildSchema(
            List<Question> questions,
            Map<Long, List<QuestionnaireAnswer>> answersByResponse,
            ExportValueFormat format) {
        Map<Long, Integer> dynamicTableMaxRows = computeDynamicTableMaxRows(questions, answersByResponse);
        List<ExportColumn> cols = new ArrayList<>();
        cols.add(new ExportColumn("_ID", "记录编号", "", "_ID"));
        cols.add(new ExportColumn("_STATUS", "提交状态", "", "_STATUS"));
        cols.add(new ExportColumn("_START", "开始时间", "", "_START"));
        cols.add(new ExportColumn("_SUBMIT", "提交时间", "", "_SUBMIT"));
        cols.add(new ExportColumn("_DURATION", "填写时长(秒)", "", "_DURATION"));
        cols.add(new ExportColumn("_IP", "答卷IP", "", "_IP"));

        Set<String> usedVarNames = new HashSet<>();
        Map<Long, String> questionVarBase = new LinkedHashMap<>();
        int qIndex = 0;
        for (Question q : questions.stream()
                .filter(q -> q.getType() != null && !SKIP_TYPES.contains(q.getType()))
                .sorted(Comparator.comparingInt(q -> q.getSortOrder() != null ? q.getSortOrder() : 0))
                .toList()) {
            qIndex++;
            String base = uniqueVarName("Q" + qIndex, q, usedVarNames);
            if (q.getId() != null) {
                questionVarBase.put(q.getId(), base);
            }
            appendQuestionColumns(q, base, cols, dynamicTableMaxRows.getOrDefault(q.getId(), 5), format);
        }

        return new ExportSchema(cols, questionVarBase, dynamicTableMaxRows, format);
    }

    public ExportSchema buildSchema(List<Question> questions, Map<Long, List<QuestionnaireAnswer>> answersByResponse) {
        return buildSchema(questions, answersByResponse, ExportValueFormat.LABEL);
    }

    public ExportSchema buildSchema(List<Question> questions) {
        return buildSchema(questions, Map.of(), ExportValueFormat.LABEL);
    }

    public ExportSchema buildSchema(List<Question> questions, ExportValueFormat format) {
        return buildSchema(questions, Map.of(), format);
    }

    public List<List<Object>> buildDataRows(
            ExportSchema schema,
            List<Question> questions,
            List<QuestionnaireResponse> responses,
            Map<Long, List<QuestionnaireAnswer>> answersByResponse) {

        List<List<Object>> rows = new ArrayList<>(responses.size());
        for (QuestionnaireResponse r : responses) {
            Map<Long, String> answerMap = new HashMap<>();
            for (QuestionnaireAnswer a : answersByResponse.getOrDefault(r.getId(), List.of())) {
                answerMap.put(a.getQuestionId(), a.getAnswerValue() != null ? a.getAnswerValue() : "");
            }

            Map<String, Object> values = new LinkedHashMap<>();
            values.put("_ID", cleanCell(r.getId()));
            values.put("_STATUS", cleanCell(formatStatus(r.getStatus())));
            values.put("_START", cleanCell(r.getStartTime() != null ? r.getStartTime().toString() : ""));
            values.put("_SUBMIT", cleanCell(r.getSubmitTime() != null ? r.getSubmitTime().toString() : ""));
            values.put("_DURATION", cleanCell(r.getDurationSeconds()));
            values.put("_IP", cleanCell(r.getRespondentIp()));

            Set<Long> answeredQuestionIds = answerMap.keySet();

            for (Question q : questions) {
                if (q.getId() == null || q.getType() == null || SKIP_TYPES.contains(q.getType())) {
                    continue;
                }
                boolean answerPresent = answeredQuestionIds.contains(q.getId());
                String raw = answerPresent ? answerMap.get(q.getId()) : "";
                String base = schema.questionVarBase().getOrDefault(q.getId(), extractVarName(q));
                int dynMax = schema.dynamicTableMaxRows().getOrDefault(q.getId(), 5);
                values.putAll(extractQuestionValues(q, raw, base, dynMax, answerPresent, schema.format()));
            }

            List<Object> row = new ArrayList<>(schema.columns().size());
            for (ExportColumn col : schema.columns()) {
                row.add(formatExcelCell(values.get(col.valueKey())));
            }
            rows.add(row);
        }
        return rows;
    }

    private void appendQuestionColumns(
            Question q, String base, List<ExportColumn> cols, int dynamicMaxRows, ExportValueFormat format) {
        String groupLabel = formatGroupLabel(base, q);
        String type = q.getType();
        boolean labelMode = format.isLabel();

        switch (type) {
            case "radio", "dropdown", "image_choice" -> {
                if ("image_choice".equals(type) && isMultiImageChoice(q)) {
                    if (labelMode) {
                        cols.add(new ExportColumn(base, groupLabel, "", base));
                    } else {
                        appendCheckboxDummyColumns(q, base, groupLabel, cols);
                    }
                } else {
                    cols.add(new ExportColumn(base, groupLabel, "", base));
                    if (hasOptionInput(q) && !labelMode) {
                        cols.add(new ExportColumn(base + "_other", groupLabel, "其它填答", base + "_other"));
                    }
                }
            }
            case "checkbox" -> {
                if (labelMode) {
                    cols.add(new ExportColumn(base, groupLabel, "", base));
                } else {
                    appendCheckboxDummyColumns(q, base, groupLabel, cols);
                }
            }
            case "matrix_radio" -> {
                List<OptionItem> rows = parseMatrixRows(q);
                for (int i = 0; i < rows.size(); i++) {
                    String key = base + "_r" + (i + 1);
                    cols.add(new ExportColumn(key, groupLabel, optionLabel(rows.get(i)), key));
                }
            }
            case "matrix_checkbox" -> {
                List<OptionItem> mRows = parseMatrixRows(q);
                List<OptionItem> mCols = parseMatrixCols(q);
                if (labelMode) {
                    for (int ri = 0; ri < mRows.size(); ri++) {
                        String key = base + "_r" + (ri + 1);
                        cols.add(new ExportColumn(key, groupLabel, optionLabel(mRows.get(ri)), key));
                    }
                } else {
                    for (int ri = 0; ri < mRows.size(); ri++) {
                        for (int ci = 0; ci < mCols.size(); ci++) {
                            String key = base + "_r" + (ri + 1) + "_c" + (ci + 1);
                            String sub = optionLabel(mRows.get(ri)) + " - " + optionLabel(mCols.get(ci));
                            cols.add(new ExportColumn(key, groupLabel, sub, key));
                        }
                    }
                }
            }
            case "matrix_input" -> {
                List<OptionItem> mRows = parseMatrixRows(q);
                List<OptionItem> mCols = parseMatrixCols(q);
                for (int ri = 0; ri < mRows.size(); ri++) {
                    for (int ci = 0; ci < mCols.size(); ci++) {
                        String key = base + "_r" + (ri + 1) + "_c" + (ci + 1);
                        String sub = optionLabel(mRows.get(ri)) + " - " + optionLabel(mCols.get(ci));
                        cols.add(new ExportColumn(key, groupLabel, sub, key));
                    }
                }
            }
            case "matrix_scale" -> {
                List<OptionItem> scaleRows = parseScaleRows(q);
                for (int i = 0; i < scaleRows.size(); i++) {
                    String key = base + "_r" + (i + 1);
                    cols.add(new ExportColumn(key, groupLabel, optionLabel(scaleRows.get(i)), key));
                }
            }
            case "matrix_complex" -> appendMatrixComplexColumns(q, base, groupLabel, cols);
            case "multi_input", "inline_input" -> {
                List<OptionItem> fields = parseFieldOptions(q).stream()
                        .filter(f -> !Boolean.TRUE.equals(f.inputHidden))
                        .toList();
                for (int i = 0; i < fields.size(); i++) {
                    OptionItem f = fields.get(i);
                    String key = base + "_" + (i + 1);
                    cols.add(new ExportColumn(key, groupLabel, optionLabel(f), key));
                }
            }
            case "cascader" -> {
                cols.add(new ExportColumn(base + "_1", groupLabel, "一级", base + "_1"));
                cols.add(new ExportColumn(base + "_2", groupLabel, "二级", base + "_2"));
                cols.add(new ExportColumn(base + "_3", groupLabel, "三级", base + "_3"));
            }
            case "date" -> appendDateColumns(q, base, groupLabel, cols);
            case "sort" -> {
                if (labelMode) {
                    cols.add(new ExportColumn(base, groupLabel, "", base));
                } else {
                    appendSortColumns(q, base, groupLabel, cols);
                }
            }
            case "file_upload", "image_upload" -> cols.add(new ExportColumn(base, groupLabel, "", base));
            case "description" -> cols.add(new ExportColumn(base, groupLabel, descriptionSubLabel(q), base));
            case "image_display" -> cols.add(new ExportColumn(base, groupLabel, "", base));
            case "dynamic_table" -> appendDynamicTableColumns(q, base, groupLabel, cols, dynamicMaxRows);
            default -> cols.add(new ExportColumn(base, groupLabel, "", base));
        }
    }

    private void appendCheckboxDummyColumns(Question q, String base, String groupLabel, List<ExportColumn> cols) {
        List<OptionItem> options = parseChoiceOptions(q);
        for (int i = 0; i < options.size(); i++) {
            OptionItem opt = options.get(i);
            String suffix = String.valueOf(i + 1);
            cols.add(new ExportColumn(base + "_" + suffix, groupLabel, optionLabel(opt), base + "_" + suffix));
            if (Boolean.TRUE.equals(opt.hasInput)) {
                cols.add(new ExportColumn(base + "_" + suffix + "_other", groupLabel,
                        optionLabel(opt) + "（填答）", base + "_" + suffix + "_other"));
            }
        }
    }

    private void appendSortColumns(Question q, String base, String groupLabel, List<ExportColumn> cols) {
        List<OptionItem> options = parseChoiceOptions(q);
        for (int i = 0; i < options.size(); i++) {
            String key = base + "_" + (i + 1);
            cols.add(new ExportColumn(key, groupLabel, optionLabel(options.get(i)), key));
        }
    }

    private void appendDateColumns(Question q, String base, String groupLabel, List<ExportColumn> cols) {
        if (isDateRange(q)) {
            cols.add(new ExportColumn(base + "_1", groupLabel, "开始日期", base + "_1"));
            cols.add(new ExportColumn(base + "_2", groupLabel, "结束日期", base + "_2"));
        } else {
            cols.add(new ExportColumn(base, groupLabel, "", base));
        }
    }

    private void appendMatrixComplexColumns(Question q, String base, String groupLabel, List<ExportColumn> cols) {
        MatrixComplexOpts opts = parseMatrixComplexOpts(q);
        for (int ri = 0; ri < opts.rows.size(); ri++) {
            for (MCCol col : opts.columns) {
                String key = base + "_r" + (ri + 1) + "_" + sanitizeSuffix(col.key);
                String sub = optionLabel(opts.rows.get(ri)) + " - " + col.key;
                cols.add(new ExportColumn(key, groupLabel, sub, key));
            }
        }
    }

    private void appendDynamicTableColumns(Question q, String base, String groupLabel, List<ExportColumn> cols, int maxRows) {
        List<DynamicCol> columns = parseDynamicTableColumns(q);
        int rows = Math.max(1, maxRows);
        for (int ri = 0; ri < rows; ri++) {
            for (DynamicCol col : columns) {
                String key = base + "_row" + (ri + 1) + "_" + sanitizeSuffix(col.key);
                String sub = "第" + (ri + 1) + "行 - " + col.key;
                cols.add(new ExportColumn(key, groupLabel, sub, key));
            }
        }
    }

    private Map<String, Object> extractQuestionValues(
            Question q, String raw, String base, int dynamicMaxRows, boolean answerPresent, ExportValueFormat format) {
        if (!answerPresent) {
            return defaultMissingValues(q, base, dynamicMaxRows, format);
        }
        if (raw == null || raw.isBlank() || "{}".equals(raw.trim()) || "[]".equals(raw.trim())) {
            return defaultEmptyValues(q, base, dynamicMaxRows, format);
        }
        String type = q.getType();
        boolean labelMode = format.isLabel();
        return switch (type) {
            case "radio", "dropdown" -> extractRadioValues(q, raw, base, format);
            case "checkbox" -> labelMode ? extractCheckboxLabelValues(q, raw, base) : extractCheckboxDummyValues(q, raw, base);
            case "image_choice" -> isMultiImageChoice(q)
                    ? (labelMode ? extractCheckboxLabelValues(q, raw, base) : extractCheckboxDummyValues(q, raw, base))
                    : extractRadioValues(q, raw, base, format);
            case "matrix_radio" -> extractMatrixRadioValues(q, raw, base, format);
            case "matrix_checkbox" -> labelMode
                    ? extractMatrixCheckboxLabelValues(q, raw, base)
                    : extractMatrixCheckboxDummyValues(q, raw, base);
            case "matrix_input" -> extractMatrixInputValues(q, raw, base);
            case "matrix_scale" -> extractMatrixScaleValues(q, raw, base);
            case "matrix_complex" -> extractMatrixComplexValues(q, raw, base, format);
            case "multi_input", "inline_input" -> extractMultiInputValues(q, raw, base);
            case "cascader" -> extractCascaderValues(q, raw, base, format);
            case "date" -> extractDateValues(q, raw, base);
            case "sort" -> labelMode ? extractSortLabelValues(q, raw, base) : extractSortValues(q, raw, base);
            case "file_upload", "image_upload" -> Map.of(base, flattenUploadValue(raw));
            case "description", "image_display" -> Map.of(base, "");
            case "dynamic_table" -> extractDynamicTableValues(q, raw, base, Math.max(1, dynamicMaxRows));
            default -> Map.of(base, parsePlainAnswer(raw, q));
        };
    }

    private Map<String, Object> defaultEmptyValues(Question q, String base, int dynamicMaxRows, ExportValueFormat format) {
        Map<String, Object> empty = new LinkedHashMap<>();
        String type = q.getType();
        if (type == null) {
            empty.put(base, "");
            return empty;
        }
        boolean labelMode = format.isLabel();
        switch (type) {
            case "radio", "dropdown" -> {
                empty.put(base, "");
                if (hasOptionInput(q) && !labelMode) empty.put(base + "_other", "");
            }
            case "checkbox" -> {
                if (labelMode) {
                    empty.put(base, "");
                } else {
                    fillCheckboxEmpty(q, base, empty);
                }
            }
            case "image_choice" -> {
                if (isMultiImageChoice(q)) {
                    if (labelMode) empty.put(base, "");
                    else fillCheckboxEmpty(q, base, empty);
                } else {
                    empty.put(base, "");
                    if (hasOptionInput(q) && !labelMode) empty.put(base + "_other", "");
                }
            }
            case "matrix_radio" -> {
                for (int i = 0; i < parseMatrixRows(q).size(); i++) {
                    empty.put(base + "_r" + (i + 1), "");
                }
            }
            case "matrix_checkbox" -> {
                List<OptionItem> rows = parseMatrixRows(q);
                List<OptionItem> cols = parseMatrixCols(q);
                if (labelMode) {
                    for (int ri = 0; ri < rows.size(); ri++) {
                        empty.put(base + "_r" + (ri + 1), "");
                    }
                } else {
                    for (int ri = 0; ri < rows.size(); ri++) {
                        for (int ci = 0; ci < cols.size(); ci++) {
                            empty.put(base + "_r" + (ri + 1) + "_c" + (ci + 1), 0);
                        }
                    }
                }
            }
            case "matrix_input" -> {
                List<OptionItem> rows = parseMatrixRows(q);
                List<OptionItem> cols = parseMatrixCols(q);
                for (int ri = 0; ri < rows.size(); ri++) {
                    for (int ci = 0; ci < cols.size(); ci++) {
                        empty.put(base + "_r" + (ri + 1) + "_c" + (ci + 1), "");
                    }
                }
            }
            case "matrix_scale" -> {
                for (int i = 0; i < parseScaleRows(q).size(); i++) {
                    empty.put(base + "_r" + (i + 1), "");
                }
            }
            case "matrix_complex" -> {
                MatrixComplexOpts opts = parseMatrixComplexOpts(q);
                for (int ri = 0; ri < opts.rows.size(); ri++) {
                    for (MCCol col : opts.columns) {
                        empty.put(base + "_r" + (ri + 1) + "_" + sanitizeSuffix(col.key), "");
                    }
                }
            }
            case "multi_input", "inline_input" -> {
                List<OptionItem> fields = visibleFieldOptions(q);
                for (int i = 0; i < fields.size(); i++) {
                    empty.put(base + "_" + (i + 1), "");
                }
            }
            case "cascader" -> {
                empty.put(base + "_1", "");
                empty.put(base + "_2", "");
                empty.put(base + "_3", "");
            }
            case "date" -> fillDateEmpty(q, base, empty);
            case "sort" -> {
                if (labelMode) {
                    empty.put(base, "");
                } else {
                    fillSortEmpty(q, base, empty);
                }
            }
            case "file_upload", "image_upload" -> empty.put(base, "");
            case "description", "image_display" -> empty.put(base, "");
            case "dynamic_table" -> {
                List<DynamicCol> columns = parseDynamicTableColumns(q);
                int rows = Math.max(1, dynamicMaxRows);
                for (int ri = 0; ri < rows; ri++) {
                    for (DynamicCol col : columns) {
                        empty.put(base + "_row" + (ri + 1) + "_" + sanitizeSuffix(col.key), "");
                    }
                }
            }
            default -> empty.put(base, "");
        }
        return empty;
    }

    /** 跳题/未提交：全部留空（含多选哑变量，不用 0 填充） */
    private Map<String, Object> defaultMissingValues(Question q, String base, int dynamicMaxRows, ExportValueFormat format) {
        Map<String, Object> missing = new LinkedHashMap<>();
        String type = q.getType();
        if (type == null) {
            missing.put(base, "");
            return missing;
        }
        boolean labelMode = format.isLabel();
        switch (type) {
            case "radio", "dropdown", "image_choice" -> {
                if ("image_choice".equals(type) && isMultiImageChoice(q)) {
                    if (labelMode) missing.put(base, "");
                    else fillCheckboxMissing(q, base, missing);
                } else {
                    missing.put(base, "");
                    if (hasOptionInput(q) && !labelMode) missing.put(base + "_other", "");
                }
            }
            case "checkbox" -> {
                if (labelMode) missing.put(base, "");
                else fillCheckboxMissing(q, base, missing);
            }
            case "matrix_radio", "matrix_scale" -> {
                List<OptionItem> rows = "matrix_scale".equals(type) ? parseScaleRows(q) : parseMatrixRows(q);
                for (int i = 0; i < rows.size(); i++) {
                    missing.put(base + "_r" + (i + 1), "");
                }
            }
            case "matrix_checkbox", "matrix_input" -> {
                List<OptionItem> rows = parseMatrixRows(q);
                List<OptionItem> cols = parseMatrixCols(q);
                if ("matrix_checkbox".equals(type) && labelMode) {
                    for (int ri = 0; ri < rows.size(); ri++) {
                        missing.put(base + "_r" + (ri + 1), "");
                    }
                } else {
                    for (int ri = 0; ri < rows.size(); ri++) {
                        for (int ci = 0; ci < cols.size(); ci++) {
                            missing.put(base + "_r" + (ri + 1) + "_c" + (ci + 1), "");
                        }
                    }
                }
            }
            case "matrix_complex" -> {
                MatrixComplexOpts opts = parseMatrixComplexOpts(q);
                for (int ri = 0; ri < opts.rows.size(); ri++) {
                    for (MCCol col : opts.columns) {
                        missing.put(base + "_r" + (ri + 1) + "_" + sanitizeSuffix(col.key), "");
                    }
                }
            }
            case "multi_input", "inline_input" -> {
                List<OptionItem> fields = visibleFieldOptions(q);
                for (int i = 0; i < fields.size(); i++) {
                    missing.put(base + "_" + (i + 1), "");
                }
            }
            case "cascader" -> {
                missing.put(base + "_1", "");
                missing.put(base + "_2", "");
                missing.put(base + "_3", "");
            }
            case "date" -> fillDateEmpty(q, base, missing);
            case "sort" -> {
                if (labelMode) missing.put(base, "");
                else fillSortEmpty(q, base, missing);
            }
            case "file_upload", "image_upload" -> missing.put(base, "");
            case "description", "image_display" -> missing.put(base, "");
            case "dynamic_table" -> {
                List<DynamicCol> columns = parseDynamicTableColumns(q);
                int rows = Math.max(1, dynamicMaxRows);
                for (int ri = 0; ri < rows; ri++) {
                    for (DynamicCol col : columns) {
                        missing.put(base + "_row" + (ri + 1) + "_" + sanitizeSuffix(col.key), "");
                    }
                }
            }
            default -> missing.put(base, "");
        }
        return missing;
    }

    private void fillCheckboxMissing(Question q, String base, Map<String, Object> missing) {
        List<OptionItem> options = parseChoiceOptions(q);
        for (int i = 0; i < options.size(); i++) {
            OptionItem opt = options.get(i);
            String suffix = String.valueOf(i + 1);
            missing.put(base + "_" + suffix, "");
            if (Boolean.TRUE.equals(opt.hasInput)) {
                missing.put(base + "_" + suffix + "_other", "");
            }
        }
    }

    private void fillCheckboxEmpty(Question q, String base, Map<String, Object> empty) {
        List<OptionItem> options = parseChoiceOptions(q);
        for (int i = 0; i < options.size(); i++) {
            OptionItem opt = options.get(i);
            String suffix = String.valueOf(i + 1);
            empty.put(base + "_" + suffix, 0);
            if (Boolean.TRUE.equals(opt.hasInput)) {
                empty.put(base + "_" + suffix + "_other", "");
            }
        }
    }

    private void fillSortEmpty(Question q, String base, Map<String, Object> empty) {
        List<OptionItem> options = parseChoiceOptions(q);
        for (int i = 0; i < options.size(); i++) {
            empty.put(base + "_" + (i + 1), "");
        }
    }

    private void fillDateEmpty(Question q, String base, Map<String, Object> empty) {
        if (isDateRange(q)) {
            empty.put(base + "_1", "");
            empty.put(base + "_2", "");
        } else {
            empty.put(base, "");
        }
    }

    private Map<String, Object> extractRadioValues(Question q, String raw, String base, ExportValueFormat format) {
        Map<String, Object> out = new LinkedHashMap<>();
        SelectedInputs si = parseSelectedInputs(raw);
        String selected = si.selectedAsString();
        List<OptionItem> options = parseChoiceOptions(q);
        if (selected.isBlank()) {
            out.put(base, "");
        } else if (format.isLabel()) {
            String text = resolveOptionLabel(selected, options);
            OptionItem picked = findOptionByValue(selected, options);
            if (picked != null && Boolean.TRUE.equals(picked.hasInput)) {
                String other = si.inputs.getOrDefault(selected, "");
                if (other != null && !other.isBlank()) {
                    text = text + "：" + other.trim();
                }
            }
            out.put(base, text);
        } else {
            out.put(base, toOptionInt(selected, options));
            if (hasOptionInput(q)) {
                String other = selected.isBlank() ? "" : si.inputs.getOrDefault(selected, "");
                out.put(base + "_other", other);
            }
        }
        return out;
    }

    private Map<String, Object> extractCheckboxLabelValues(Question q, String raw, String base) {
        Map<String, Object> out = new LinkedHashMap<>();
        SelectedInputs si = parseSelectedInputs(raw);
        Set<String> selected = si.selectedAsSet();
        List<OptionItem> options = parseChoiceOptions(q);
        List<String> parts = new ArrayList<>();
        for (OptionItem opt : options) {
            if (!selected.contains(opt.value)) continue;
            String text = optionLabel(opt);
            if (Boolean.TRUE.equals(opt.hasInput)) {
                String other = si.inputs.getOrDefault(opt.value, "");
                if (other != null && !other.isBlank()) {
                    text = text + "：" + other.trim();
                }
            }
            parts.add(text);
        }
        out.put(base, String.join("；", parts));
        return out;
    }

    private Map<String, Object> extractCheckboxDummyValues(Question q, String raw, String base) {
        Map<String, Object> out = new LinkedHashMap<>();
        SelectedInputs si = parseSelectedInputs(raw);
        Set<String> selected = si.selectedAsSet();
        List<OptionItem> options = parseChoiceOptions(q);
        for (int i = 0; i < options.size(); i++) {
            OptionItem opt = options.get(i);
            String suffix = String.valueOf(i + 1);
            String key = base + "_" + suffix;
            out.put(key, selected.contains(opt.value) ? 1 : 0);
            if (Boolean.TRUE.equals(opt.hasInput)) {
                out.put(key + "_other", selected.contains(opt.value) ? si.inputs.getOrDefault(opt.value, "") : "");
            }
        }
        return out;
    }

    private Map<String, Object> extractMatrixRadioValues(Question q, String raw, String base, ExportValueFormat format) {
        Map<String, Object> out = new LinkedHashMap<>();
        List<OptionItem> rows = parseMatrixRows(q);
        List<OptionItem> cols = parseMatrixCols(q);
        Map<String, String> obj = parseStringMap(raw);
        for (int ri = 0; ri < rows.size(); ri++) {
            String colVal = obj.getOrDefault(matrixSimpleRowKey(rows, ri), "");
            String key = base + "_r" + (ri + 1);
            if (colVal.isBlank()) {
                out.put(key, "");
            } else if (format.isLabel()) {
                out.put(key, resolveOptionLabel(colVal, cols));
            } else {
                out.put(key, toColIndex(colVal, cols));
            }
        }
        return out;
    }

    private Map<String, Object> extractMatrixCheckboxLabelValues(Question q, String raw, String base) {
        Map<String, Object> out = new LinkedHashMap<>();
        List<OptionItem> rows = parseMatrixRows(q);
        List<OptionItem> cols = parseMatrixCols(q);
        Map<String, Object> obj = parseObjectMap(raw);
        for (int ri = 0; ri < rows.size(); ri++) {
            Set<String> picked = toStringSet(obj.get(matrixSimpleRowKey(rows, ri)));
            List<String> labels = new ArrayList<>();
            for (OptionItem col : cols) {
                if (picked.contains(col.value)) {
                    labels.add(optionLabel(col));
                }
            }
            out.put(base + "_r" + (ri + 1), String.join("；", labels));
        }
        return out;
    }

    private Map<String, Object> extractMatrixCheckboxDummyValues(Question q, String raw, String base) {
        Map<String, Object> out = new LinkedHashMap<>();
        List<OptionItem> rows = parseMatrixRows(q);
        List<OptionItem> cols = parseMatrixCols(q);
        Map<String, Object> obj = parseObjectMap(raw);
        for (int ri = 0; ri < rows.size(); ri++) {
            Set<String> picked = toStringSet(obj.get(matrixSimpleRowKey(rows, ri)));
            for (int ci = 0; ci < cols.size(); ci++) {
                String key = base + "_r" + (ri + 1) + "_c" + (ci + 1);
                out.put(key, picked.contains(cols.get(ci).value) ? 1 : 0);
            }
        }
        return out;
    }

    private Map<String, Object> extractMatrixScaleValues(Question q, String raw, String base) {
        Map<String, Object> out = new LinkedHashMap<>();
        List<OptionItem> rows = parseScaleRows(q);
        Map<String, String> obj = parseStringMap(raw);
        for (int ri = 0; ri < rows.size(); ri++) {
            String key = base + "_r" + (ri + 1);
            out.put(key, parseScalar(obj.getOrDefault(matrixSimpleRowKey(rows, ri), ""), q));
        }
        return out;
    }

    private List<OptionItem> parseScaleRows(Question q) {
        try {
            JsonNode node = objectMapper.readTree(q.getOptions());
            if (node.has("rows")) {
                return objectMapper.convertValue(node.get("rows"), new TypeReference<>() {});
            }
        } catch (Exception ignored) {
        }
        return List.of();
    }

    private Map<String, Object> extractMatrixInputValues(Question q, String raw, String base) {
        Map<String, Object> out = new LinkedHashMap<>();
        List<OptionItem> rows = parseMatrixRows(q);
        List<OptionItem> cols = parseMatrixCols(q);
        Map<String, String> flat = parseStringMap(raw);
        for (int ri = 0; ri < rows.size(); ri++) {
            for (int ci = 0; ci < cols.size(); ci++) {
                String compositeKey = matrixSimpleRowKey(rows, ri) + "__" + cols.get(ci).value;
                String key = base + "_r" + (ri + 1) + "_c" + (ci + 1);
                out.put(key, parseScalar(flat.getOrDefault(compositeKey, ""), q));
            }
        }
        return out;
    }

    private Map<String, Object> extractMatrixComplexValues(Question q, String raw, String base, ExportValueFormat format) {
        Map<String, Object> out = new LinkedHashMap<>();
        MatrixComplexOpts opts = parseMatrixComplexOpts(q);
        Map<String, Map<String, Object>> all = parseNestedMap(raw);
        for (int ri = 0; ri < opts.rows.size(); ri++) {
            Map<String, Object> rowData = getMatrixComplexRowData(all, opts.rows, ri);
            for (MCCol col : opts.columns) {
                String key = base + "_r" + (ri + 1) + "_" + sanitizeSuffix(col.key);
                Object cell = rowData.get(col.key);
                out.put(key, formatMatrixComplexCell(cell, col, format));
            }
        }
        return out;
    }

    private Map<String, Object> extractSortLabelValues(Question q, String raw, String base) {
        Map<String, Object> out = new LinkedHashMap<>();
        List<String> order = parseStringList(raw);
        List<OptionItem> options = parseChoiceOptions(q);
        Map<String, OptionItem> byValue = new HashMap<>();
        for (OptionItem opt : options) {
            byValue.put(opt.value, opt);
        }
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < order.size(); i++) {
            OptionItem opt = byValue.get(order.get(i));
            String name = opt != null ? optionLabel(opt) : order.get(i);
            parts.add((i + 1) + "." + name);
        }
        out.put(base, String.join(" ", parts));
        return out;
    }

    private Map<String, Object> extractMultiInputValues(Question q, String raw, String base) {
        Map<String, Object> out = new LinkedHashMap<>();
        Map<String, String> obj = parseStringMap(raw);
        List<OptionItem> fields = visibleFieldOptions(q);
        for (int i = 0; i < fields.size(); i++) {
            OptionItem f = fields.get(i);
            String key = base + "_" + (i + 1);
            out.put(key, parseScalar(obj.getOrDefault(f.value, ""), q));
        }
        return out;
    }

    private Map<String, Object> extractCascaderValues(Question q, String raw, String base, ExportValueFormat format) {
        Map<String, Object> out = new LinkedHashMap<>();
        List<String> levels = parseStringList(raw);
        List<String> display = format.isLabel() ? resolveCascaderLabels(q, levels) : levels;
        for (int i = 0; i < 3; i++) {
            String val = i < display.size() ? display.get(i) : "";
            out.put(base + "_" + (i + 1), parseScalar(val, q));
        }
        return out;
    }

    private List<String> resolveCascaderLabels(Question q, List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(q.getOptions() != null ? q.getOptions() : "[]");
            if (!root.isArray()) {
                return values;
            }
            List<String> labels = new ArrayList<>();
            JsonNode levelNodes = root;
            for (String val : values) {
                if (val == null || val.isBlank()) {
                    labels.add("");
                    continue;
                }
                JsonNode matched = null;
                for (JsonNode node : levelNodes) {
                    if (val.equals(node.path("value").asText())) {
                        matched = node;
                        break;
                    }
                }
                if (matched == null) {
                    labels.add(val);
                    levelNodes = objectMapper.createArrayNode();
                    continue;
                }
                String label = matched.path("label").asText("");
                labels.add(label.isBlank() ? val : label);
                levelNodes = matched.path("children");
                if (!levelNodes.isArray()) {
                    levelNodes = objectMapper.createArrayNode();
                }
            }
            return labels;
        } catch (Exception e) {
            return values;
        }
    }

    private Map<String, Object> extractDateValues(Question q, String raw, String base) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (isDateRange(q)) {
            List<String> parts = parseStringList(raw);
            out.put(base + "_1", parts.size() > 0 ? parts.get(0) : "");
            out.put(base + "_2", parts.size() > 1 ? parts.get(1) : "");
        } else {
            out.put(base, parsePlainAnswer(raw, q));
        }
        return out;
    }

    private Map<String, Object> extractSortValues(Question q, String raw, String base) {
        Map<String, Object> out = new LinkedHashMap<>();
        List<String> order = parseStringList(raw);
        Map<String, Integer> rankByValue = new HashMap<>();
        for (int i = 0; i < order.size(); i++) {
            rankByValue.put(order.get(i), i + 1);
        }
        List<OptionItem> options = parseChoiceOptions(q);
        for (int i = 0; i < options.size(); i++) {
            Integer rank = rankByValue.get(options.get(i).value);
            out.put(base + "_" + (i + 1), rank != null ? rank : "");
        }
        return out;
    }

    private Map<String, Object> extractDynamicTableValues(Question q, String raw, String base, int maxRows) {
        Map<String, Object> out = new LinkedHashMap<>();
        List<DynamicCol> columns = parseDynamicTableColumns(q);
        List<Map<String, String>> tableRows = parseTableRows(raw);
        int rows = Math.max(maxRows, tableRows.size());
        for (int ri = 0; ri < rows; ri++) {
            Map<String, String> row = ri < tableRows.size() ? tableRows.get(ri) : Map.of();
            for (DynamicCol col : columns) {
                String key = base + "_row" + (ri + 1) + "_" + sanitizeSuffix(col.key);
                out.put(key, parseScalar(row.getOrDefault(col.key, ""), q));
            }
        }
        return out;
    }

    private Map<Long, Integer> computeDynamicTableMaxRows(
            List<Question> questions, Map<Long, List<QuestionnaireAnswer>> answersByResponse) {
        Map<Long, Integer> maxRows = new HashMap<>();
        Set<Long> dynamicIds = questions.stream()
                .filter(q -> "dynamic_table".equals(q.getType()) && q.getId() != null)
                .map(Question::getId)
                .collect(Collectors.toSet());
        for (List<QuestionnaireAnswer> answers : answersByResponse.values()) {
            for (QuestionnaireAnswer a : answers) {
                if (!dynamicIds.contains(a.getQuestionId())) continue;
                int size = parseTableRows(a.getAnswerValue()).size();
                maxRows.merge(a.getQuestionId(), Math.max(1, size), Math::max);
            }
        }
        return maxRows;
    }

    private String uniqueVarName(String base, Question q, Set<String> usedVarNames) {
        if (!usedVarNames.contains(base)) {
            usedVarNames.add(base);
            return base;
        }
        String candidate = base + "_Q" + (q.getSortOrder() != null ? q.getSortOrder() : q.getId());
        if (!usedVarNames.contains(candidate)) {
            usedVarNames.add(candidate);
            return candidate;
        }
        candidate = base + "_Q" + q.getId();
        usedVarNames.add(candidate);
        return candidate;
    }

    // ====== 解析工具 ======

    private record OptionItem(String label, String value, Boolean hasInput, Boolean inputHidden) {
    }

    private record SelectedInputs(Object selected, Map<String, String> inputs) {
        String selectedAsString() {
            if (selected == null) return "";
            return String.valueOf(selected);
        }

        Set<String> selectedAsSet() {
            if (selected instanceof Collection<?> c) {
                Set<String> set = new LinkedHashSet<>();
                for (Object o : c) set.add(String.valueOf(o));
                return set;
            }
            if (selected instanceof String s && !s.isBlank()) {
                return Set.of(s);
            }
            return Set.of();
        }
    }

    private record MCCol(String key, String type, List<OptionItem> options) {
    }

    private record MatrixComplexOpts(List<OptionItem> rows, List<MCCol> columns) {
    }

    private record DynamicCol(String key) {
    }

    private SelectedInputs parseSelectedInputs(String raw) {
        try {
            JsonNode node = objectMapper.readTree(raw);
            if (node.isObject() && node.has("selected")) {
                Object selected;
                JsonNode sel = node.get("selected");
                if (sel.isArray()) {
                    selected = objectMapper.convertValue(sel, new TypeReference<List<String>>() {});
                } else {
                    selected = sel.asText("");
                }
                Map<String, String> inputs = node.has("inputs")
                        ? objectMapper.convertValue(node.get("inputs"), new TypeReference<>() {})
                        : Map.of();
                return new SelectedInputs(selected, inputs != null ? inputs : Map.of());
            }
            if (node.isArray()) {
                return new SelectedInputs(objectMapper.convertValue(node, new TypeReference<List<String>>() {}), Map.of());
            }
            if (node.isTextual()) {
                return new SelectedInputs(node.asText(), Map.of());
            }
        } catch (Exception ignored) {
            // fall through
        }
        return new SelectedInputs(raw, Map.of());
    }

    private List<OptionItem> parseChoiceOptions(Question q) {
        return parseOptionArray(q.getOptions());
    }

    private List<OptionItem> parseFieldOptions(Question q) {
        return parseOptionArray(q.getOptions());
    }

    private List<OptionItem> visibleFieldOptions(Question q) {
        return parseFieldOptions(q).stream()
                .filter(f -> !Boolean.TRUE.equals(f.inputHidden))
                .toList();
    }

    private OptionItem findOptionByValue(String value, List<OptionItem> options) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (OptionItem opt : options) {
            if (value.equals(opt.value)) {
                return opt;
            }
        }
        return null;
    }

    private List<OptionItem> parseMatrixRows(Question q) {
        try {
            JsonNode node = objectMapper.readTree(q.getOptions());
            if (node.has("rows")) {
                return objectMapper.convertValue(node.get("rows"), new TypeReference<>() {});
            }
        } catch (Exception ignored) {
        }
        return List.of();
    }

    private List<OptionItem> parseMatrixCols(Question q) {
        try {
            JsonNode node = objectMapper.readTree(q.getOptions());
            if (node.has("cols")) {
                return objectMapper.convertValue(node.get("cols"), new TypeReference<>() {});
            }
        } catch (Exception ignored) {
        }
        return List.of();
    }

    private MatrixComplexOpts parseMatrixComplexOpts(Question q) {
        try {
            JsonNode node = objectMapper.readTree(q.getOptions());
            List<OptionItem> rows = node.has("rows")
                    ? objectMapper.convertValue(node.get("rows"), new TypeReference<>() {})
                    : List.of();
            List<MCCol> cols = new ArrayList<>();
            if (node.has("columns")) {
                for (JsonNode c : node.get("columns")) {
                    List<OptionItem> colOpts = c.has("options")
                            ? objectMapper.convertValue(c.get("options"), new TypeReference<>() {})
                            : List.of();
                    cols.add(new MCCol(c.path("key").asText("c"), c.path("type").asText("input"), colOpts));
                }
            }
            return new MatrixComplexOpts(rows, cols);
        } catch (Exception e) {
            return new MatrixComplexOpts(List.of(), List.of());
        }
    }

    private List<DynamicCol> parseDynamicTableColumns(Question q) {
        List<OptionItem> opts = parseOptionArray(q.getOptions());
        if (opts.isEmpty()) {
            return List.of(new DynamicCol("col1"));
        }
        return opts.stream().map(o -> new DynamicCol(o.value)).toList();
    }

    private List<OptionItem> parseOptionArray(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(optionsJson, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private Map<String, String> parseStringMap(String raw) {
        try {
            return objectMapper.readValue(raw, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private Map<String, Object> parseObjectMap(String raw) {
        try {
            return objectMapper.readValue(raw, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private Map<String, Map<String, Object>> parseNestedMap(String raw) {
        try {
            return objectMapper.readValue(raw, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private List<String> parseStringList(String raw) {
        try {
            return objectMapper.readValue(raw, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<Map<String, String>> parseTableRows(String raw) {
        try {
            return objectMapper.readValue(raw, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private Set<String> toStringSet(Object val) {
        if (val == null) return Set.of();
        if (val instanceof Collection<?> c) {
            Set<String> set = new LinkedHashSet<>();
            for (Object o : c) set.add(String.valueOf(o));
            return set;
        }
        return Set.of(String.valueOf(val));
    }

    private Object formatMatrixComplexCell(Object cell, MCCol col, ExportValueFormat format) {
        if (cell == null) return "";
        if ("radio".equals(col.type)) {
            if (format.isLabel()) {
                return resolveOptionLabel(String.valueOf(cell), col.options != null ? col.options : List.of());
            }
            return toColIndex(String.valueOf(cell), col.options != null ? col.options : List.of());
        }
        if ("checkbox".equals(col.type) && cell instanceof Collection<?> c) {
            if (c.isEmpty()) return "";
            List<OptionItem> options = col.options != null ? col.options : List.of();
            if (format.isLabel()) {
                if (options.isEmpty()) {
                    return String.join("；", c.stream().map(String::valueOf).toList());
                }
                List<String> picked = c.stream().map(String::valueOf).toList();
                List<String> labels = new ArrayList<>();
                for (OptionItem opt : options) {
                    if (picked.contains(opt.value)) {
                        labels.add(optionLabel(opt));
                    }
                }
                return String.join("；", labels);
            }
            if (options.isEmpty()) {
                return String.join(",", c.stream().map(String::valueOf).toList());
            }
            List<String> picked = c.stream().map(String::valueOf).toList();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < options.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(picked.contains(options.get(i).value) ? 1 : 0);
            }
            return sb.toString();
        }
        if ("freq".equals(col.type) && cell instanceof Map<?, ?> m) {
            Object value = m.get("value");
            Object unit = m.get("unit");
            if (value == null || String.valueOf(value).isBlank()) return "";
            String num = String.valueOf(parseScalar(String.valueOf(value), null));
            if (format.isLabel() && unit != null && !String.valueOf(unit).isBlank()) {
                return num + unit;
            }
            return num;
        }
        if ("freq".equals(col.type)) {
            return "";
        }
        return parseScalar(String.valueOf(cell), null);
    }

    private String optionLabel(OptionItem opt) {
        if (opt == null) return "";
        if (opt.label != null && !opt.label.isBlank()) {
            return opt.label.trim();
        }
        return opt.value != null ? opt.value : "";
    }

    private String resolveOptionLabel(String value, List<OptionItem> options) {
        if (value == null || value.isBlank()) return "";
        for (OptionItem opt : options) {
            if (value.equals(opt.value)) {
                return optionLabel(opt);
            }
        }
        return value.trim();
    }

    private Object parsePlainAnswer(String raw, Question q) {
        if (raw == null || raw.isBlank()) return "";
        String trimmed = raw.trim();
        if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            return flattenJsonAnswer(trimmed);
        }
        return parseScalar(raw, q);
    }

    private Object flattenUploadValue(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String trimmed = raw.trim();
        if (trimmed.startsWith("[")) {
            List<String> urls = parseStringList(raw);
            if (!urls.isEmpty()) {
                return String.join(";", urls);
            }
        }
        return parseScalar(raw, null);
    }

    private Object flattenJsonAnswer(String raw) {
        try {
            JsonNode node = objectMapper.readTree(raw);
            if (node.isArray()) {
                List<String> parts = new ArrayList<>();
                for (JsonNode item : node) {
                    if (item.isTextual()) {
                        parts.add(item.asText());
                    } else if (!item.isNull()) {
                        parts.add(item.asText());
                    }
                }
                if (!parts.isEmpty()) {
                    return String.join(";", parts);
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private boolean isDateRange(Question q) {
        try {
            JsonNode node = objectMapper.readTree(q.getValidationRules() != null ? q.getValidationRules() : "{}");
            return "daterange".equals(node.path("dateType").asText("date"));
        } catch (Exception e) {
            return false;
        }
    }

    private Object parseScalar(String val, Question q) {
        if (val == null || val.isBlank() || "NaN".equalsIgnoreCase(val) || "null".equalsIgnoreCase(val)) {
            return "";
        }
        String trimmed = val.trim();
        // 长整数字符串（如 15 位个人编码）必须保持原样，避免 double 精度丢失
        if (trimmed.matches("-?\\d+")) {
            if (trimmed.length() > 15) {
                return trimmed;
            }
            try {
                long l = Long.parseLong(trimmed);
                if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
                    return (int) l;
                }
                return trimmed;
            } catch (NumberFormatException e) {
                return trimmed;
            }
        }
        try {
            double d = Double.parseDouble(trimmed);
            if (Double.isNaN(d) || Double.isInfinite(d)) return "";
            if (d == Math.rint(d) && Math.abs(d) <= Integer.MAX_VALUE) {
                return (int) d;
            }
            return d;
        } catch (NumberFormatException e) {
            return trimmed;
        }
    }

    private Object toOptionInt(String optValue, List<OptionItem> options) {
        if (optValue == null || optValue.isBlank()) return "";
        for (int i = 0; i < options.size(); i++) {
            if (optValue.equals(options.get(i).value)) {
                return i + 1;
            }
        }
        try {
            double d = Double.parseDouble(optValue.trim());
            if (Double.isNaN(d) || Double.isInfinite(d)) return "";
            if (d == Math.rint(d) && Math.abs(d) <= Integer.MAX_VALUE) {
                return (int) d;
            }
        } catch (NumberFormatException ignored) {
        }
        return optValue;
    }

    private Object toColIndex(String colValue, List<OptionItem> cols) {
        if (colValue == null || colValue.isBlank()) return "";
        for (int i = 0; i < cols.size(); i++) {
            if (colValue.equals(cols.get(i).value)) {
                return i + 1;
            }
        }
        try {
            double d = Double.parseDouble(colValue.trim());
            if (Double.isNaN(d) || Double.isInfinite(d)) return "";
            if (d == Math.rint(d) && d >= 1 && d <= cols.size()) {
                return (int) d;
            }
        } catch (NumberFormatException ignored) {
        }
        return parseScalar(colValue, null);
    }

    /** matrix_radio / matrix_checkbox / matrix_scale / matrix_input：填写端使用 r.value（可为空串） */
    private String matrixSimpleRowKey(List<OptionItem> rows, int ri) {
        if (ri < 0 || ri >= rows.size()) return "";
        String rowVal = rows.get(ri).value;
        return rowVal != null ? rowVal : "";
    }

    /** matrix_complex：填写端使用 r.value || String(ri)（0-based 索引字符串） */
    private String matrixComplexRowKey(List<OptionItem> rows, int ri) {
        if (ri < 0 || ri >= rows.size()) return String.valueOf(ri);
        String rowVal = rows.get(ri).value;
        return (rowVal == null || rowVal.isBlank()) ? String.valueOf(ri) : rowVal;
    }

    private Map<String, Object> getMatrixComplexRowData(
            Map<String, Map<String, Object>> all, List<OptionItem> rows, int ri) {
        String primary = matrixComplexRowKey(rows, ri);
        Map<String, Object> rowData = all.get(primary);
        if (rowData != null) return rowData;
        String rowVal = rows.get(ri).value;
        if (rowVal != null && !rowVal.isBlank() && !primary.equals(String.valueOf(ri))) {
            rowData = all.get(String.valueOf(ri));
        }
        return rowData != null ? rowData : Map.of();
    }

    /** 写入 Excel 前统一转字符串，避免 155.0、科学计数法等问题 */
    public String formatExcelCell(Object val) {
        Object cleaned = cleanCell(val);
        if (cleaned == null || "".equals(cleaned)) {
            return "";
        }
        if (cleaned instanceof Number n) {
            double d = n.doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                return "";
            }
            if (d == Math.rint(d)) {
                return String.valueOf((long) d);
            }
            return String.valueOf(cleaned);
        }
        return String.valueOf(cleaned).trim();
    }

    public Object cleanCell(Object val) {
        if (val == null) return "";
        if (val instanceof Double d) {
            if (Double.isNaN(d) || Double.isInfinite(d)) return "";
            if (d == Math.rint(d) && Math.abs(d) <= Integer.MAX_VALUE) return d.intValue();
            return d;
        }
        if (val instanceof Float f) {
            if (Float.isNaN(f) || Float.isInfinite(f)) return "";
            if (f == Math.rint(f)) return f.intValue();
            return f;
        }
        if (val instanceof Number n) {
            double d = n.doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) return "";
            if (d == Math.rint(d) && Math.abs(d) <= Integer.MAX_VALUE) return n.intValue();
            return n;
        }
        String s = String.valueOf(val).trim();
        if (s.isEmpty() || "NaN".equalsIgnoreCase(s) || "null".equalsIgnoreCase(s)) return "";
        if (s.startsWith("{") || s.startsWith("[")) return "";
        return s;
    }

    private boolean hasOptionInput(Question q) {
        return parseChoiceOptions(q).stream().anyMatch(o -> Boolean.TRUE.equals(o.hasInput));
    }

    private boolean isMultiImageChoice(Question q) {
        try {
            JsonNode node = objectMapper.readTree(q.getValidationRules() != null ? q.getValidationRules() : "{}");
            return node.path("multiple").asBoolean(false);
        } catch (Exception e) {
            return false;
        }
    }

    private String extractVarName(Question q) {
        if (q.getTitle() != null) {
            Matcher m = TITLE_CODE.matcher(q.getTitle().trim());
            if (m.find()) {
                return m.group(1);
            }
        }
        return "Q" + (q.getSortOrder() != null ? q.getSortOrder() : 0);
    }

    private String questionLabel(Question q) {
        if (q.getTitle() == null || q.getTitle().isBlank()) {
            return "第" + q.getSortOrder() + "题";
        }
        return q.getTitle().trim();
    }

    /** 文字描述题：第3行展示说明正文（无正文则留空，仍占 Q 编号列） */
    private String descriptionSubLabel(Question q) {
        if (q.getDescription() != null && !q.getDescription().isBlank()) {
            return q.getDescription().trim();
        }
        return "";
    }

    /** 题干行：带上 Q 编号，便于对照变量名 */
    private String formatGroupLabel(String base, Question q) {
        return base + "  " + questionLabel(q);
    }

    private String sanitizeSuffix(String s) {
        if (s == null || s.isBlank()) return "x";
        return s.replaceAll("[^A-Za-z0-9_]", "_");
    }

    private String formatStatus(Integer status) {
        if (status == null) return "";
        return switch (status) {
            case 0 -> "进行中";
            case 1 -> "有效提交";
            case 2 -> "不良样本";
            default -> String.valueOf(status);
        };
    }
}
