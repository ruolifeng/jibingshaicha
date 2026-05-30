package cn.luyou.controller;

import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.ScreeningSchool;
import cn.luyou.model.vo.QuestionnaireConfigVO;
import cn.luyou.service.QuestionnaireService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Tag(name = "筛查问卷")
@RestController
@RequestMapping("/questionnaire")
@RequiredArgsConstructor
public class QuestionnaireController {

    private final QuestionnaireService questionnaireService;

    @Operation(summary = "公开获取问卷配置")
    @GetMapping("/public/{code}")
    public ResultResponse<QuestionnaireConfigVO> publicConfig(@PathVariable String code) {
        return ResultRes.success(questionnaireService.getPublicConfig(code));
    }

    @Operation(summary = "公开提交问卷")
    @PostMapping("/public/{code}/submit")
    public ResultResponse<Void> publicSubmit(@PathVariable String code, @RequestBody Map<String, Object> formData) {
        questionnaireService.submit(code, formData);
        return ResultRes.success(null);
    }

    @Operation(summary = "获取问卷配置（管理端）")
    @GetMapping("/{code}/config")
    public ResultResponse<QuestionnaireConfigVO> getConfig(@PathVariable String code) {
        return ResultRes.success(questionnaireService.getConfig(code));
    }

    @Operation(summary = "更新问卷配置（管理端）")
    @PutMapping("/{code}/config")
    public ResultResponse<Void> updateConfig(@PathVariable String code, @RequestBody QuestionnaireConfigVO vo) {
        questionnaireService.updateConfig(code, vo);
        return ResultRes.success(null);
    }

    @Operation(summary = "切换问卷开关（管理端）")
    @PutMapping("/{code}/enabled")
    public ResultResponse<Void> updateEnabled(@PathVariable String code, @RequestBody Map<String, Boolean> body) {
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        questionnaireService.updateEnabled(code, enabled);
        return ResultRes.success(null);
    }

    @Operation(summary = "分页查询问卷提交记录")
    @GetMapping("/{code}/submissions")
    public ResultResponse<IPage<ScreeningSchool>> listSubmissions(
            @PathVariable String code,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber) {
        return ResultRes.success(questionnaireService.listSubmissions(code, page, size, name, idNumber));
    }

    @Operation(summary = "导出问卷提交记录")
    @GetMapping("/{code}/submissions/export")
    public void exportSubmissions(
            @PathVariable String code,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber,
            HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encodedName = URLEncoder.encode("问卷提交记录", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename=" + encodedName + ".xlsx");
        questionnaireService.exportSubmissions(code, name, idNumber, response.getOutputStream());
    }
}
