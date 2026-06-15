package cn.luyou.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.model.DataCleaningResult;
import cn.luyou.service.DataCleaningService;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import lombok.Data;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DataCleaningServiceImpl implements DataCleaningService {
    private static final String TYPE_SCHOOL = "school";
    private static final String TYPE_KEY = "keyPopulation";
    private static final String TYPE_CLOSE = "closeContact";
    private static final int MAX_ERROR_PREVIEW = 200;
    private static final long FILE_EXPIRE_MS = 30 * 60 * 1000L;
    private static final DataFormatter DATA_FORMATTER = new DataFormatter();
    private static final List<String> SCHOOL_HEADER_TOP = List.of(
            "序号", "年份", "市（州）", "县（市、区）", "姓名", "性别", "出生日期", "年龄", "证件类型", "证件号",
            "民族", "联系电话", "户籍所在地（XX市XX县、区）", "现地址", "学校类型", "学校名称", "班级（院系）",
            "既往结核病史", "密切接触史", "结核病可疑症状", "学校人群感染筛查情况", "学校人群感染筛查情况",
            "学校人群感染筛查情况", "学校人群感染筛查情况", "学校人群感染筛查情况", "学校人群胸片检查",
            "学校人群胸片检查", "学校人群胸片检查", "痰涂片结果", "分子生物学结果", "诊断结果",
            "潜伏感染者管理情况", "潜伏感染者管理情况", "潜伏感染者管理情况", "潜伏感染者管理情况",
            "潜伏感染者管理情况", "潜伏感染者管理情况"
    );
    private static final List<String> SCHOOL_HEADER_SUB = List.of(
            "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
            "是否进行感染筛", "感染筛查日期", "方法", "结果（PPD：mmXmm；EC及IGRA：阳性/阴性）", "感染筛查结果",
            "是否进行胸片检查", "胸片检查日期", "胸片结果", "", "", "", "是否进行预防者治疗", "预防性治疗方案",
            "预防性治疗开始时间（年月日）", "预防性治疗完成时间（年月日）", "预防性治疗结果", "预防性治疗期间随访管理人员"
    );
    private static final List<String> SCHOOL_FIELD_KEYS = List.of(
            "seq", "year", "city", "district", "name", "gender", "birthDate", "age", "idType", "idNumber",
            "ethnicity", "phone", "householdAddress", "currentAddress", "schoolType", "schoolName", "className",
            "tbHistory", "closeContactHistory", "suspiciousSymptoms", "hasInfectionScreen", "screenDate",
            "screenMethod", "screenResult", "infectionResult", "hasChestXray", "chestXrayDate", "chestXrayResult",
            "sputumSmearResult", "molecularBiologyResult", "diagnosisResult", "hasPreventiveTreatment",
            "preventivePlan", "preventiveStartDate", "preventiveEndDate", "preventiveResult", "preventiveManager"
    );

    private final Map<String, CleaningFileMeta> resultFileStore = new ConcurrentHashMap<>();

    @Override
    public DataCleaningResult clean(String populationType, MultipartFile file) {
        String type = normalizeType(populationType);
        validateExcel(file);

        int headRowNumber = resolveHeadRows(type);
        ValidationResult validationResult = readAndValidate(type, file, headRowNumber);
        List<RowValidationError> allErrors = validationResult.getErrors();
        String fileId = IdUtil.fastSimpleUUID();
        File resultFile = markAndWriteResult(type, file, fileId, allErrors, headRowNumber);
        Long currentUserId = currentUserId();
        resultFileStore.put(fileId, new CleaningFileMeta(resultFile, currentUserId, System.currentTimeMillis()));

        List<String> previewErrors = allErrors.stream()
                .limit(MAX_ERROR_PREVIEW)
                .map(RowValidationError::getMessage)
                .toList();
        return DataCleaningResult.builder()
                .totalCount(validationResult.getTotalCount())
                .abnormalCount((int) allErrors.stream().map(RowValidationError::getRowIndex).distinct().count())
                .fileId(fileId)
                .fileName(resultFile.getName())
                .errors(previewErrors)
                .build();
    }

    @Override
    public DataCleaningResult matchSchool(MultipartFile file) {
        validateExcel(file);
        String fileId = IdUtil.fastSimpleUUID();
        File resultFile = buildSchoolMatchedFile(file, fileId);
        Long currentUserId = currentUserId();
        resultFileStore.put(fileId, new CleaningFileMeta(resultFile, currentUserId, System.currentTimeMillis()));
        int totalCount = countDataRows(resultFile);
        return DataCleaningResult.builder()
                .totalCount(totalCount)
                .abnormalCount(0)
                .fileId(fileId)
                .fileName(resultFile.getName())
                .errors(List.of())
                .build();
    }

    @Override
    public Resource getResultFile(String fileId, Long currentUserId, boolean isSuperAdmin) {
        CleaningFileMeta meta = resultFileStore.get(fileId);
        if (meta == null || meta.getFile() == null || !meta.getFile().exists()) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "清洗结果文件不存在或已过期");
        }
        if (isExpired(meta.getCreateAtMs())) {
            deleteMetaFile(fileId, meta);
            throw new ServiceException(StatusEnum.PARAM_INVALID, "清洗结果文件已过期，请重新清洗");
        }
        if (!Boolean.TRUE.equals(isSuperAdmin) && !sameUser(meta.getCreatorUserId(), currentUserId)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "无权限下载该清洗结果文件");
        }
        return new FileSystemResource(meta.getFile());
    }

    private String normalizeType(String populationType) {
        String type = StrUtil.blankToDefault(populationType, "").trim();
        if (!Arrays.asList(TYPE_SCHOOL, TYPE_KEY, TYPE_CLOSE).contains(type)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "populationType 仅支持 school/keyPopulation/closeContact");
        }
        return type;
    }

    private void validateExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "请上传 Excel 文件");
        }
        String name = StrUtil.blankToDefault(file.getOriginalFilename(), "").toLowerCase(Locale.ROOT);
        if (!name.endsWith(".xlsx") && !name.endsWith(".xls")) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "仅支持 .xlsx 或 .xls 文件");
        }
    }

    private int resolveHeadRows(String type) {
        if (TYPE_KEY.equals(type)) return 4;
        return 2;
    }

    private ValidationResult readAndValidate(String type, MultipartFile file, int headRowNumber) {
        List<RowValidationError> errors = new ArrayList<>();
        AtomicInteger totalCount = new AtomicInteger(0);
        AtomicInteger rowOffset = new AtomicInteger(0);
        try (InputStream inputStream = file.getInputStream()) {
            EasyExcel.read(inputStream, List.class, new PageReadListener<List<Object>>(rows -> {
                        for (List<Object> row : rows) {
                            int excelRowIndex = headRowNumber + rowOffset.getAndIncrement() + 1;
                            if (shouldSkipValidationRow(type, row)) {
                                continue;
                            }
                            totalCount.incrementAndGet();
                            errors.addAll(validateRow(type, row, excelRowIndex));
                        }
                    }))
                    .sheet()
                    .headRowNumber(headRowNumber)
                    .doRead();
        } catch (Exception e) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "解析 Excel 失败: " + e.getMessage());
        }
        return new ValidationResult(totalCount.get(), errors);
    }

    private List<RowValidationError> validateRow(String type, List<Object> row, int excelRowIndex) {
        List<RowValidationError> result = new ArrayList<>();
        String name = getCellString(row, TYPE_KEY.equals(type) ? 4 : (TYPE_CLOSE.equals(type) ? 11 : 4));

        String idCard;
        String phone;
        String infectionResult;
        if (TYPE_SCHOOL.equals(type)) {
            idCard = getCellString(row, 9);
            phone = getCellString(row, 11);
            infectionResult = getCellString(row, 24);
            appendCommonValidation(result, excelRowIndex, name, idCard, 9, phone, 11, infectionResult, 24);
            String gender = getCellString(row, 5);
            if (StrUtil.isNotBlank(gender) && !isInOptions(gender, "男", "女")) {
                result.add(err(excelRowIndex, name, 5, "性别仅支持：男/女"));
            }
            String idType = getCellString(row, 8);
            if (StrUtil.isNotBlank(idType) && !isInOptions(idType, "身份证", "居民身份证", "其它", "其他")) {
                result.add(err(excelRowIndex, name, 8, "证件类型仅支持：身份证/其它"));
            }
            validateOption(result, excelRowIndex, name, row, 17, "既往结核病史仅支持：有/无", "有", "无");
            validateOption(result, excelRowIndex, name, row, 18, "密切接触史仅支持：有/无", "有", "无");
            validateOption(result, excelRowIndex, name, row, 20, "是否进行感染筛仅支持：是/否", "是", "否");
            validateOption(result, excelRowIndex, name, row, 22, "感染筛查方法仅支持：PPD/EC/IGRA", "PPD", "EC", "IGRA");
            validateOption(result, excelRowIndex, name, row, 25, "是否进行胸片检查仅支持：是/否", "是", "否");
            validateOption(result, excelRowIndex, name, row, 27, "胸片结果仅支持：正常/异常/未查", "正常", "异常", "未查");
            validateOption(result, excelRowIndex, name, row, 30, "诊断结果仅支持：排除/疑似肺结核/潜伏感染者/确诊患者/其他",
                    "排除", "疑似肺结核", "潜伏感染者", "确诊患者", "其他");
        } else if (TYPE_KEY.equals(type)) {
            idCard = getCellString(row, 9);
            phone = getCellString(row, 11);
            infectionResult = getCellString(row, 36);
            appendCommonValidation(result, excelRowIndex, name, idCard, 9, phone, 11, infectionResult, 36);
            for (int idx = 15; idx <= 22; idx++) {
                String value = getCellString(row, idx);
                if (StrUtil.isNotBlank(value) && !isInOptions(value, "是", "否")) {
                    result.add(err(excelRowIndex, name, idx, "人群分类列仅支持：是/否"));
                }
            }
        } else {
            idCard = getCellString(row, 12);
            phone = getCellString(row, 15);
            appendCommonValidation(result, excelRowIndex, name, idCard, 12, phone, 15, null, -1);
            String finalResult = getCellString(row, 30);
            if (StrUtil.isBlank(finalResult) || !isInOptions(finalResult, "活动性肺结核", "潜伏感染者", "未做", "未发现异常")) {
                result.add(err(excelRowIndex, name, 30, "最终筛查结果仅支持：活动性肺结核/潜伏感染者/未做/未发现异常"));
            }
        }
        return result;
    }

    private boolean shouldSkipValidationRow(String type, List<Object> row) {
        int nameIndex = TYPE_KEY.equals(type) ? 4 : (TYPE_CLOSE.equals(type) ? 11 : 4);
        int idIndex = TYPE_CLOSE.equals(type) ? 12 : 9;
        return StrUtil.isBlank(getCellString(row, nameIndex)) && StrUtil.isBlank(getCellString(row, idIndex));
    }

    private void appendCommonValidation(
            List<RowValidationError> result,
            int excelRowIndex,
            String name,
            String idCard,
            int idCardCol,
            String phone,
            int phoneCol,
            String infectionResult,
            int infectionCol
    ) {
        if (StrUtil.isBlank(idCard)) {
            result.add(err(excelRowIndex, name, idCardCol, "身份证号不能为空"));
        } else if (!isValidIdCard(idCard)) {
            result.add(err(excelRowIndex, name, idCardCol, "身份证号格式不正确"));
        }

        if (StrUtil.isBlank(phone)) {
            result.add(err(excelRowIndex, name, phoneCol, "手机号不能为空"));
        } else if (!isValidPhone(phone)) {
            result.add(err(excelRowIndex, name, phoneCol, "手机号格式不正确"));
        }

        if (infectionCol >= 0 && StrUtil.isNotBlank(infectionResult) && !isInOptions(infectionResult,
                "PPD阴性", "PPD+", "PPD++", "PPD+++", "EC阴性", "EC阳性", "IGRA阴性", "IGRA阳性")) {
            result.add(err(excelRowIndex, name, infectionCol, "感染筛查结果不在系统支持范围"));
        }
    }

    private RowValidationError err(int rowIndex, String name, int colIndex, String reason) {
        String person = StrUtil.isBlank(name) ? "未知姓名" : name;
        String msg = String.format("第%d行[%s] 第%d列：%s", rowIndex, person, colIndex + 1, reason);
        return new RowValidationError(rowIndex, colIndex, reason, msg);
    }

    private boolean isInOptions(String value, String... options) {
        if (value == null) return false;
        for (String option : options) {
            if (option.equals(value.trim())) return true;
        }
        return false;
    }

    private boolean isValidIdCard(String id) {
        return id != null && id.length() == 18 && id.matches("\\d{17}[\\dXx]");
    }

    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^1[3-9]\\d{9}$");
    }

    private String getCellString(List<Object> row, int index) {
        if (index < 0 || row == null || index >= row.size()) return "";
        Object value = row.get(index);
        if (value == null) return "";
        return String.valueOf(value).trim();
    }

    private void validateOption(List<RowValidationError> result, int excelRowIndex, String name,
                                List<Object> row, int colIndex, String reason, String... options) {
        String value = getCellString(row, colIndex);
        if (StrUtil.isNotBlank(value) && !isInOptions(value, options)) {
            result.add(err(excelRowIndex, name, colIndex, reason));
        }
    }

    private File buildSchoolMatchedFile(MultipartFile sourceFile, String fileId) {
        try {
            Path dir = Files.createDirectories(Path.of(System.getProperty("java.io.tmpdir"), "disease-cleaning"));
            Path path = dir.resolve("学生筛查数据匹配结果_" + fileId + ".xlsx");
            try (InputStream inputStream = sourceFile.getInputStream();
                 Workbook source = WorkbookFactory.create(inputStream);
                 Workbook target = new XSSFWorkbook();
                 FileOutputStream outputStream = new FileOutputStream(path.toFile())) {
                Sheet sourceSheet = source.getSheetAt(0);
                HeaderLocation headerLocation = locateSchoolHeader(sourceSheet);
                Map<String, Integer> headerIndex = buildSchoolMatchHeaderIndex(sourceSheet, headerLocation);

                Sheet targetSheet = target.createSheet("学生筛查");
                Row top = targetSheet.createRow(0);
                Row sub = targetSheet.createRow(1);
                for (int i = 0; i < SCHOOL_HEADER_TOP.size(); i++) {
                    top.createCell(i).setCellValue(SCHOOL_HEADER_TOP.get(i));
                    sub.createCell(i).setCellValue(SCHOOL_HEADER_SUB.get(i));
                }

                int targetRowNum = 2;
                for (int i = headerLocation.dataStartRow(); i <= sourceSheet.getLastRowNum(); i++) {
                    Row sourceRow = sourceSheet.getRow(i);
                    if (sourceRow == null || isBlankSourceRow(sourceRow, headerIndex)) continue;
                    Row targetRow = targetSheet.createRow(targetRowNum++);
                    for (int col = 0; col < SCHOOL_FIELD_KEYS.size(); col++) {
                        String key = SCHOOL_FIELD_KEYS.get(col);
                        String value = matchedValue(sourceRow, headerIndex, key, targetRowNum - 2);
                        writeMatchedCell(targetRow, col, key, value);
                    }
                }
                for (int i = 0; i < SCHOOL_HEADER_TOP.size(); i++) {
                    targetSheet.autoSizeColumn(i);
                }
                target.write(outputStream);
            }
            return path.toFile();
        } catch (IOException e) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "生成匹配结果失败: " + e.getMessage());
        }
    }

    private HeaderLocation locateSchoolHeader(Sheet sheet) {
        for (int i = 0; i <= Math.min(sheet.getLastRowNum(), 8); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            String joined = rowToText(row);
            if (joined.contains("姓名") && (joined.contains("证件号") || joined.contains("身份证号"))) {
                boolean hasNextSubHeader = i + 1 <= sheet.getLastRowNum()
                        && rowToText(sheet.getRow(i + 1)).contains("方法");
                return new HeaderLocation(i, hasNextSubHeader ? i + 2 : i + 1);
            }
        }
        throw new ServiceException(StatusEnum.PARAM_INVALID, "未识别到学生筛查表头，请检查上传文件");
    }

    private Map<String, Integer> buildSchoolMatchHeaderIndex(Sheet sheet, HeaderLocation location) {
        Map<String, Integer> index = new HashMap<>();
        Row main = sheet.getRow(location.headerRow());
        Row sub = location.dataStartRow() - location.headerRow() > 1 ? sheet.getRow(location.headerRow() + 1) : null;
        int last = Math.max(main == null ? 0 : main.getLastCellNum(), sub == null ? 0 : sub.getLastCellNum());
        for (int col = 0; col < last; col++) {
            String mainText = getPoiCellString(main, col);
            String subText = getPoiCellString(sub, col);
            putMatchAlias(index, mainText, col);
            putMatchAlias(index, subText, col);
            putMatchAlias(index, mainText + subText, col);
        }
        return index;
    }

    private void putMatchAlias(Map<String, Integer> index, String rawHeader, int col) {
        if (StrUtil.isBlank(rawHeader)) return;
        String header = normalizeHeader(rawHeader);
        if (StrUtil.isBlank(header)) return;
        String key = switch (header) {
            case "序号" -> "seq";
            case "年份", "年度" -> "year";
            case "市州" -> "city";
            case "县市区", "区县" -> "district";
            case "姓名" -> "name";
            case "性别" -> "gender";
            case "出生日期" -> "birthDate";
            case "年龄", "年龄根据身份证号自动生成" -> "age";
            case "证件类型" -> "idType";
            case "证件号", "身份证号", "身份证号码" -> "idNumber";
            case "民族" -> "ethnicity";
            case "联系电话" -> "phone";
            case "户籍所在地XX市XX县区", "户籍所在地" -> "householdAddress";
            case "现地址", "现住址" -> "currentAddress";
            case "学校类型" -> "schoolType";
            case "学校名称" -> "schoolName";
            case "班级院系" -> "className";
            case "既往结核病史" -> "tbHistory";
            case "密切接触史" -> "closeContactHistory";
            case "结核病可疑症状" -> "suspiciousSymptoms";
            case "是否进行感染筛" -> "hasInfectionScreen";
            case "感染筛查日期" -> "screenDate";
            case "方法", "感染筛查方法" -> "screenMethod";
            case "结果PPDmmXmmEC及IGRA阳性阴性" -> "screenResult";
            case "感染筛查结果", "判定结果", "感染筛查判定结果" -> "infectionResult";
            case "是否进行胸片检查" -> "hasChestXray";
            case "胸片检查日期" -> "chestXrayDate";
            case "胸片结果", "胸部DR" -> "chestXrayResult";
            case "痰涂片", "痰涂片结果" -> "sputumSmearResult";
            case "分子生物学", "分子生物学结果" -> "molecularBiologyResult";
            case "诊断", "诊断结果" -> "diagnosisResult";
            case "符合潜伏治疗条件者是否进行预防性治疗是写出方案否填写原因", "是否进行预防者治疗", "是否进行预防性治疗" -> "hasPreventiveTreatment";
            case "预防性治疗方案" -> "preventivePlan";
            case "预防性治疗开始时间年月日" -> "preventiveStartDate";
            case "预防性治疗完成时间年月日" -> "preventiveEndDate";
            case "预防性治疗结果" -> "preventiveResult";
            case "预防性治疗期间随访管理人员" -> "preventiveManager";
            case "备注" -> "remark";
            default -> "";
        };
        if (StrUtil.isNotBlank(key)) {
            index.putIfAbsent(key, col);
        }
    }

    private String matchedValue(Row sourceRow, Map<String, Integer> headerIndex, String key, int seq) {
        if ("seq".equals(key)) return String.valueOf(seq);
        Integer col = headerIndex.get(key);
        String value = col == null ? "" : getPoiCellString(sourceRow, col, key);
        if ("age".equals(key)) {
            if (isFormulaLike(value)) {
                value = "";
            }
            if (StrUtil.isBlank(value)) {
                value = calculateAgeFromIdNumber(matchedValue(sourceRow, headerIndex, "idNumber", seq));
            }
            if (StrUtil.isNotBlank(value)) {
                String digits = value.replaceAll("[^0-9]", "");
                return StrUtil.isBlank(digits) ? "" : digits;
            }
        }
        if ("birthDate".equals(key) && StrUtil.isBlank(value)) {
            value = birthDateFromIdNumber(matchedValue(sourceRow, headerIndex, "idNumber", seq));
        }
        return value;
    }

    private void writeMatchedCell(Row row, int col, String key, String value) {
        Cell cell = row.createCell(col);
        if (StrUtil.isBlank(value)) {
            cell.setBlank();
            return;
        }
        if (("age".equals(key) || "seq".equals(key) || "year".equals(key)) && value.matches("\\d+")) {
            cell.setCellValue(Long.parseLong(value));
            return;
        }
        cell.setCellValue(value);
    }

    private boolean isFormulaLike(String value) {
        if (StrUtil.isBlank(value)) return false;
        String text = value.trim();
        return text.startsWith("=")
                || text.contains("DATEDIF(")
                || text.contains("MID(")
                || text.contains("TODAY(")
                || text.contains("TEXT(");
    }

    private String calculateAgeFromIdNumber(String idNumber) {
        if (StrUtil.isBlank(idNumber) || idNumber.length() != 18) return "";
        try {
            LocalDate birthDate = LocalDate.parse(
                    idNumber.substring(6, 14),
                    DateTimeFormatter.ofPattern("yyyyMMdd")
            );
            int age = Period.between(birthDate, LocalDate.now()).getYears();
            return age >= 0 ? String.valueOf(age) : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private String birthDateFromIdNumber(String idNumber) {
        if (StrUtil.isBlank(idNumber) || idNumber.length() != 18) return "";
        try {
            LocalDate birthDate = LocalDate.parse(
                    idNumber.substring(6, 14),
                    DateTimeFormatter.ofPattern("yyyyMMdd")
            );
            return birthDate.toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private String normalizeExcelCellText(String val) {
        if (StrUtil.isBlank(val)) return "";
        String text = val.trim();
        if (text.matches(".*[eE].*") || text.matches("\\d+\\.0+")) {
            try {
                return new java.math.BigDecimal(text).toPlainString();
            } catch (Exception ignored) {
                return text;
            }
        }
        return text;
    }

    private boolean isBlankSourceRow(Row row, Map<String, Integer> headerIndex) {
        return StrUtil.isBlank(matchedValue(row, headerIndex, "name", 0))
                && StrUtil.isBlank(matchedValue(row, headerIndex, "idNumber", 0));
    }

    private String getPoiCellString(Row row, int col) {
        return getPoiCellString(row, col, null);
    }

    private String getPoiCellString(Row row, int col, String fieldKey) {
        if (row == null || col < 0) return "";
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        // 证件号/手机号须保留 Excel 显示文本，避免数值型单元格精度丢失
        if ("idNumber".equals(fieldKey) || "phone".equals(fieldKey)) {
            return normalizeExcelCellText(DATA_FORMATTER.formatCellValue(cell).trim());
        }
        return formatCellValue(cell);
    }

    private String formatCellValue(Cell cell) {
        if (cell == null) return "";
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            try {
                type = cell.getCachedFormulaResultType();
            } catch (Exception ignored) {
                return "";
            }
        }
        return switch (type) {
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double num = cell.getNumericCellValue();
                if (num == Math.floor(num) && !Double.isInfinite(num)) {
                    yield String.valueOf((long) num);
                }
                yield String.valueOf(num);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case STRING -> cell.getStringCellValue().trim();
            default -> DATA_FORMATTER.formatCellValue(cell).trim();
        };
    }

    private String rowToText(Row row) {
        if (row == null) return "";
        List<String> values = new ArrayList<>();
        for (int i = 0; i < row.getLastCellNum(); i++) {
            values.add(getPoiCellString(row, i));
        }
        return String.join("|", values);
    }

    private String normalizeHeader(String value) {
        if (value == null) return "";
        return value.replaceAll("[\\s\\n\\r（）()：:；;、/]", "").trim();
    }

    private int countDataRows(File file) {
        try (Workbook workbook = WorkbookFactory.create(file)) {
            Sheet sheet = workbook.getSheetAt(0);
            int count = 0;
            for (int i = 2; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null && (StrUtil.isNotBlank(getPoiCellString(row, 4))
                        || StrUtil.isNotBlank(getPoiCellString(row, 9, "idNumber")))) {
                    count++;
                }
            }
            return count;
        } catch (IOException e) {
            return 0;
        }
    }

    private File markAndWriteResult(String type, MultipartFile sourceFile, String fileId, List<RowValidationError> errors, int headRowNumber) {
        try {
            Path dir = Files.createDirectories(Path.of(System.getProperty("java.io.tmpdir"), "disease-cleaning"));
            String fileName = "数据清洗结果_" + type + "_" + fileId + ".xlsx";
            Path path = dir.resolve(fileName);

            try (InputStream inputStream = sourceFile.getInputStream();
                 Workbook workbook = WorkbookFactory.create(inputStream);
                 FileOutputStream outputStream = new FileOutputStream(path.toFile())) {
                Sheet sheet = workbook.getSheetAt(0);
                CellStyle yellowStyle = workbook.createCellStyle();
                yellowStyle.cloneStyleFrom(workbook.createCellStyle());
                yellowStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
                yellowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                // 插入异常原因列
                int remarkCol = getLastColumnIndex(sheet) + 1;
                for (int rowNum = 0; rowNum <= sheet.getLastRowNum(); rowNum++) {
                    Row row = sheet.getRow(rowNum);
                    if (row == null) continue;
                    Cell cell = row.createCell(remarkCol);
                    if (rowNum == headRowNumber - 1) {
                        cell.setCellValue("异常原因");
                    }
                }

                Map<Integer, List<RowValidationError>> grouped = new java.util.HashMap<>();
                for (RowValidationError error : errors) {
                    grouped.computeIfAbsent(error.getRowIndex() - 1, k -> new ArrayList<>()).add(error);
                }

                for (Map.Entry<Integer, List<RowValidationError>> entry : grouped.entrySet()) {
                    int rowNum = entry.getKey();
                    Row row = sheet.getRow(rowNum);
                    if (row == null) continue;
                    Set<Integer> cols = new HashSet<>();
                    List<String> reasons = new ArrayList<>();
                    for (RowValidationError e : entry.getValue()) {
                        cols.add(e.getColIndex());
                        reasons.add(String.format("第%d列:%s", e.getColIndex() + 1, e.getReason()));
                    }
                    for (Integer col : cols) {
                        Cell cell = row.getCell(col);
                        if (cell == null) cell = row.createCell(col);
                        CellStyle mergedStyle = buildYellowStyle(workbook, cell, yellowStyle);
                        cell.setCellStyle(mergedStyle);
                    }
                    Cell reasonCell = row.getCell(remarkCol);
                    if (reasonCell == null) reasonCell = row.createCell(remarkCol);
                    CellStyle mergedStyle = buildYellowStyle(workbook, reasonCell, yellowStyle);
                    reasonCell.setCellStyle(mergedStyle);
                    reasonCell.setCellValue(String.join("；", reasons));
                }

                workbook.write(outputStream);
                return path.toFile();
            }
        } catch (IOException e) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "生成清洗结果失败: " + e.getMessage());
        }
    }

    private int getLastColumnIndex(Sheet sheet) {
        int max = 0;
        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null && row.getLastCellNum() > max) {
                max = row.getLastCellNum();
            }
        }
        return Math.max(0, max - 1);
    }

    private CellStyle buildYellowStyle(Workbook workbook, Cell sourceCell, CellStyle yellowBaseStyle) {
        CellStyle style = workbook.createCellStyle();
        if (sourceCell != null && sourceCell.getCellStyle() != null) {
            style.cloneStyleFrom(sourceCell.getCellStyle());
        } else {
            style.cloneStyleFrom(yellowBaseStyle);
        }
        style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private Long currentUserId() {
        Long userId = cn.luyou.utils.BaseContext.getCurrentId();
        return userId == null ? -1L : userId;
    }

    private boolean sameUser(Long ownerUserId, Long currentUserId) {
        if (ownerUserId == null || currentUserId == null) return false;
        return ownerUserId.equals(currentUserId);
    }

    private boolean isExpired(long createAtMs) {
        return System.currentTimeMillis() - createAtMs > FILE_EXPIRE_MS;
    }

    private void deleteMetaFile(String fileId, CleaningFileMeta meta) {
        try {
            if (meta != null && meta.getFile() != null && meta.getFile().exists()) {
                Files.deleteIfExists(meta.getFile().toPath());
            }
        } catch (IOException ignored) {
        } finally {
            resultFileStore.remove(fileId);
        }
    }

    /** 每10分钟清理一次过期清洗结果，避免磁盘与内存堆积 */
    @Scheduled(fixedDelay = 10 * 60 * 1000L, initialDelay = 5 * 60 * 1000L)
    public void cleanExpiredResultFiles() {
        for (Map.Entry<String, CleaningFileMeta> entry : resultFileStore.entrySet()) {
            if (entry.getValue() != null && isExpired(entry.getValue().getCreateAtMs())) {
                deleteMetaFile(entry.getKey(), entry.getValue());
            }
        }
    }

    private record HeaderLocation(int headerRow, int dataStartRow) {}

    @Data
    private static class RowValidationError {
        private final int rowIndex;
        private final int colIndex;
        private final String reason;
        private final String message;
    }

    @Data
    private static class CleaningFileMeta {
        private final File file;
        private final Long creatorUserId;
        private final long createAtMs;
    }

    @Data
    private static class ValidationResult {
        private final int totalCount;
        private final List<RowValidationError> errors;
    }
}
