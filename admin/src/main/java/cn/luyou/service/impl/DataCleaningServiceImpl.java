package cn.luyou.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.model.DataCleaningResult;
import cn.luyou.service.DataCleaningService;
import cn.luyou.utils.CloseContactCaseExcelSupport;
import cn.luyou.utils.FlexibleDateParseUtil;
import cn.luyou.utils.ImportIdentitySupport;
import cn.luyou.utils.InfectionScreenFieldSupport;
import cn.luyou.utils.ScreeningDiagnosisSupport;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.DirectoryStream;
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

@Slf4j
@Service
public class DataCleaningServiceImpl implements DataCleaningService {
    private static final String TYPE_SCHOOL = "school";
    private static final String TYPE_KEY = "keyPopulation";
    private static final String TYPE_CLOSE = "closeContact";
    private static final int MAX_ERROR_PREVIEW = 200;
    private static final long FILE_EXPIRE_MS = 30 * 60 * 1000L;
    private static final Path RESULT_DIR = Path.of(System.getProperty("java.io.tmpdir"), "disease-cleaning");
    private static final DataFormatter DATA_FORMATTER = new DataFormatter();
    private static final List<String> SCHOOL_HEADER_TOP = List.of(
            "填报机构", "市州", "县区", "乡镇/街道", "类型", "是否寄宿制", "学校名称（全称）", "姓名", "年份", "性别", "身份证号",
            "年龄", "户籍所在地", "年级", "班级", "民族", "是否参加筛查", "有无既往结核病史", "有无肺结核接触史",
            "结核病可疑症状", "结核病可疑症状", "结核病可疑症状",
            "感染筛查", "感染筛查", "感染筛查", "感染筛查",
            "胸部影像学", "胸部影像学", "胸部影像学",
            "分子生物学结果", "痰培养结果", "筛查结果", "备注"
    );
    private static final List<String> SCHOOL_HEADER_SUB = List.of(
            "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
            "咳嗽，咳痰≥两周", "咯血或血痰", "其他",
            "感染筛查时间", "方法", "结果", "判定结果",
            "胸片检查时间", "方法", "结果",
            "", "", "", ""
    );
    private static final List<String> SCHOOL_FIELD_KEYS = List.of(
            "reportingOrg", "city", "district", "township", "schoolType", "boardingType", "schoolName", "name", "year", "gender", "idNumber",
            "age", "householdAddress", "gradeName", "className", "ethnicity", "participatedScreening", "tbHistory", "closeContactHistory",
            "symptomCough", "symptomHemoptysis", "symptomOther",
            "screenDate", "screenMethod", "screenResult", "infectionResult",
            "chestXrayDate", "chestXrayMethod", "chestXrayResult",
            "molecularBiologyResult", "sputumCultureResult", "diagnosisResult", "remark"
    );

    private final Map<String, CleaningFileMeta> resultFileStore = new ConcurrentHashMap<>();

    @Override
    public DataCleaningResult clean(String populationType, MultipartFile file) {
        String type = normalizeType(populationType);
        validateExcel(file);
        byte[] fileBytes = readUploadBytes(file);

        int headRowNumber = resolveHeadRows(type, fileBytes);
        CloseContactColumnLayout closeLayout = TYPE_CLOSE.equals(type)
                ? resolveCloseContactLayout(fileBytes, headRowNumber)
                : null;
        ValidationResult validationResult = readAndValidate(type, fileBytes, headRowNumber, closeLayout);
        List<RowValidationError> allErrors = validationResult.getErrors();
        String fileId = IdUtil.fastSimpleUUID();
        File resultFile = markAndWriteResult(type, fileBytes, fileId, allErrors, headRowNumber);
        persistResultMeta(fileId, resultFile, currentUserId());

        List<String> previewErrors = allErrors.stream()
                .limit(MAX_ERROR_PREVIEW)
                .map(RowValidationError::getMessage)
                .toList();
        return DataCleaningResult.builder()
                .totalCount(validationResult.getTotalCount())
                .abnormalCount((int) allErrors.stream().map(RowValidationError::getRowIndex).distinct().count())
                .errorItemCount(allErrors.size())
                .fileId(fileId)
                .fileName(resultFile.getName())
                .errors(previewErrors)
                .build();
    }

