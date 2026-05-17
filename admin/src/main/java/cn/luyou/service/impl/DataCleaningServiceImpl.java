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
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
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
import java.util.ArrayList;
import java.util.Arrays;
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
        try (InputStream inputStream = file.getInputStream()) {
            EasyExcel.read(inputStream, List.class, new PageReadListener<List<Object>>(rows -> {
                        for (List<Object> row : rows) {
                            int rowOffset = totalCount.getAndIncrement();
                            int excelRowIndex = headRowNumber + rowOffset + 1;
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
            String hasInfectionScreen = getCellString(row, 20);
            if (StrUtil.isNotBlank(hasInfectionScreen) && !isInOptions(hasInfectionScreen, "是", "否")) {
                result.add(err(excelRowIndex, name, 20, "是否进行感染筛仅支持：是/否"));
            }
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
