package cn.luyou.controller;

import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.Department;
import cn.luyou.service.DepartmentService;
import cn.luyou.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
    public ResultResponse<Void> create(@RequestBody Department department) {
        userService.checkPermission(1);
        departmentService.createDepartment(department);
        return ResultRes.success(null);
    }

    @Operation(summary = "更新部门")
    @PutMapping("/update")
    public ResultResponse<Void> update(@RequestBody Department department) {
        userService.checkPermission(1);
        departmentService.updateDepartment(department);
        return ResultRes.success(null);
    }

    @Operation(summary = "删除部门")
    @DeleteMapping("/delete/{id}")
    public ResultResponse<Void> delete(@PathVariable Long id) {
        userService.checkPermission(1);
        departmentService.deleteDepartment(id);
        return ResultRes.success(null);
    }
}