    @Override
    public DataCleaningResult matchSchool(MultipartFile file) {
        validateExcel(file);
        byte[] fileBytes = readUploadBytes(file);
        String fileId = IdUtil.fastSimpleUUID();
        File resultFile = buildSchoolMatchedFile(fileBytes, fileId);
        persistResultMeta(fileId, resultFile, currentUserId());
        int totalCount = countDataRows(resultFile);
        return DataCleaningResult.builder()
                .totalCount(totalCount)
                .abnormalCount(0)
                .errorItemCount(0)
                .fileId(fileId)
                .fileName(resultFile.getName())
                .errors(List.of())
                .build();
    }

    @Override
    public Resource getResultFile(String fileId, Long currentUserId, boolean isSuperAdmin) {
        CleaningFileMeta meta = resolveResultMeta(fileId);
        if (meta == null || meta.getFile() == null || !meta.getFile().exists()) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "清洗结果文件不存在或已过期，请重新清洗");
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

    private byte[] readUploadBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel 文件读取失败: " + e.getMessage());
        }
    }

    private int resolveHeadRows(String type, byte[] fileBytes) {
        if (TYPE_KEY.equals(type)) {
            return 3;
        }
        if (TYPE_CLOSE.equals(type)) {
            try {
                return CloseContactCaseExcelSupport.resolveHeadRowNumber(fileBytes);
            } catch (IOException e) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel文件读取失败: " + e.getMessage());
            }
        }
        return 2;
    }

    /**
     * 密接 72 列官方模板与旧版（含原患者身份证号 / 无「是否开展预防治疗」列）列位不同。
     */
    private CloseContactColumnLayout resolveCloseContactLayout(byte[] fileBytes, int headRowNumber) {
        try (InputStream inputStream = new ByteArrayInputStream(fileBytes);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                return CloseContactColumnLayout.STANDARD_71;
            }
            int headerRowIndex = Math.max(0, headRowNumber - 1);
            Row headerRow = sheet.getRow(headerRowIndex);
            String col6Header = getPoiCellString(headerRow, 6);
            if (StrUtil.isNotBlank(col6Header) && col6Header.contains("身份证")) {
                return CloseContactColumnLayout.LEGACY_73;
            }
            return CloseContactColumnLayout.STANDARD_71;
        } catch (IOException e) {
            return CloseContactColumnLayout.STANDARD_71;
        }
    }

    private ValidationResult readAndValidate(
            String type,
            byte[] fileBytes,
            int headRowNumber,
            CloseContactColumnLayout closeLayout
    ) {
        List<RowValidationError> errors = new ArrayList<>();
        AtomicInteger totalCount = new AtomicInteger(0);
        AtomicInteger rowOffset = new AtomicInteger(0);
        try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
            EasyExcel.read(inputStream, List.class, new PageReadListener<List<Object>>(rows -> {
                        for (List<Object> row : rows) {
                            int excelRowIndex = headRowNumber + rowOffset.getAndIncrement() + 1;
                            if (shouldSkipValidationRow(type, row, closeLayout)) {
                                continue;
                            }
                            totalCount.incrementAndGet();
                            errors.addAll(validateRow(type, row, excelRowIndex, closeLayout));
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

    private List<RowValidationError> validateRow(
            String type,
            List<Object> row,
            int excelRowIndex,
            CloseContactColumnLayout closeLayout
    ) {
        List<RowValidationError> result = new ArrayList<>();
        int nameCol = TYPE_KEY.equals(type) ? 4 : (TYPE_CLOSE.equals(type) ? closeLayout.nameCol() : 7);
        String name = getCellString(row, nameCol);

        String idCard;
        String phone;
        String infectionResult;
        if (TYPE_SCHOOL.equals(type)) {
            // 新表：姓名=7，身份证号=10；无电话列；含是否寄宿制、年级
            idCard = getCellString(row, 10);
            if (ImportIdentitySupport.isBlankOrPlaceholder(idCard)) {
                result.add(err(excelRowIndex, name, 10, "未填写身份证号（可继续，建议后续补全）"));
            } else if (!isValidIdCard(idCard)) {
                result.add(err(excelRowIndex, name, 10, "身份证号格式不正确"));
            }
            String gender = getCellString(row, 9);
            if (StrUtil.isNotBlank(gender) && !isInOptions(gender, "男", "女")) {
                result.add(err(excelRowIndex, name, 9, "性别仅支持：男/女"));
            }
            validateOption(result, excelRowIndex, name, row, 4, "类型仅支持：1-7或对应中文",
                    "1", "2", "3", "4", "5", "6", "7",
                    "托幼机构", "小学", "初中", "高中阶段教育学校", "高等教育学校", "教职工", "其他");
            validateOption(result, excelRowIndex, name, row, 5, "是否寄宿制仅支持：1-4或对应中文",
                    "1", "2", "3", "4",
                    "寄宿制", "非寄宿制", "大学", "其他");
            validateOption(result, excelRowIndex, name, row, 16, "是否参加筛查仅支持：是/否", "是", "否");
            validateOption(result, excelRowIndex, name, row, 17, "既往结核病史仅支持：有/无", "有", "无");
            validateOption(result, excelRowIndex, name, row, 18, "肺结核接触史仅支持：有/无", "有", "无");
            validateOption(result, excelRowIndex, name, row, 19, "咳嗽咳痰≥两周仅支持：有/无", "有", "无");
            validateOption(result, excelRowIndex, name, row, 20, "咯血或血痰仅支持：有/无", "有", "无");
            validateOption(result, excelRowIndex, name, row, 21, "可疑症状-其他仅支持：有/无", "有", "无");
            validateOption(result, excelRowIndex, name, row, 23, "感染筛查方法仅支持：1-4或PPD/EC/IGRA/未查",
                    "1", "2", "3", "4", "PPD", "EC", "IGRA", "未查");
            validateOption(result, excelRowIndex, name, row, 25, "判定结果仅支持：0-3或对应中文",
                    "0", "1", "2", "3", "未感染", "感染", "无法判读", "未查",
                    "PPD阴性", "PPD+", "PPD++", "PPD+++", "EC阴性", "EC阳性", "IGRA阴性", "IGRA阳性");
            validateOption(result, excelRowIndex, name, row, 27, "胸部影像学方法仅支持：1-4或对应中文",
                    "1", "2", "3", "4", "胸部X线", "胸部CT", "其他", "未查");
            validateOption(result, excelRowIndex, name, row, 28, "胸部影像学结果仅支持：0-4或对应中文",
                    "0", "1", "2", "3", "4",
                    "未见异常", "异常（疑似活动性结核病变）", "异常（非活动性结核病变）", "其他", "未查",
                    "正常", "异常");
            validateOption(result, excelRowIndex, name, row, 29, "分子生物学结果仅支持：0-3或对应中文",
                    "0", "1", "2", "3", "阴性", "阳性", "无法判读", "未查");
            validateOption(result, excelRowIndex, name, row, 30, "痰培养结果仅支持：0-3或对应中文",
                    "0", "1", "2", "3", "阴性", "阳性", "无法判读", "未查");
            validateOption(result, excelRowIndex, name, row, 31, "筛查结果仅支持：0-4或系统诊断文案",
                    "0", "1", "2", "3", "4",
                    "未发现异常", "活动性肺结核", "疑似肺结核", "潜伏感染者", "其他", "其它",
                    "排除", "确诊患者", "疑似结核", "正常");
            // EC/IGRA 结果、未查结果
            String screenMethod = getCellString(row, 23);
            String screenResult = getCellString(row, 24);
            if (StrUtil.isNotBlank(screenResult)) {
                if (isInOptions(screenMethod, "2", "3", "EC", "IGRA")
                        && !isInOptions(screenResult, "阳性", "阴性")) {
                    result.add(err(excelRowIndex, name, 24, "感染筛查结果（EC/IGRA）仅支持：阳性/阴性"));
                } else if (isInOptions(screenMethod, "4", "未查") && !"无".equals(screenResult)) {
                    result.add(err(excelRowIndex, name, 24, "感染筛查方法为未查时，结果仅支持填写「无」"));
                }
            }
        } else if (TYPE_KEY.equals(type)) {
            idCard = getCellString(row, 9);
            phone = getCellString(row, 11);
            infectionResult = getCellString(row, 36);
            // 感染结果单独按新口径校验，此处不传 infectionResult
            appendCommonValidation(result, excelRowIndex, name, idCard, 9, phone, 11, null, -1);
            String screenMethod = getCellString(row, 34);
            if (StrUtil.isNotBlank(screenMethod) && !InfectionScreenFieldSupport.isValidMethod(screenMethod)) {
                result.add(err(excelRowIndex, name, 34,
                        "感染筛查方法仅支持：结核菌素皮肤试验_PPD/结核抗原皮肤试验_EC/γ干扰素释放试验_IGRA/未做（兼容PPD/EC/IGRA/未查）"));
            }
            if (StrUtil.isNotBlank(infectionResult) && !InfectionScreenFieldSupport.isValidResult(infectionResult)) {
                result.add(err(excelRowIndex, name, 36,
                        "感染筛查结果仅支持：一般阳性/中度阳性/强阳性/阳性/阴性/未判读"));
            }
            validateKeyPopulationDiagnosisCell(result, excelRowIndex, name, row, 40, "首次诊断结果");
            validateKeyPopulationDiagnosisCell(result, excelRowIndex, name, row, 41, "半年诊断结果");
            validateKeyPopulationDiagnosisCell(result, excelRowIndex, name, row, 42, "一年诊断结果");
            for (int idx = 15; idx <= 22; idx++) {
                String value = getCellString(row, idx);
                if (StrUtil.isNotBlank(value) && !isInOptions(value, "是", "否")) {
                    result.add(err(excelRowIndex, name, idx, "人群分类列仅支持：是/否"));
                }
            }
        } else {
            int idCol = closeLayout.idCol();
            int phoneCol = closeLayout.phoneCol();
            int finalResultCol = closeLayout.finalResultCol();
            idCard = getCellString(row, idCol);
            phone = getCellString(row, phoneCol);
            appendCommonValidation(result, excelRowIndex, name, idCard, idCol, phone, phoneCol, null, -1);
            int infectionMethodCol = finalResultCol - 8;
            int infectionResultCol = finalResultCol - 7;
            String infectionMethod = getCellString(row, infectionMethodCol);
            String infectionJudge = getCellString(row, infectionResultCol);
            if (StrUtil.isNotBlank(infectionMethod) && !InfectionScreenFieldSupport.isValidMethod(infectionMethod)) {
                result.add(err(excelRowIndex, name, infectionMethodCol,
                        "感染筛查方法仅支持：结核菌素皮肤试验_PPD/结核抗原皮肤试验_EC/γ干扰素释放试验_IGRA/未做（兼容PPD/EC/IGRA/未查）"));
            }
            if (StrUtil.isNotBlank(infectionJudge) && !InfectionScreenFieldSupport.isValidResult(infectionJudge)) {
                result.add(err(excelRowIndex, name, infectionResultCol,
                        "结果判定仅支持：一般阳性/中度阳性/强阳性/阳性/阴性/未判读"));
            }
            String finalResult = getCellString(row, finalResultCol);
            if (StrUtil.isNotBlank(finalResult)
                    && !isInOptions(finalResult, "活动性肺结核", "潜伏感染者", "未做", "未发现异常")) {
                result.add(err(excelRowIndex, name, finalResultCol,
                        "最终筛查结果仅支持：活动性肺结核/潜伏感染者/未做/未发现异常"));
            }
        }
        return result;
    }

    private boolean shouldSkipValidationRow(String type, List<Object> row, CloseContactColumnLayout closeLayout) {
        int nameIndex;
        int idIndex;
        if (TYPE_KEY.equals(type)) {
            nameIndex = 4;
            idIndex = 9;
        } else if (TYPE_CLOSE.equals(type)) {
            nameIndex = closeLayout.nameCol();
            idIndex = closeLayout.idCol();
        } else {
            // 学生筛查新表：姓名=7，身份证号=10
            nameIndex = 7;
            idIndex = 10;
        }
        return StrUtil.isBlank(getCellString(row, nameIndex))
                && ImportIdentitySupport.isBlankOrPlaceholder(getCellString(row, idIndex));
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
        if (ImportIdentitySupport.isBlankOrPlaceholder(idCard)) {
            result.add(err(excelRowIndex, name, idCardCol, "未填写身份证号（可继续，建议后续补全）"));
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

    private void validateKeyPopulationDiagnosisCell(List<RowValidationError> result, int excelRowIndex, String name,
                                                    List<Object> row, int colIndex, String fieldLabel) {
        String value = getCellString(row, colIndex);
        if (StrUtil.isNotBlank(value) && !ScreeningDiagnosisSupport.isValidKeyPopulationDiagnosis(value)) {
            result.add(err(excelRowIndex, name, colIndex,
                    fieldLabel + "仅支持：排除/正常/疑似结核/确诊结核/潜伏感染者/在治患者"));
        }
    }

    private boolean isInOptions(String value, String... options) {
        if (value == null) return false;
        String normalized = normalizeOptionToken(value);
        for (String option : options) {
            if (option == null) continue;
            String opt = normalizeOptionToken(option);
            if (opt.equals(normalized)) return true;
            // 其他（需注明）/其他（培训学校…）
            if (normalized.startsWith(opt + "（") || normalized.startsWith(opt + "(")) return true;
        }
        return false;
    }

    /** Excel 数值 1.0、以及「（需注明）」说明后缀归一化 */
    private String normalizeOptionToken(String value) {
        String trimmed = value.trim();
        if (trimmed.matches("\\d+(\\.0+)?")) {
            trimmed = trimmed.replaceAll("\\.0+$", "");
        }
        return trimmed.replaceAll("[（(]需注明[）)]", "").trim();
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
        if (value instanceof Number number) {
            double d = number.doubleValue();
            if (!Double.isInfinite(d) && !Double.isNaN(d) && d == Math.floor(d)) {
                return String.valueOf(number.longValue());
            }
        }
        return normalizeOptionToken(String.valueOf(value));
    }

    private void validateOption(List<RowValidationError> result, int excelRowIndex, String name,
                                List<Object> row, int colIndex, String reason, String... options) {
        String value = getCellString(row, colIndex);
        if (StrUtil.isNotBlank(value) && !isInOptions(value, options)) {
            result.add(err(excelRowIndex, name, colIndex, reason));
        }
    }

    private File buildSchoolMatchedFile(byte[] sourceBytes, String fileId) {
        try {
            Path dir = Files.createDirectories(RESULT_DIR);
            Path path = dir.resolve("学生筛查数据匹配结果_" + fileId + ".xlsx");
            try (InputStream inputStream = new ByteArrayInputStream(sourceBytes);
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
        String carriedGroup = "";
        Integer nameCol = null;
        for (int col = 0; col < last; col++) {
            String mainText = getPoiCellString(main, col);
            String subText = getPoiCellString(sub, col);
            if (StrUtil.isNotBlank(mainText)) {
                String normalizedMain = normalizeHeader(mainText);
                if ("结核病可疑症状".equals(normalizedMain)
                        || "感染筛查".equals(normalizedMain)
                        || "胸部影像学".equals(normalizedMain)
                        || normalizedMain.contains("感染筛查情况")
                        || normalizedMain.contains("胸片检查")) {
                    carriedGroup = mainText;
                } else if (StrUtil.isBlank(subText)) {
                    carriedGroup = "";
                }
            }
            String resolved = resolveMatchHeaderKey(mainText, subText, carriedGroup);
            putMatchAlias(index, resolved, col);
            putMatchAlias(index, mainText, col);
            putMatchAlias(index, subText, col);
            if ("姓名".equals(normalizeHeader(mainText)) || "姓名".equals(normalizeHeader(subText))) {
                nameCol = col;
            }
        }
        // 年份：优先姓名右侧列
        Integer yearCol = null;
        Integer yearFallback = null;
        for (int col = 0; col < last; col++) {
            String h = normalizeHeader(getPoiCellString(main, col));
            String s = normalizeHeader(getPoiCellString(sub, col));
            if (!"年份".equals(h) && !"年度".equals(h) && !"年份".equals(s) && !"年度".equals(s)) {
                continue;
            }
            yearFallback = col;
            if (nameCol != null && col > nameCol) {
                yearCol = col;
                break;
            }
        }
        if (yearCol == null) yearCol = yearFallback;
        if (yearCol != null) {
            index.put("year", yearCol);
        }
        return index;
    }

    private String resolveMatchHeaderKey(String main, String sub, String carriedGroup) {
        String m = StrUtil.blankToDefault(main, "");
        String s = StrUtil.blankToDefault(sub, "");
        String g = StrUtil.blankToDefault(carriedGroup, "");
        if (StrUtil.isNotBlank(s)) {
            if ("方法".equals(s)) {
                if (g.contains("感染") || m.contains("感染")) return "感染筛查方法";
                if (g.contains("胸") || m.contains("胸")) return "胸部影像学方法";
            }
            if ("结果".equals(s) || s.startsWith("结果")) {
                if (g.contains("感染") || m.contains("感染")) return "感染筛查原始结果";
                if (g.contains("胸") || m.contains("胸")) return "胸片结果";
            }
            if ("其他".equals(s) && (g.contains("可疑") || m.contains("可疑"))) {
                return "可疑症状其他";
            }
        }
        return StrUtil.isNotBlank(s) ? s : m;
    }

    private void putMatchAlias(Map<String, Integer> index, String rawHeader, int col) {
        if (StrUtil.isBlank(rawHeader)) return;
        String header = normalizeHeader(rawHeader);
        if (StrUtil.isBlank(header)) return;
        String key = switch (header) {
            case "序号" -> "seq";
            case "年份", "年度" -> "year";
            case "填报机构" -> "reportingOrg";
            case "市州" -> "city";
            case "县市区", "区县", "县区" -> "district";
            case "乡镇街道", "乡镇社区" -> "township";
            case "姓名" -> "name";
            case "性别" -> "gender";
            case "出生日期" -> "birthDate";
            case "年龄", "年龄根据身份证号自动生成" -> "age";
            case "证件类型" -> "idType";
            case "证件号", "身份证号", "身份证号码" -> "idNumber";
            case "民族" -> "ethnicity";
            case "是否参加筛查" -> "participatedScreening";
            case "联系电话" -> "phone";
            case "户籍所在地XX市XX县区", "户籍所在地" -> "householdAddress";
            case "现地址", "现住址" -> "currentAddress";
            case "学校类型", "类型" -> "schoolType";
            case "是否寄宿制" -> "boardingType";
            case "学校名称", "学校名称全称" -> "schoolName";
            case "年级" -> "gradeName";
            case "班级院系", "班级" -> "className";
            case "既往结核病史", "有无既往结核病史" -> "tbHistory";
            case "密切接触史", "有无肺结核接触史" -> "closeContactHistory";
            case "结核病可疑症状" -> "suspiciousSymptoms";
            case "咳嗽咳痰两周", "咳嗽咳痰≥两周", "咳嗽咳痰" -> "symptomCough";
            case "咯血或血痰" -> "symptomHemoptysis";
            case "其他", "可疑症状其他" -> "symptomOther";
            case "是否进行感染筛" -> "hasInfectionScreen";
            case "感染筛查日期", "感染筛查时间" -> "screenDate";
            case "方法", "感染筛查方法" -> "screenMethod";
            case "结果PPDmmXmmEC及IGRA阳性阴性", "结果", "感染筛查原始结果" -> "screenResult";
            case "感染筛查结果", "判定结果", "感染筛查判定结果" -> "infectionResult";
            case "是否进行胸片检查" -> "hasChestXray";
            case "胸片检查日期", "胸片检查时间" -> "chestXrayDate";
            case "胸部影像学方法" -> "chestXrayMethod";
            case "胸片结果", "胸部DR" -> "chestXrayResult";
            case "痰涂片", "痰涂片结果" -> "sputumSmearResult";
            case "分子生物学", "分子生物学结果" -> "molecularBiologyResult";
            case "痰培养", "痰培养结果" -> "sputumCultureResult";
            case "诊断", "诊断结果", "筛查结果" -> "diagnosisResult";
            case "符合潜伏治疗条件者是否进行预防性治疗是写出方案否填写原因", "是否进行预防者治疗", "是否进行预防性治疗", "是否开展预防治疗" -> "hasPreventiveTreatment";
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
        if (FlexibleDateParseUtil.isDateFieldKey(key)) {
            value = FlexibleDateParseUtil.normalizeToStandardString(value);
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
                && ImportIdentitySupport.isBlankOrPlaceholder(matchedValue(row, headerIndex, "idNumber", 0));
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

    private File markAndWriteResult(String type, byte[] sourceBytes, String fileId, List<RowValidationError> errors, int headRowNumber) {
        try {
            Path dir = Files.createDirectories(RESULT_DIR);
            String fileName = "数据清洗结果_" + type + "_" + fileId + ".xlsx";
            Path path = dir.resolve(fileName);

            try (InputStream inputStream = new ByteArrayInputStream(sourceBytes);
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

    private void persistResultMeta(String fileId, File resultFile, Long creatorUserId) {
        long createAtMs = System.currentTimeMillis();
        CleaningFileMeta meta = new CleaningFileMeta(resultFile, creatorUserId, createAtMs);
        resultFileStore.put(fileId, meta);
        try {
            Files.createDirectories(RESULT_DIR);
            JSONObject json = new JSONObject();
            json.set("fileId", fileId);
            json.set("filePath", resultFile.getAbsolutePath());
            json.set("creatorUserId", creatorUserId);
            json.set("createAtMs", createAtMs);
            Files.writeString(metaSidecarPath(fileId), json.toString());
        } catch (IOException e) {
            log.warn("写入数据清洗结果元数据失败 fileId={}: {}", fileId, e.getMessage());
        }
    }

    private CleaningFileMeta resolveResultMeta(String fileId) {
        CleaningFileMeta cached = resultFileStore.get(fileId);
        if (cached != null && cached.getFile() != null && cached.getFile().exists()) {
            return cached;
        }
        CleaningFileMeta fromDisk = loadMetaFromDisk(fileId);
        if (fromDisk != null) {
            resultFileStore.put(fileId, fromDisk);
        }
        return fromDisk;
    }

    private CleaningFileMeta loadMetaFromDisk(String fileId) {
        try {
            Path sidecar = metaSidecarPath(fileId);
            if (!Files.exists(sidecar)) {
                return null;
            }
            JSONObject json = JSONUtil.parseObj(Files.readString(sidecar));
            File file = new File(json.getStr("filePath", ""));
            if (!file.exists()) {
                return null;
            }
            return new CleaningFileMeta(
                    file,
                    json.getLong("creatorUserId"),
                    json.getLong("createAtMs", Files.getLastModifiedTime(file.toPath()).toMillis())
            );
        } catch (IOException e) {
            log.warn("读取数据清洗结果元数据失败 fileId={}: {}", fileId, e.getMessage());
        }
        return null;
    }

    private Path metaSidecarPath(String fileId) {
        return RESULT_DIR.resolve(fileId + ".meta.json");
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
            Files.deleteIfExists(metaSidecarPath(fileId));
        } catch (IOException ignored) {
        } finally {
            resultFileStore.remove(fileId);
        }
    }

    /** 每10分钟清理一次过期清洗结果，避免磁盘与内存堆积 */
    @Scheduled(fixedDelay = 10 * 60 * 1000L, initialDelay = 5 * 60 * 1000L)
    public void cleanExpiredResultFiles() {
        if (Files.isDirectory(RESULT_DIR)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(RESULT_DIR)) {
                for (Path path : stream) {
                    String name = path.getFileName().toString();
                    if (name.endsWith(".meta.json")) {
                        String fileId = name.substring(0, name.length() - ".meta.json".length());
                        CleaningFileMeta meta = resolveResultMeta(fileId);
                        if (meta != null && isExpired(meta.getCreateAtMs())) {
                            deleteMetaFile(fileId, meta);
                        }
                    }
                }
            } catch (IOException e) {
                log.warn("清理过期数据清洗结果失败: {}", e.getMessage());
            }
        }
        for (Map.Entry<String, CleaningFileMeta> entry : resultFileStore.entrySet()) {
            if (entry.getValue() != null && isExpired(entry.getValue().getCreateAtMs())) {
                deleteMetaFile(entry.getKey(), entry.getValue());
            }
        }
    }

    private record CloseContactColumnLayout(int nameCol, int idCol, int phoneCol, int finalResultCol) {
        /** 72 列官方模板（校验列位：姓名/证件/电话/最终筛查结果与 index 33 前一致） */
        static final CloseContactColumnLayout STANDARD_71 = new CloseContactColumnLayout(10, 11, 14, 29);
        /** 旧 73 列模板（G 列为患者身份证号） */
        static final CloseContactColumnLayout LEGACY_73 = new CloseContactColumnLayout(11, 12, 15, 30);
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
