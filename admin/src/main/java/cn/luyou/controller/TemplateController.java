package cn.luyou.controller;

import cn.luyou.constant.LatentImportHeaders;
import cn.luyou.constant.PatientManualImportHeaders;
import cn.luyou.utils.CloseContactCaseExcelExportSupport;
import cn.luyou.utils.KeyPopulationScreeningExcelExportSupport;
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
import java.util.Arrays;
import java.util.List;

/**
 * 数据导入模板下载（生成带表头的空 Excel 模板）
 */
@Slf4j
@Tag(name = "模板下载")
@RestController
@RequestMapping("/template")
public class TemplateController {

    /** EasyExcel 多级表头需可变 List，不可使用 List.of 多元素不可变列表 */
    private static List<String> head(String... parts) {
        return new ArrayList<>(Arrays.asList(parts));
    }

    /** 学校人群筛查模板列头 */
    private static final List<List<String>> SCHOOL_HEADERS = List.of(
            head("序号"), head("年份"), head("市（州）"), head("县（市、区）"),
            head("姓名"), head("性别"), head("出生日期"), head("年龄"),
            head("证件类型"), head("证件号"), head("民族"), head("联系电话"),
            head("户籍所在地（XX市XX县、区）"), head("现地址"),
            head("学校类型"), head("学校名称"), head("班级（院系）"),
            head("既往结核病史"), head("密切接触史"), head("结核病可疑症状"),
            head("学校人群感染筛查情况", "是否进行感染筛"),
            head("学校人群感染筛查情况", "感染筛查日期"),
            head("学校人群感染筛查情况", "方法"),
            head("学校人群感染筛查情况", "结果（PPD：mmXmm；EC及IGRA：阳性/阴性）"),
            head("学校人群感染筛查情况", "感染筛查结果"),
            head("学校人群胸片检查", "是否进行胸片检查"),
            head("学校人群胸片检查", "胸片检查日期"),
            head("学校人群胸片检查", "胸片结果"),
            head("痰涂片结果"), head("分子生物学结果"), head("诊断结果"),
            head("潜伏感染者管理情况", "是否进行预防者治疗"),
            head("潜伏感染者管理情况", "预防性治疗方案"),
            head("潜伏感染者管理情况", "预防性治疗开始时间（年月日）"),
            head("潜伏感染者管理情况", "预防性治疗完成时间（年月日）"),
            head("潜伏感染者管理情况", "预防性治疗结果"),
            head("潜伏感染者管理情况", "预防性治疗期间随访管理人员")
    );

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

        String fileName;
        List<List<String>> headers;

        switch (type) {
            case "school" -> {
                fileName = "学生筛查数据模板";
                headers = SCHOOL_HEADERS;
            }
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
