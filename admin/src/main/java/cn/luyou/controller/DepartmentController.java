package cn.luyou.controller;

import cn.luyou.common.annotation.OperationLog;
import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.Department;
import cn.luyou.model.ImportResult;
import cn.luyou.service.DepartmentService;
import cn.luyou.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "部门管理")
@RestController
@RequestMapping("/department")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;
    private final UserService userService;

    @Operation(summary = "部门列表")
    @GetMapping("/list")
    public ResultResponse<List<Department>> list() {
        return ResultRes.success(departmentService.listAll());
    }

    @Operation(summary = "创建部门")
    @PostMapping("/create")
    @OperationLog(type = "create", module = "system", action = "创建部门")
    public ResultResponse<Void> create(@RequestBody Department department) {
        userService.checkPermissionCode("system:department");
        departmentService.createDepartment(department);
        return ResultRes.success(null);
    }

    @Operation(summary = "批量导入部门")
    @PostMapping("/import")
    @OperationLog(type = "import", module = "system", action = "批量导入部门")
    public ResultResponse<ImportResult> importDepartments(@RequestParam("file") MultipartFile file) {
        userService.checkPermissionCode("system:department");
        return ResultRes.success(departmentService.importDepartments(file));
    }

    @Operation(summary = "更新部门")
    @PutMapping("/update")
    @OperationLog(type = "update", module = "system", action = "更新部门")
    public ResultResponse<Void> update(@RequestBody Department department) {
        userService.checkPermissionCode("system:department");
        departmentService.updateDepartment(department);
        return ResultRes.success(null);
    }

    @Operation(summary = "删除部门")
    @DeleteMapping("/delete/{id}")
    @OperationLog(type = "delete", module = "system", action = "删除部门")
    public ResultResponse<Void> delete(@PathVariable Long id) {
        userService.checkPermissionCode("system:department");
        departmentService.deleteDepartment(id);
        return ResultRes.success(null);
    }
}
