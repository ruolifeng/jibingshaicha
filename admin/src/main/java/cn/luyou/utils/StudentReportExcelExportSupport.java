package cn.luyou.utils;

import cn.luyou.model.vo.StudentReportStatisticsVO;
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
import java.util.List;

/**
 * 新生入学体检结核病检查情况（学生报表）Excel 导出，对齐《学生统计报表》表头。
 */
public final class StudentReportExcelExportSupport {

    private StudentReportExcelExportSupport() {
    }

    public static void write(OutputStream out, String year, List<StudentReportStatisticsVO> rows) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("学生报表");
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle bodyStyle = createBodyStyle(workbook);

            String yearLabel = (year == null || year.isBlank()) ? "" : year;
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(yearLabel.isEmpty()
                    ? "新生入学体检结核病检查情况"
                    : yearLabel + "年新生入学体检结核病检查情况");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

            // 第 2 行：学校分类 / 入学新生人数 / 结核病检查情况 / 发现肺结核患者例数
            Row groupRow = sheet.createRow(1);
            writeCell(groupRow, 0, "学校分类", headerStyle);
            writeCell(groupRow, 1, "入学新生人数", headerStyle);
            writeCell(groupRow, 2, "结核病检查情况", headerStyle);
            writeCell(groupRow, 3, "", headerStyle);
            writeCell(groupRow, 4, "发现肺结核患者例数", headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 2, 0, 0));
            sheet.addMergedRegion(new CellRangeAddress(1, 2, 1, 1));
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 2, 3));
            sheet.addMergedRegion(new CellRangeAddress(1, 2, 4, 4));

            // 第 3 行：接受检查人数 / 接受规范检查人数
            Row leafRow = sheet.createRow(2);
            writeCell(leafRow, 0, "", headerStyle);
            writeCell(leafRow, 1, "", headerStyle);
            writeCell(leafRow, 2, "接受检查人数", headerStyle);
            writeCell(leafRow, 3, "接受规范检查人数", headerStyle);
            writeCell(leafRow, 4, "", headerStyle);

            int rowIdx = 3;
            for (StudentReportStatisticsVO vo : rows) {
                Row dataRow = sheet.createRow(rowIdx++);
                writeCell(dataRow, 0, vo.getSchoolCategory(), bodyStyle);
                writeNumberCell(dataRow, 1, vo.getEnrollmentCount(), bodyStyle);
                writeNumberCell(dataRow, 2, vo.getAcceptedExamCount(), bodyStyle);
                writeNumberCell(dataRow, 3, vo.getStandardizedExamCount(), bodyStyle);
                writeNumberCell(dataRow, 4, vo.getTbPatientCount(), bodyStyle);
            }

            sheet.setColumnWidth(0, 22 * 256);
            sheet.setColumnWidth(1, 14 * 256);
            sheet.setColumnWidth(2, 14 * 256);
            sheet.setColumnWidth(3, 18 * 256);
            sheet.setColumnWidth(4, 20 * 256);

            workbook.write(out);
        }
    }

    private static void writeCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private static void writeNumberCell(Row row, int col, Long value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value == null ? 0 : value.doubleValue());
        cell.setCellStyle(style);
    }

    private static CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(true);
        return style;
    }

    private static CellStyle createBodyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
}
