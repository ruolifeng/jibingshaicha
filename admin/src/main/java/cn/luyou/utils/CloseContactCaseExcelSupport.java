package cn.luyou.utils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 密接个案表 Excel 表头行数识别（兼容：无表头单行数据 / 单行表头 / 双行表头官方模板）。
 */
public final class CloseContactCaseExcelSupport {

    private static final DataFormatter FORMATTER = new DataFormatter();

    private CloseContactCaseExcelSupport() {
    }

    /**
     * @return 表头占用行数：0=首行即数据，1=第1行为表头，2=前两行为表头
     */
    public static int resolveHeadRowNumber(byte[] content) throws IOException {
        try (InputStream is = new ByteArrayInputStream(content); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                return 1;
            }
            String row0Col0 = cellText(sheet, 0, 0);
            String row0Col11 = cellText(sheet, 0, 11);
            String row1Col0 = cellText(sheet, 1, 0);
            String row1Col11 = cellText(sheet, 1, 11);

            if (isHeaderCell(row1Col0) || isHeaderCell(row1Col11)) {
                return 2;
            }
            if (isHeaderCell(row0Col0) || isHeaderCell(row0Col11)) {
                return 1;
            }
            return 0;
        }
    }

    private static boolean isHeaderCell(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return value.contains("市/州")
                || value.contains("患者姓名")
                || value.contains("接触者姓名")
                || value.contains("接触者身份证号");
    }

    private static String cellText(Sheet sheet, int rowIndex, int colIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            return null;
        }
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            return null;
        }
        String text = FORMATTER.formatCellValue(cell);
        return text == null ? null : text.trim();
    }
}
