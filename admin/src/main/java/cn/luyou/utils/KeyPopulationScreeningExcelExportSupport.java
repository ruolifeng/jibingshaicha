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
 * 重点人群 / 疫情筛查 — 按官方 49 列四级表头模板导出（与导入模板一致，可再导入）。
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
                .head(KeyPopulationScreeningExcelHeaders.asEasyExcelHead())
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .sheet(KeyPopulationScreeningExcelHeaders.SHEET_NAME)
                .doWrite(rows);
    }

    public static void writeTemplate(OutputStream outputStream) {
        EasyExcel.write(outputStream, KeyPopulationScreeningExcelExportRow.class)
                .head(KeyPopulationScreeningExcelHeaders.asEasyExcelHead())
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .sheet(KeyPopulationScreeningExcelHeaders.SHEET_NAME)
                .doWrite(Collections.emptyList());
    }
}
