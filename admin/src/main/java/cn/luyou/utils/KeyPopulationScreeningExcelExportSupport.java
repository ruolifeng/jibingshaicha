package cn.luyou.utils;

import cn.luyou.constant.KeyPopulationScreeningExcelHeaders;
import cn.luyou.model.KeyPopulationScreeningExcelExportRow;
import cn.luyou.model.ScreeningKeyPopulation;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 重点人群 / 疫情筛查 — 导出含录入用户/录入时间；导入模板仍为官方 49 列。
 */
public final class KeyPopulationScreeningExcelExportSupport {

    private KeyPopulationScreeningExcelExportSupport() {
    }

    public static void write(OutputStream outputStream, List<ScreeningKeyPopulation> records) {
        List<KeyPopulationScreeningExcelExportRow> rows = new ArrayList<>();
        int seq = 1;
        for (ScreeningKeyPopulation record : records) {
            rows.add(KeyPopulationScreeningExcelExportRow.from(record, seq++));
        }
        EasyExcel.write(outputStream, KeyPopulationScreeningExcelExportRow.class)
                .head(KeyPopulationScreeningExcelHeaders.asExportHead())
                .registerWriteHandler(new ExcelFirstHeadRowCenterHandler())
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .sheet(KeyPopulationScreeningExcelHeaders.SHEET_NAME)
                .doWrite(rows);
    }

    public static void writeTemplate(OutputStream outputStream) {
        EasyExcel.write(outputStream)
                .head(KeyPopulationScreeningExcelHeaders.asTemplateHead())
                .registerWriteHandler(new ExcelFirstHeadRowCenterHandler())
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .sheet(KeyPopulationScreeningExcelHeaders.SHEET_NAME)
                .doWrite(Collections.emptyList());
    }
}
