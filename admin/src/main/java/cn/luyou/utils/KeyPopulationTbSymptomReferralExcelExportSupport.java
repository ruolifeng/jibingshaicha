package cn.luyou.utils;

import cn.luyou.model.vo.KeyPopulationTbSymptomReferralStatisticsVO;
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
import java.util.function.Function;

/**
 * 重点人群肺结核可疑症状筛查和推介情况报表 Excel 导出（多级表头对齐季度报表模板）。
 */
public final class KeyPopulationTbSymptomReferralExcelExportSupport {

    private KeyPopulationTbSymptomReferralExcelExportSupport() {
    }

    public static void write(OutputStream out, String year, List<KeyPopulationTbSymptomReferralStatisticsVO> rows)
            throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("症状筛查和推介情况");
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle bodyStyle = createBodyStyle(workbook);

            String yearLabel = (year == null || year.isBlank()) ? "" : year;
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(yearLabel + "年自贡市重点人群肺结核可疑症状筛查和推介情况报表");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 22));

            // 第 2 行：老年人 / 糖尿病患者 大分组
            Row groupRow = sheet.createRow(1);
            writeMergedHeader(sheet, groupRow, 0, 0, "地区", headerStyle);
            writeMergedHeader(sheet, groupRow, 1, 11, "老年人", headerStyle);
            writeMergedHeader(sheet, groupRow, 12, 22, "糖尿病患者", headerStyle);

            // 第 3 行：二级分组
            Row midRow = sheet.createRow(2);
            writeMergedHeader(sheet, midRow, 0, 0, "地区", headerStyle);
            writeMergedHeader(sheet, midRow, 1, 1, "老年人数", headerStyle);
            writeMergedHeader(sheet, midRow, 2, 2, "参加年度体检人数", headerStyle);
            writeMergedHeader(sheet, midRow, 3, 5, "筛查方式", headerStyle);
            writeMergedHeader(sheet, midRow, 6, 8, "筛查异常人数", headerStyle);
            writeMergedHeader(sheet, midRow, 9, 11, "转诊、推荐及确诊人数", headerStyle);
            writeMergedHeader(sheet, midRow, 12, 12, "管理的糖尿病患者数", headerStyle);
            writeMergedHeader(sheet, midRow, 13, 13, "完成糖尿病管理季度随访的患者数", headerStyle);
            writeMergedHeader(sheet, midRow, 14, 16, "筛查方式", headerStyle);
            writeMergedHeader(sheet, midRow, 17, 19, "筛查异常人数", headerStyle);
            writeMergedHeader(sheet, midRow, 20, 22, "转诊、推荐及确诊人数", headerStyle);

            // 第 4 行：细项
            String[] leafHeaders = {
                    "地区",
                    "老年人数", "参加年度体检人数",
                    "进行症状筛查人数", "开展胸部影像学筛查人数", "开展感染筛查人数",
                    "肺结核可疑症状人数", "胸部影像学筛查异常人数", "开展感染筛查异常人数",
                    "开具推介转诊单人数", "到结核病定点医疗机构就诊人数", "诊断为肺结核的人数",
                    "管理的糖尿病患者数", "完成糖尿病管理季度随访的患者数",
                    "进行症状筛查人数", "开展胸部影像学筛查人数", "开展感染筛查人数",
                    "肺结核可疑症状人数", "胸部影像学筛查异常人数", "开展感染筛查异常人数",
                    "开具推介转诊单人数", "到结核病定点医疗机构就诊人数", "诊断为肺结核的人数"
            };
            Row leafRow = sheet.createRow(3);
            for (int i = 0; i < leafHeaders.length; i++) {
                Cell cell = leafRow.createCell(i);
                cell.setCellValue(leafHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 4;
            for (KeyPopulationTbSymptomReferralStatisticsVO vo : rows) {
                Row dataRow = sheet.createRow(rowIdx++);
                writeDataRow(dataRow, vo, bodyStyle);
            }

            // 合计
            Row totalRow = sheet.createRow(rowIdx);
            Cell totalLabel = totalRow.createCell(0);
            totalLabel.setCellValue("合计");
            totalLabel.setCellStyle(bodyStyle);
            writeTotalCell(totalRow, 1, rows, KeyPopulationTbSymptomReferralStatisticsVO::getElderCount, bodyStyle);
            writeTotalCell(totalRow, 2, rows, KeyPopulationTbSymptomReferralStatisticsVO::getElderAnnualExamCount, bodyStyle);
            writeTotalCell(totalRow, 3, rows, KeyPopulationTbSymptomReferralStatisticsVO::getElderSymptomScreenCount, bodyStyle);
            writeTotalCell(totalRow, 4, rows, KeyPopulationTbSymptomReferralStatisticsVO::getElderChestXrayCount, bodyStyle);
            writeTotalCell(totalRow, 5, rows, KeyPopulationTbSymptomReferralStatisticsVO::getElderInfectionScreenCount, bodyStyle);
            writeTotalCell(totalRow, 6, rows, KeyPopulationTbSymptomReferralStatisticsVO::getElderSuspiciousSymptomCount, bodyStyle);
            writeTotalCell(totalRow, 7, rows, KeyPopulationTbSymptomReferralStatisticsVO::getElderChestXrayAbnormalCount, bodyStyle);
            writeTotalCell(totalRow, 8, rows, KeyPopulationTbSymptomReferralStatisticsVO::getElderInfectionAbnormalCount, bodyStyle);
            writeTotalCell(totalRow, 9, rows, KeyPopulationTbSymptomReferralStatisticsVO::getElderReferralFormCount, bodyStyle);
            writeTotalCell(totalRow, 10, rows, KeyPopulationTbSymptomReferralStatisticsVO::getElderArrivedCount, bodyStyle);
            writeTotalCell(totalRow, 11, rows, KeyPopulationTbSymptomReferralStatisticsVO::getElderConfirmedTbCount, bodyStyle);
            writeTotalCell(totalRow, 12, rows, KeyPopulationTbSymptomReferralStatisticsVO::getDiabetesManagedCount, bodyStyle);
            writeTotalCell(totalRow, 13, rows, KeyPopulationTbSymptomReferralStatisticsVO::getDiabetesQuarterFollowCount, bodyStyle);
            writeTotalCell(totalRow, 14, rows, KeyPopulationTbSymptomReferralStatisticsVO::getDiabetesSymptomScreenCount, bodyStyle);
            writeTotalCell(totalRow, 15, rows, KeyPopulationTbSymptomReferralStatisticsVO::getDiabetesChestXrayCount, bodyStyle);
            writeTotalCell(totalRow, 16, rows, KeyPopulationTbSymptomReferralStatisticsVO::getDiabetesInfectionScreenCount, bodyStyle);
            writeTotalCell(totalRow, 17, rows, KeyPopulationTbSymptomReferralStatisticsVO::getDiabetesSuspiciousSymptomCount, bodyStyle);
            writeTotalCell(totalRow, 18, rows, KeyPopulationTbSymptomReferralStatisticsVO::getDiabetesChestXrayAbnormalCount, bodyStyle);
            writeTotalCell(totalRow, 19, rows, KeyPopulationTbSymptomReferralStatisticsVO::getDiabetesInfectionAbnormalCount, bodyStyle);
            writeTotalCell(totalRow, 20, rows, KeyPopulationTbSymptomReferralStatisticsVO::getDiabetesReferralFormCount, bodyStyle);
            writeTotalCell(totalRow, 21, rows, KeyPopulationTbSymptomReferralStatisticsVO::getDiabetesArrivedCount, bodyStyle);
            writeTotalCell(totalRow, 22, rows, KeyPopulationTbSymptomReferralStatisticsVO::getDiabetesConfirmedTbCount, bodyStyle);

            for (int i = 0; i < leafHeaders.length; i++) {
                sheet.setColumnWidth(i, 14 * 256);
            }
            sheet.setColumnWidth(0, 16 * 256);

            workbook.write(out);
        }
    }

    private static void writeDataRow(Row row, KeyPopulationTbSymptomReferralStatisticsVO vo, CellStyle style) {
        setString(row, 0, vo.getDistrict(), style);
        setLong(row, 1, vo.getElderCount(), style);
        setLong(row, 2, vo.getElderAnnualExamCount(), style);
        setLong(row, 3, vo.getElderSymptomScreenCount(), style);
        setLong(row, 4, vo.getElderChestXrayCount(), style);
        setLong(row, 5, vo.getElderInfectionScreenCount(), style);
        setLong(row, 6, vo.getElderSuspiciousSymptomCount(), style);
        setLong(row, 7, vo.getElderChestXrayAbnormalCount(), style);
        setLong(row, 8, vo.getElderInfectionAbnormalCount(), style);
        setLong(row, 9, vo.getElderReferralFormCount(), style);
        setLong(row, 10, vo.getElderArrivedCount(), style);
        setLong(row, 11, vo.getElderConfirmedTbCount(), style);
        setLong(row, 12, vo.getDiabetesManagedCount(), style);
        setLong(row, 13, vo.getDiabetesQuarterFollowCount(), style);
        setLong(row, 14, vo.getDiabetesSymptomScreenCount(), style);
        setLong(row, 15, vo.getDiabetesChestXrayCount(), style);
        setLong(row, 16, vo.getDiabetesInfectionScreenCount(), style);
        setLong(row, 17, vo.getDiabetesSuspiciousSymptomCount(), style);
        setLong(row, 18, vo.getDiabetesChestXrayAbnormalCount(), style);
        setLong(row, 19, vo.getDiabetesInfectionAbnormalCount(), style);
        setLong(row, 20, vo.getDiabetesReferralFormCount(), style);
        setLong(row, 21, vo.getDiabetesArrivedCount(), style);
        setLong(row, 22, vo.getDiabetesConfirmedTbCount(), style);
    }

    private static void writeTotalCell(Row row, int col, List<KeyPopulationTbSymptomReferralStatisticsVO> rows,
                                       Function<KeyPopulationTbSymptomReferralStatisticsVO, Long> getter,
                                       CellStyle style) {
        long sum = 0L;
        boolean hasValue = false;
        for (KeyPopulationTbSymptomReferralStatisticsVO r : rows) {
            Long value = getter.apply(r);
            if (value != null) {
                sum += value;
                hasValue = true;
            }
        }
        Cell cell = row.createCell(col);
        if (hasValue) {
            cell.setCellValue(sum);
        } else {
            cell.setBlank();
        }
        cell.setCellStyle(style);
    }

    private static void setString(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private static void setLong(Row row, int col, Long value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value == null) {
            cell.setBlank();
        } else {
            cell.setCellValue(value);
        }
        cell.setCellStyle(style);
    }

    private static void writeMergedHeader(Sheet sheet, Row row, int fromCol, int toCol, String text, CellStyle style) {
        Cell cell = row.createCell(fromCol);
        cell.setCellValue(text);
        cell.setCellStyle(style);
        if (fromCol != toCol) {
            sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), fromCol, toCol));
            for (int c = fromCol + 1; c <= toCol; c++) {
                Cell extra = row.createCell(c);
                extra.setCellStyle(style);
            }
        }
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
        style.setWrapText(true);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
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
