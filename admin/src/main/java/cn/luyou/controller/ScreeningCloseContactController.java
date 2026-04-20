package cn.luyou.controller;

import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.ImportResult;
import cn.luyou.model.ScreeningCloseContact;
import cn.luyou.service.ScreeningCloseContactService;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "密接人群筛查管理")
@RestController
@RequestMapping("/screening/close-contact")
@RequiredArgsConstructor
public class ScreeningCloseContactController {

    private final ScreeningCloseContactService screeningCloseContactService;

    @Operation(summary = "上传密接人群筛查Excel")
    @PostMapping("/upload")
    public ResultResponse<ImportResult> upload(@RequestParam("file") MultipartFile file) {
        ImportResult result = screeningCloseContactService.uploadAndParse(file);
        return ResultRes.success(result);
    }

    @Operation(summary = "分页查询密接人群筛查数据")
    @GetMapping("/list")
    public ResultResponse<IPage<ScreeningCloseContact>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) Integer isLatent) {
        return ResultRes.success(screeningCloseContactService.queryPage(page, size, name, idNumber, district, isLatent));
    }

    @Operation(summary = "新增密接人群筛查记录")
    @PostMapping("/create")
    public ResultResponse<Void> create(@RequestBody ScreeningCloseContact data) {
        screeningCloseContactService.createScreening(data);
        return ResultRes.success(null);
    }

    @Operation(summary = "更新密接人群筛查记录")
    @PutMapping("/update/{id}")
    public ResultResponse<Void> update(@PathVariable Long id, @RequestBody ScreeningCloseContact data) {
        data.setId(id);
        screeningCloseContactService.updateScreening(data);
        return ResultRes.success(null);
    }

    @Operation(summary = "删除密接人群筛查记录（级联删除后续所有关联数据）")
    @DeleteMapping("/delete/{id}")
    public ResultResponse<Void> delete(@PathVariable Long id) {
        screeningCloseContactService.deleteScreeningCascade(id);
        return ResultRes.success(null);
    }

    @Operation(summary = "导出密接人群筛查数据")
    @GetMapping("/export")
    public void export(
            HttpServletResponse response,
            @RequestParam(required = false) String ids) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" +
                URLEncoder.encode("密接人群筛查数据.xlsx", StandardCharsets.UTF_8));
        var query = Wrappers.<ScreeningCloseContact>lambdaQuery();
        if (ids != null && !ids.isBlank()) {
            List<Long> idList = Arrays.stream(ids.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty() && s.matches("\\d+"))
                    .map(Long::valueOf)
                    .toList();
            if (!idList.isEmpty()) {
                query.in(ScreeningCloseContact::getId, idList);
            }
        }
        query.orderByDesc(ScreeningCloseContact::getCreateTime);
        List<ScreeningCloseContact> list = screeningCloseContactService.list(query);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ScreeningCloseContact s : list) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("年份", s.getYear());
            row.put("市（州）", s.getCity());
            row.put("县（市、区）", s.getDistrict());
            row.put("姓名", s.getName());
            row.put("性别", s.getGender());
            row.put("出生日期", s.getBirthDate());
            row.put("年龄", s.getAge());
            row.put("证件类型", s.getIdType());
            row.put("证件号", s.getIdNumber());
            row.put("民族", s.getEthnicity());
            row.put("职业", s.getOccupation());
            row.put("联系电话", s.getPhone());
            row.put("户籍所在地", s.getHouseholdAddress());
            row.put("现住址", s.getCurrentAddress());
            row.put("接触类型", toCnContactType(s.getContactType()));
            row.put("原患者姓名", s.getSourcePatientName());
            row.put("原患者确诊日期", s.getSourcePatientConfirmDate());
            row.put("原患者身份证号", s.getSourcePatientIdNumber());

            row.put("首次筛查日期", s.getFirstScreenDate());
            row.put("首次症状筛查结果", s.getFirstSymptomResult());
            row.put("首次感染检查方法", s.getFirstInfectionMethod());
            row.put("首次感染检查结果", s.getFirstScreenResult());
            row.put("首次感染筛查结果", s.getFirstInfectionResult());
            row.put("首次是否进行胸片", toCnYesNo(s.getFirstHasChestXray()));
            row.put("首次胸片日期", s.getFirstChestXrayDate());
            row.put("首次胸片结果", s.getFirstChestXrayResult());
            row.put("首次诊断结果", s.getFirstDiagnosis());

            row.put("半年后筛查日期", s.getHalfYearScreenDate());
            row.put("半年后症状筛查结果", s.getHalfYearSymptomResult());
            row.put("半年后感染检查方法", s.getHalfYearInfectionMethod());
            row.put("半年后感染检查结果", s.getHalfYearScreenResult());
            row.put("半年后感染筛查结果", s.getHalfYearInfectionResult());
            row.put("半年后是否进行胸片", toCnYesNo(s.getHalfYearHasChestXray()));
            row.put("半年后胸片日期", s.getHalfYearChestXrayDate());
            row.put("半年后胸片结果", s.getHalfYearChestXrayResult());
            row.put("半年后诊断结果", s.getHalfYearDiagnosis());

            row.put("一年后筛查日期", s.getOneYearScreenDate());
            row.put("一年后症状筛查结果", s.getOneYearSymptomResult());
            row.put("一年后感染检查方法", s.getOneYearInfectionMethod());
            row.put("一年后感染检查结果", s.getOneYearScreenResult());
            row.put("一年后感染筛查结果", s.getOneYearInfectionResult());
            row.put("一年后是否进行胸片", toCnYesNo(s.getOneYearHasChestXray()));
            row.put("一年后胸片日期", s.getOneYearChestXrayDate());
            row.put("一年后胸片结果", s.getOneYearChestXrayResult());
            row.put("一年后诊断结果", s.getOneYearDiagnosis());

            row.put("是否进行预防性治疗", toCnYesNo(s.getHasPreventiveTreatment()));
            row.put("预防性治疗方案", s.getPreventivePlan());
            row.put("预防性治疗开始时间", s.getPreventiveStartDate());
            row.put("预防性治疗完成时间", s.getPreventiveEndDate());
            row.put("预防性治疗结果", s.getPreventiveResult());
            row.put("预防性治疗期间随访管理人员", s.getPreventiveManager());
            row.put("惠民方式", s.getBenefitMethod());
            row.put("备注", s.getRemark());

            row.put("疑似结核判定", Integer.valueOf(1).equals(s.getIsLatent()) ? "疑似结核" : "正常");
            row.put("阳性轮次", toCnRound(s.getActiveRound()));
            rows.add(row);
        }

        if (rows.isEmpty()) {
            EasyExcel.write(response.getOutputStream()).sheet("筛查数据").doWrite(new ArrayList<>());
            return;
        }
        List<List<String>> heads = rows.get(0).keySet().stream()
                .map(k -> List.of(k))
                .collect(Collectors.toList());
        List<List<Object>> data = rows.stream()
                .map(r -> new ArrayList<Object>(r.values()))
                .collect(Collectors.toList());
        EasyExcel.write(response.getOutputStream())
                .head(heads)
                .sheet("筛查数据")
                .doWrite(data);
    }

    private String toCnContactType(String contactType) {
        if (contactType == null) return "";
        return switch (contactType) {
            case "家庭内", "family", "inside", "home" -> "家庭内";
            case "家庭外", "outside", "outer", "external" -> "家庭外";
            default -> contactType;
        };
    }

    private String toCnYesNo(String value) {
        if (value == null) return "";
        String v = value.trim();
        return switch (v.toLowerCase()) {
            case "yes", "y", "true", "1", "是" -> "是";
            case "no", "n", "false", "0", "否" -> "否";
            default -> value;
        };
    }

    private String toCnRound(Integer round) {
        if (round == null) return "";
        return switch (round) {
            case 1 -> "首次";
            case 2 -> "半年后";
            case 3 -> "一年后";
            default -> "";
        };
    }
}
