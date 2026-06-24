package cn.luyou.utils;

import cn.luyou.constant.CloseContactCaseExcelHeaders;
import cn.luyou.model.CloseContactCase;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;

import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

/**
 * 密接个案表 / 密接筛查 — 按官方 72 列模板导出（与导入模板表头一致，可再导入）。
 */
public final class CloseContactCaseExcelExportSupport {

    public static final String SHEET_NAME = "密接个案表";

    private CloseContactCaseExcelExportSupport() {
    }

    public static <T> void write(OutputStream outputStream, String sheetName, Class<T> rowType, List<T> rows) {
        EasyExcel.write(outputStream, rowType)
                .head(CloseContactCaseExcelHeaders.asEasyExcelHead())
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .sheet(sheetName)
                .doWrite(rows);
    }

    /** 下载空模板（仅官方 72 列表头，与导出/导入一致） */
    public static void writeTemplate(OutputStream outputStream) {
        write(outputStream, SHEET_NAME, CloseContactCase.class, Collections.emptyList());
    }
}
