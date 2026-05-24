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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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

    @Operation(summary = "上传密接人群筛查Excel（新模板73列）")
    @PostMapping("/upload")
    public ResultResponse<ImportResult> upload(@RequestParam("file") MultipartFile file) {
        return ResultRes.success(screeningCloseContactService.uploadAndParse(file));
    }

    @Operation(summary = "分页查询密接人群筛查数据")
    @GetMapping("/list")
    public ResultResponse<IPage<ScreeningCloseContact>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) Integer ccStatus,
            @RequestParam(required = false) String finalScreeningResult) {
        return ResultRes.success(screeningCloseContactService.queryPage(
                page, size, name, idNumber, district, ccStatus, finalScreeningResult));
    }

    @Operation(summary = "各最终筛查结果分类统计")
    @GetMapping("/count-by-result")
    public ResultResponse<Map<String, Long>> countByResult() {
        return ResultRes.success(screeningCloseContactService.countByFinalResult());
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

    @Operation(summary = "删除密接人群筛查记录（级联删除）")
    @DeleteMapping("/delete/{id}")
    public ResultResponse<Void> delete(@PathVariable Long id) {
        screeningCloseContactService.deleteScreeningCascade(id);
        return ResultRes.success(null);
    }

    @Operation(summary = "批量删除密接人群筛查记录（级联删除）")
    @DeleteMapping("/batch-delete")
    public ResultResponse<Void> batchDelete(@RequestBody List<Long> ids) {
        if (ids != null) ids.forEach(screeningCloseContactService::deleteScreeningCascade);
        return ResultRes.success(null);
    }

    @Operation(summary = "按ID查询密接人群筛查记录详情")
    @GetMapping("/{id}")
    public ResultResponse<ScreeningCloseContact> detail(@PathVariable Long id) {
        return ResultRes.success(screeningCloseContactService.getEnrichedById(id));
    }

    // ==================== 密接专属业务接口 ====================

    @Operation(summary = "设置预计完成治疗时间（潜伏感染者-预防治疗）")
    @PostMapping("/{id}/expected-end-date")
    public ResultResponse<Void> setExpectedEndDate(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate expectedDate) {
        screeningCloseContactService.setExpectedTreatmentEndDate(id, expectedDate);
        return ResultRes.success(null);
    }

    @Operation(summary = "确认治疗是否完成（到预计完成时间后操作）")
    @PostMapping("/{id}/confirm-treatment")
    public ResultResponse<Void> confirmTreatment(
            @PathVariable Long id,
            @RequestParam boolean done) {
        screeningCloseContactService.confirmTreatmentDone(id, done);
        return ResultRes.success(null);
    }

    @Operation(summary = "提交3月复查结果（未发现异常流程）")
    @PostMapping("/{id}/three-month-check")
    public ResultResponse<Void> threeMonthCheck(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate checkDate,
            @RequestParam String checkResult,
            @RequestParam String finalResult) {
        screeningCloseContactService.submitThreeMonthCheck(id, checkDate, checkResult, finalResult);
        return ResultRes.success(null);
    }

    // ==================== 导出 ====================

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
            if (!idList.isEmpty()) query.in(ScreeningCloseContact::getId, idList);
        }
        query.orderByDesc(ScreeningCloseContact::getCreateTime);
        List<ScreeningCloseContact> list = screeningCloseContactService.list(query);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (ScreeningCloseContact s : list) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("市/州", s.getCity());
            row.put("区/县", s.getDistrict());
            row.put("原患者姓名", s.getSourcePatientName());
            row.put("原患者病案号", s.getSourcePatientCaseNo());
            row.put("原患者身份证号", s.getSourcePatientIdNumber());
            row.put("接触者姓名", s.getName());
            row.put("接触者身份证号", s.getIdNumber());
            row.put("年龄", s.getAge());
            row.put("接触者电话", s.getPhone());
            row.put("接触类型", s.getContactType());
            row.put("接触场所", s.getContactPlace());
            row.put("密接登记日期", s.getRegistrationDate());
            row.put("首次筛查日期", s.getFirstScreenDate());
            row.put("感染检测方法", s.getInfectionCheckMethod());
            row.put("感染检测结果", s.getInfectionCheckResult());
            row.put("影像方法", s.getImagingMethod());
            row.put("影像结果", s.getImagingResult());
            row.put("痰检方法", s.getSputumCheckMethod());
            row.put("痰检结果", s.getSputumCheckResult());
            row.put("最终筛查结果", s.getFinalScreeningResult());
            row.put("是否开展预防治疗", s.getHasPreventiveTreatment());
            row.put("预防性治疗方案", s.getPreventivePlan());
            row.put("是否完成治疗", s.getTreatmentCompleted());
            row.put("6月随访结果", s.getFollowup6Result());
            row.put("12月随访结果", s.getFollowup12Result());
            row.put("24月随访结果", s.getFollowup24Result());
            row.put("流程状态", toCnStatus(s.getCcStatus()));
            row.put("备注", s.getRemark());
            rows.add(row);
        }

        if (rows.isEmpty()) {
            EasyExcel.write(response.getOutputStream()).sheet("筛查数据").doWrite(new ArrayList<>());
            return;
        }
        List<List<String>> heads = rows.get(0).keySet().stream().map(List::of).collect(Collectors.toList());
        List<List<Object>> data = rows.stream()
                .map(r -> new ArrayList<Object>(r.values())).collect(Collectors.toList());
        EasyExcel.write(response.getOutputStream()).head(heads).sheet("筛查数据").doWrite(data);
    }

    private String toCnStatus(Integer status) {
        if (status == null) return "待处理";
        return switch (status) {
            case 1 -> "活动性肺结核-结案";
            case 2 -> "潜伏感染者-管理中";
            case 3 -> "潜伏感染者-已归档";
            case 4 -> "随访监测中";
            case 5 -> "随访监测-已归档";
            case 6 -> "未发现异常-待3月复查";
            case 7 -> "3月复查阴性-已结束";
            case 8 -> "3月复查阳性-转潜伏流程";
            default -> "待处理";
        };
    }
}
