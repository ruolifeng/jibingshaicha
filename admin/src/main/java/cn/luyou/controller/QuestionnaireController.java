package cn.luyou.controller;

import cn.luyou.common.annotation.OperationLog;
import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.Question;
import cn.luyou.model.Questionnaire;
import cn.luyou.service.QuestionnaireService;
import cn.luyou.service.UserService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "问卷管理")
@RestController
@RequestMapping("/questionnaire")
@RequiredArgsConstructor
public class QuestionnaireController {

    private final QuestionnaireService questionnaireService;
    private final UserService userService;

    @Operation(summary = "问卷分页列表")
    @GetMapping("/page")
    public ResultResponse<Page<Questionnaire>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        userService.checkPermissionCode("questionnaire:list");
        return ResultRes.success(questionnaireService.page(pageNum, pageSize, keyword, status));
    }

    @Operation(summary = "问卷详情")
    @GetMapping("/{id}")
    public ResultResponse<Questionnaire> getById(@PathVariable Long id) {
        return ResultRes.success(questionnaireService.getById(id));
    }

    @Operation(summary = "创建问卷")
    @PostMapping
    @OperationLog(type = "create", module = "questionnaire", action = "创建问卷")
    public ResultResponse<Questionnaire> create(@RequestBody Questionnaire q) {
        userService.checkPermissionCode("questionnaire:create");
        return ResultRes.success(questionnaireService.create(q));
    }

    @Operation(summary = "更新问卷")
    @PutMapping("/{id}")
    @OperationLog(type = "update", module = "questionnaire", action = "更新问卷")
    public ResultResponse<Questionnaire> update(@PathVariable Long id, @RequestBody Questionnaire q) {
        userService.checkPermissionCode("questionnaire:update");
        return ResultRes.success(questionnaireService.update(id, q));
    }

    @Operation(summary = "删除问卷")
    @DeleteMapping("/{id}")
    @OperationLog(type = "delete", module = "questionnaire", action = "删除问卷")
    public ResultResponse<Void> delete(@PathVariable Long id) {
        userService.checkPermissionCode("questionnaire:delete");
        questionnaireService.delete(id);
        return ResultRes.success(null);
    }

    @Operation(summary = "更新问卷状态（发布/暂停/关闭）")
    @PutMapping("/{id}/status")
    @OperationLog(type = "update", module = "questionnaire", action = "更新问卷状态")
    public ResultResponse<Void> updateStatus(@PathVariable Long id, @RequestParam int status) {
        userService.checkPermissionCode("questionnaire:publish");
        questionnaireService.updateStatus(id, status);
        return ResultRes.success(null);
    }

    @Operation(summary = "获取问卷题目列表")
    @GetMapping("/{id}/questions")
    public ResultResponse<List<Question>> listQuestions(@PathVariable Long id) {
        return ResultRes.success(questionnaireService.listQuestions(id));
    }

    @Operation(summary = "批量保存题目（全量覆盖）")
    @PostMapping("/{id}/questions")
    @OperationLog(type = "update", module = "questionnaire", action = "保存问卷题目")
    public ResultResponse<Void> saveQuestions(@PathVariable Long id, @RequestBody List<Question> questions) {
        userService.checkPermissionCode("questionnaire:update");
        questionnaireService.saveQuestions(id, questions);
        return ResultRes.success(null);
    }

    @Operation(summary = "问卷模板列表")
    @GetMapping("/template/list")
    public ResultResponse<Page<Questionnaire>> templateList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "50") int pageSize,
            @RequestParam(required = false) String templateType) {
        userService.checkPermissionCode("questionnaire:template:view");
        return ResultRes.success(questionnaireService.templateList(pageNum, pageSize, templateType));
    }

    @Operation(summary = "保存为模板")
    @PostMapping("/{id}/save-as-template")
    @OperationLog(type = "create", module = "questionnaire", action = "保存为模板")
    public ResultResponse<Questionnaire> saveAsTemplate(
            @PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        userService.checkPermissionCode("questionnaire:create");
        String title = body != null ? body.get("title") : null;
        String templateType = body != null ? body.get("templateType") : null;
        return ResultRes.success(questionnaireService.saveAsTemplate(id, title, templateType));
    }

    @Operation(summary = "从模板创建问卷")
    @PostMapping("/create-from-template")
    @OperationLog(type = "create", module = "questionnaire", action = "从模板创建问卷")
    public ResultResponse<Questionnaire> createFromTemplate(@RequestBody Map<String, Object> body) {
        userService.checkPermissionCode("questionnaire:create");
        Long templateId = Long.valueOf(body.get("templateId").toString());
        String title = body.get("title") != null ? body.get("title").toString() : null;
        return ResultRes.success(questionnaireService.createFromTemplate(templateId, title));
    }

    @Operation(summary = "删除模板")
    @DeleteMapping("/template/{id}")
    @OperationLog(type = "delete", module = "questionnaire", action = "删除模板")
    public ResultResponse<Void> deleteTemplate(@PathVariable Long id) {
        userService.checkPermissionCode("questionnaire:template:delete");
        questionnaireService.deleteTemplate(id);
        return ResultRes.success(null);
    }
}
