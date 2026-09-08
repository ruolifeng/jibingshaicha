package cn.luyou.utils;

import cn.luyou.constant.SchoolScreeningExcelHeaders;
import cn.luyou.model.SchoolScreeningExcelExportRow;
import cn.luyou.model.ScreeningSchool;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.List;

/**
 * 学生筛查导出：两级表头 + 全部业务列；导出额外追加录入用户/录入时间。
 * <p>不再输出标题行、空行、填写说明行。
 */
public final class SchoolScreeningExcelExportSupport {

    private SchoolScreeningExcelExportSupport() {
    }

    public static void write(OutputStream outputStream, List<ScreeningSchool> records) throws IOException {
        writeWorkbook(outputStream, records, true);
    }

    public static void writeTemplate(OutputStream outputStream) throws IOException {
        writeWorkbook(outputStream, List.of(), false);
    }

    private static void writeWorkbook(OutputStream outputStream, List<ScreeningSchool> records, boolean includeMeta)
            throws IOException {
        int colCount = includeMeta
                ? SchoolScreeningExcelHeaders.BIZ_COLUMN_COUNT + 2
                : SchoolScreeningExcelHeaders.BIZ_COLUMN_COUNT;
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(SchoolScreeningExcelHeaders.SHEET_NAME);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle bodyStyle = createBodyStyle(workbook);
            if (SchoolScreeningExcelHeaders.TOP_HEADERS.length != SchoolScreeningExcelHeaders.BIZ_COLUMN_COUNT
                    || SchoolScreeningExcelHeaders.SUB_HEADERS.length != SchoolScreeningExcelHeaders.BIZ_COLUMN_COUNT) {
                throw new IllegalStateException("学生筛查 Excel 表头列数与官方 33 列不一致");
            }

            // 第 1–2 行：两级表头
            Row topRow = sheet.createRow(0);
            Row subRow = sheet.createRow(1);
            topRow.setHeightInPoints(54);
            subRow.setHeightInPoints(29);
            String[] top = SchoolScreeningExcelHeaders.TOP_HEADERS;
            String[] sub = SchoolScreeningExcelHeaders.SUB_HEADERS;
            for (int c = 0; c < SchoolScreeningExcelHeaders.BIZ_COLUMN_COUNT; c++) {
                writeHeaderCell(topRow, c, top[c], headerStyle);
                writeHeaderCell(subRow, c, sub[c], headerStyle);
            }
            if (includeMeta) {
                writeHeaderCell(topRow, 33, "录入用户", headerStyle);
                writeHeaderCell(subRow, 33, "", headerStyle);
                writeHeaderCell(topRow, 34, "录入时间", headerStyle);
                writeHeaderCell(subRow, 34, "", headerStyle);
            }
            mergeGroup(sheet, 0, 19, 21);
            mergeGroup(sheet, 0, 22, 25);
            mergeGroup(sheet, 0, 26, 28);
            for (int c = 0; c < colCount; c++) {
                boolean grouped = (c >= 19 && c <= 21) || (c >= 22 && c <= 25) || (c >= 26 && c <= 28);
                if (!grouped) {
                    sheet.addMergedRegion(new CellRangeAddress(0, 1, c, c));
                }
            }

            // 第 3 行起：数据
            int rowIdx = 2;
            if (records != null) {
                for (ScreeningSchool record : records) {
                    Row dataRow = sheet.createRow(rowIdx++);
                    writeDataRow(dataRow, SchoolScreeningExcelExportRow.from(record), includeMeta, bodyStyle);
                }
            }

            for (int c = 0; c < colCount; c++) {
                sheet.setColumnWidth(c, columnWidth(c));
            }
            workbook.write(outputStream);
        }
    }

    private static void writeDataRow(Row row, SchoolScreeningExcelExportRow source, boolean includeMeta, CellStyle style) {
        set(row, 0, source.getReportingOrg(), style);
        set(row, 1, source.getCity(), style);
        set(row, 2, source.getDistrict(), style);
        set(row, 3, source.getTownship(), style);
        set(row, 4, source.getSchoolType(), style);
        set(row, 5, source.getBoardingType(), style);
        set(row, 6, source.getSchoolName(), style);
        set(row, 7, source.getName(), style);
        set(row, 8, source.getYear(), style);
        set(row, 9, source.getGender(), style);
        set(row, 10, source.getIdNumber(), style);
        set(row, 11, source.getAge(), style);
        set(row, 12, source.getHouseholdAddress(), style);
        set(row, 13, source.getGradeName(), style);
        set(row, 14, source.getClassName(), style);
        set(row, 15, source.getEthnicity(), style);
        set(row, 16, source.getParticipatedScreening(), style);
        set(row, 17, source.getTbHistory(), style);
        set(row, 18, source.getCloseContactHistory(), style);
        set(row, 19, source.getSymptomCough(), style);
        set(row, 20, source.getSymptomHemoptysis(), style);
        set(row, 21, source.getSymptomOther(), style);
        set(row, 22, source.getScreenDate(), style);
        set(row, 23, source.getScreenMethod(), style);
        set(row, 24, source.getScreenResult(), style);
        set(row, 25, source.getInfectionResult(), style);
        set(row, 26, source.getChestXrayDate(), style);
        set(row, 27, source.getChestXrayMethod(), style);
        set(row, 28, source.getChestXrayResult(), style);
        set(row, 29, source.getMolecularBiologyResult(), style);
        set(row, 30, source.getSputumCultureResult(), style);
        set(row, 31, source.getDiagnosisFirst(), style);
        set(row, 32, source.getRemark(), style);
        if (includeMeta) {
            set(row, 33, source.getCreatorUsername(), style);
            set(row, 34, source.getCreateTime(), style);
        }
    }

    private static void set(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private static void set(Row row, int col, Integer value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value == null) {
            cell.setBlank();
        } else {
            cell.setCellValue(value);
        }
        cell.setCellStyle(style);
    }

    private static void set(Row row, int col, LocalDate value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value == null ? "" : value.toString());
        cell.setCellStyle(style);
    }

    private static void writeHeaderCell(Row row, int col, String text, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(text == null ? "" : text);
        cell.setCellStyle(style);
    }

    private static void mergeGroup(Sheet sheet, int row, int fromCol, int toCol) {
        if (fromCol < toCol) {
            sheet.addMergedRegion(new CellRangeAddress(row, row, fromCol, toCol));
        }
    }

    private static int columnWidth(int col) {
        return switch (col) {
            case 4, 6, 10, 12, 33, 34 -> 18 * 256;
            case 23, 24, 25, 27, 28, 29, 30, 31 -> 22 * 256;
            default -> 12 * 256;
        };
    }

    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        applyThinBorder(style);
        return style;
    }

    private static CellStyle createBodyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        applyThinBorder(style);
        return style;
    }

    private static void applyThinBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
