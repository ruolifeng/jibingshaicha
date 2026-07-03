package cn.luyou.utils;

import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.handler.context.CellWriteHandlerContext;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * 多级表头 Excel 导出：首行分组标题水平/垂直居中。
 */
public class ExcelFirstHeadRowCenterHandler implements CellWriteHandler {

    @Override
    public void afterCellDispose(CellWriteHandlerContext context) {
        if (!Boolean.TRUE.equals(context.getHead())) {
            return;
        }
        Cell cell = context.getCell();
        if (cell == null || cell.getRowIndex() != 0) {
            return;
        }
        Workbook workbook = context.getWriteSheetHolder().getSheet().getWorkbook();
        CellStyle style = workbook.createCellStyle();
        if (cell.getCellStyle() != null) {
            style.cloneStyleFrom(cell.getCellStyle());
        }
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        cell.setCellStyle(style);
    }
}
