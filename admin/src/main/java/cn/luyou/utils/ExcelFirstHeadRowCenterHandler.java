package cn.luyou.utils;

import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.handler.context.CellWriteHandlerContext;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;

/**
 * 将 Excel 第一行表头（合并分组标题）水平、垂直居中。
 */
public class ExcelFirstHeadRowCenterHandler implements CellWriteHandler {

    @Override
    public void afterCellDispose(CellWriteHandlerContext context) {
        if (!Boolean.TRUE.equals(context.getHead())) {
            return;
        }
        Integer rowIndex = context.getRowIndex();
        if (rowIndex == null || rowIndex != 0) {
            return;
        }
        WriteCellData<?> cellData = context.getFirstCellData();
        if (cellData == null) {
            return;
        }
        WriteCellStyle style = cellData.getOrCreateStyle();
        style.setHorizontalAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
    }
}
