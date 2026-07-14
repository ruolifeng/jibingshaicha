package cn.luyou.controller;

import cn.luyou.constant.LatentImportHeaders;
import cn.luyou.constant.PatientManualImportHeaders;
import cn.luyou.utils.CloseContactCaseExcelExportSupport;
import cn.luyou.utils.KeyPopulationScreeningExcelExportSupport;
import cn.luyou.utils.SchoolScreeningExcelExportSupport;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据导入模板下载（生成带表头的空 Excel 模板）
 */
@Slf4j
@Tag(name = "模板下载")
@RestController
@RequestMapping("/template")
public class TemplateController {

    @Operation(summary = "下载数据导入模板（type: school/keyPopulation/regular/latent/patient/closeContactCase）")
    @GetMapping("/download")
    public void download(
            @RequestParam String type,
            HttpServletResponse response) throws IOException {

        if ("closeContactCase".equals(type)) {
            String fileName = "密接个案表模板";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            String encodedName = URLEncoder.encode(fileName + ".xlsx", StandardCharsets.UTF_8).replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);
            CloseContactCaseExcelExportSupport.writeTemplate(response.getOutputStream());
            log.info("[模板下载] type={} fileName={}", type, fileName);
            return;
        }

        if ("school".equals(type)) {
            String fileName = "学生筛查数据模板";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            String encodedName = URLEncoder.encode(fileName + ".xlsx", StandardCharsets.UTF_8).replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);
            SchoolScreeningExcelExportSupport.writeTemplate(response.getOutputStream());
            log.info("[模板下载] type={} fileName={}", type, fileName);
            return;
        }

        String fileName;
        List<List<String>> headers;

        switch (type) {
            case "keyPopulation" -> {
                fileName = "重点人群筛查数据模板";
                response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                response.setCharacterEncoding("UTF-8");
                String encodedKeyPop = URLEncoder.encode(fileName + ".xlsx", StandardCharsets.UTF_8).replace("+", "%20");
                response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedKeyPop);
                KeyPopulationScreeningExcelExportSupport.writeTemplate(response.getOutputStream());
                log.info("[模板下载] type={} fileName={}", type, fileName);
                return;
            }
            case "regular" -> {
                fileName = "疫情筛查数据模板";
                response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                response.setCharacterEncoding("UTF-8");
                String encodedRegular = URLEncoder.encode(fileName + ".xlsx", StandardCharsets.UTF_8).replace("+", "%20");
                response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedRegular);
                KeyPopulationScreeningExcelExportSupport.writeTemplate(response.getOutputStream());
                log.info("[模板下载] type={} fileName={}", type, fileName);
                return;
            }
            case "latent" -> {
                fileName = "潜伏感染者导入模板";
                headers = LatentImportHeaders.FIELDS.stream().map(List::of).toList();
            }
            case "patient" -> {
                fileName = "在管患者导入模板";
                headers = PatientManualImportHeaders.FIELDS.stream().map(List::of).toList();
            }
            default -> {
                response.sendError(400, "未知模板类型：" + type);
                return;
            }
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String encodedName = URLEncoder.encode(fileName + ".xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);

        String sheetName = "数据";
        EasyExcel.write(response.getOutputStream())
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .head(headers)
                .sheet(sheetName)
                .doWrite(new ArrayList<>());

        log.info("[模板下载] type={} fileName={}", type, fileName);
    }
}
