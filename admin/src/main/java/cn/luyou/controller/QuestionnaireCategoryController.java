package cn.luyou.controller;

import cn.luyou.common.annotation.OperationLog;
import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.QuestionnaireCategory;
import cn.luyou.service.QuestionnaireCategoryService;
import cn.luyou.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "问卷分类")
@RestController
@RequestMapping("/questionnaire/category")
@RequiredArgsConstructor
public class QuestionnaireCategoryController {

    private final QuestionnaireCategoryService categoryService;
    private final UserService userService;

    @Operation(summary = "分类列表（创建问卷下拉等场景均可调用）")
    @GetMapping("/list")
    public ResultResponse<List<QuestionnaireCategory>> list() {
        return ResultRes.success(categoryService.listAll());
    }

    @Operation(summary = "新增分类")
    @PostMapping
    @OperationLog(type = "create", module = "questionnaire", action = "创建问卷分类")
    public ResultResponse<QuestionnaireCategory> create(@RequestBody QuestionnaireCategory category) {
        userService.checkPermissionCode("questionnaire:category");
        return ResultRes.success(categoryService.create(category));
    }

    @Operation(summary = "更新分类")
    @PutMapping("/{id}")
    @OperationLog(type = "update", module = "questionnaire", action = "更新问卷分类")
    public ResultResponse<QuestionnaireCategory> update(
            @PathVariable Long id, @RequestBody QuestionnaireCategory category) {
        userService.checkPermissionCode("questionnaire:category");
        return ResultRes.success(categoryService.update(id, category));
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    @OperationLog(type = "delete", module = "questionnaire", action = "删除问卷分类")
    public ResultResponse<Void> delete(@PathVariable Long id) {
        userService.checkPermissionCode("questionnaire:category");
        categoryService.delete(id);
        return ResultRes.success(null);
    }
}
