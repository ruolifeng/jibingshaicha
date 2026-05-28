package cn.luyou.controller;

import cn.luyou.constant.CloseContactCaseExcelHeaders;
import cn.luyou.constant.LatentImportHeaders;
import cn.luyou.constant.PatientManualImportHeaders;
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

    /** 学校人群筛查模板列头 */
    private static final List<List<String>> SCHOOL_HEADERS = List.of(
            List.of("序号"), List.of("年份"), List.of("市（州）"), List.of("县（市、区）"),
            List.of("姓名"), List.of("性别"), List.of("出生日期"), List.of("年龄"),
            List.of("证件类型"), List.of("证件号"), List.of("民族"), List.of("联系电话"),
            List.of("户籍所在地（XX市XX县、区）"), List.of("现地址"),
            List.of("学校类型"), List.of("学校名称"), List.of("班级（院系）"),
            List.of("既往结核病史"), List.of("密切接触史"), List.of("结核病可疑症状"),
            List.of("是否进行感染筛"), List.of("感染筛查日期"),
            List.of("方法"), List.of("结果（PPD：mmXmm；EC及IGRA：阳性/阴性）"),
            List.of("感染筛查结果（学校人群感染筛查情况）"),
            List.of("是否进行胸片检查"), List.of("胸片检查日期"), List.of("胸片结果"),
            List.of("痰涂片"), List.of("分子生物学"),
            List.of("诊断结果"),
            List.of("符合潜伏治疗条件者是否进行预防性治疗（是：写出方案；否：填写原因）"),
            List.of("备注")
    );

    /** 重点人群/疫情筛查模板列头（两者结构相同） */
    private static final List<List<String>> KEY_POPULATION_HEADERS = List.of(
            List.of("序号"), List.of("年份"), List.of("市（州）"), List.of("县（市、区）"),
            List.of("姓名"), List.of("性别"), List.of("出生日期"), List.of("年龄"),
            List.of("证件类型"), List.of("证件号"), List.of("民族"), List.of("联系电话"),
            List.of("户籍所在地（XX市XX县、区）"), List.of("现住址"),
            List.of("密接"), List.of("学生"), List.of("教职工"), List.of("老年人"),
            List.of("糖尿病"), List.of("双感"), List.of("既往结核史"), List.of("非重点人群"),
            List.of("是否有可疑症状"), List.of("咳嗽咳痰"), List.of("咯血或血痰"),
            List.of("发热"), List.of("胸痛"), List.of("夜间盗汗"), List.of("食欲不振"),
            List.of("乏力"), List.of("体重减轻"),
            List.of("是否进行感染筛"), List.of("感染筛查日期"),
            List.of("方法（PPD）"), List.of("方法（EC/IGRA）"),
            List.of("结果（PPD：mmXmm；EC及IGRA：阳性/阴性）"), List.of("感染筛查结果"),
            List.of("是否进行胸片检查"), List.of("胸片检查日期"), List.of("胸片结果"),
            List.of("结果判定"),
            List.of("是否转诊到定点医疗机构"), List.of("诊断结果"),
            List.of("是否符合预防性治疗"), List.of("是否进行预防性治疗"),
            List.of("是否规范完成预防性治疗"), List.of("其他情况说明（备注）")
    );

    @Operation(summary = "下载数据导入模板（type: school/keyPopulation/regular/latent/patient/closeContactCase）")
    @GetMapping("/download")
    public void download(
            @RequestParam String type,
            HttpServletResponse response) throws IOException {

        String fileName;
        List<List<String>> headers;

        switch (type) {
            case "school" -> {
                fileName = "学生筛查数据模板";
                headers = SCHOOL_HEADERS;
            }
            case "keyPopulation" -> {
                fileName = "重点人群筛查数据模板";
                headers = KEY_POPULATION_HEADERS;
            }
            case "regular" -> {
                fileName = "疫情筛查数据模板";
                headers = KEY_POPULATION_HEADERS;
            }
            case "latent" -> {
                fileName = "潜伏感染者导入模板";
                headers = LatentImportHeaders.FIELDS.stream().map(List::of).toList();
            }
            case "patient" -> {
                fileName = "在管患者导入模板";
                headers = PatientManualImportHeaders.FIELDS.stream().map(List::of).toList();
            }
            case "closeContactCase" -> {
                fileName = "密接个案表";
                headers = CloseContactCaseExcelHeaders.asEasyExcelHead();
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

        String sheetName = "closeContactCase".equals(type) ? "密接个案表" : "数据";
        EasyExcel.write(response.getOutputStream())
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .head(headers)
                .sheet(sheetName)
                .doWrite(new ArrayList<>());

        log.info("[模板下载] type={} fileName={}", type, fileName);
    }
}
