package cn.luyou.utils;

import cn.luyou.constant.CloseContactCaseExcelHeaders;
import cn.luyou.model.CloseContactCase;
import cn.luyou.model.ScreeningCloseContact;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;

import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

/**
 * 密接个案表 / 密接筛查 — 导出含录入用户；导入模板仍为官方 72 列。
 */
public final class CloseContactCaseExcelExportSupport {

    public static final String SHEET_NAME = "密接个案表";

    private CloseContactCaseExcelExportSupport() {
    }

    public static <T> void write(OutputStream outputStream, String sheetName, Class<T> rowType, List<T> rows) {
        applyDerivedFields(rows);
        EasyExcel.write(outputStream, rowType)
                .head(CloseContactCaseExcelHeaders.asExportHead())
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .sheet(sheetName)
                .doWrite(rows);
    }

    /** 下载空模板（仅官方 72 列表头，不含录入用户） */
    public static void writeTemplate(OutputStream outputStream) {
        EasyExcel.write(outputStream, CloseContactCase.class)
                .head(CloseContactCaseExcelHeaders.asTemplateHead())
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .sheet(SHEET_NAME)
                .doWrite(Collections.emptyList());
    }

    private static <T> void applyDerivedFields(List<T> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Object first = rows.get(0);
        if (first instanceof CloseContactCase) {
            @SuppressWarnings("unchecked")
            List<CloseContactCase> cases = (List<CloseContactCase>) rows;
            CloseContactCaseExcelDerivedSupport.applyAll(cases);
        } else if (first instanceof ScreeningCloseContact) {
            @SuppressWarnings("unchecked")
            List<ScreeningCloseContact> contacts = (List<ScreeningCloseContact>) rows;
            CloseContactCaseExcelDerivedSupport.applyAllScreening(contacts);
        }
    }
}
