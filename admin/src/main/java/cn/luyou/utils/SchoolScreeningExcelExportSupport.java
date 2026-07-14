package cn.luyou.utils;

import cn.luyou.constant.SchoolScreeningExcelHeaders;
import cn.luyou.model.SchoolScreeningExcelExportRow;
import cn.luyou.model.ScreeningSchool;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 学生/学校人群筛查 — 按系统一致的二级表头导出（含序号、录入用户、预防治疗列）。
 */
public final class SchoolScreeningExcelExportSupport {

    private SchoolScreeningExcelExportSupport() {
    }

    public static void write(OutputStream outputStream, List<ScreeningSchool> records) {
        List<SchoolScreeningExcelExportRow> rows = new ArrayList<>(records == null ? 0 : records.size());
        int seq = 1;
        if (records != null) {
            for (ScreeningSchool record : records) {
                rows.add(SchoolScreeningExcelExportRow.from(record, seq++));
            }
        }
        EasyExcel.write(outputStream, SchoolScreeningExcelExportRow.class)
                .head(SchoolScreeningExcelHeaders.asExportHead())
                .registerWriteHandler(new ExcelFirstHeadRowCenterHandler())
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .sheet(SchoolScreeningExcelHeaders.SHEET_NAME)
                .doWrite(rows);
    }

    public static void writeTemplate(OutputStream outputStream) {
        EasyExcel.write(outputStream)
                .head(SchoolScreeningExcelHeaders.asTemplateHead())
                .registerWriteHandler(new ExcelFirstHeadRowCenterHandler())
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .sheet(SchoolScreeningExcelHeaders.SHEET_NAME)
                .doWrite(Collections.emptyList());
    }
}
