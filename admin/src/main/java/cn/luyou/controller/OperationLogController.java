package cn.luyou.controller;

import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.OperationLog;
import cn.luyou.service.OperationLogService;
import cn.luyou.service.UserService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 操作日志 Controller（V13）
 *
 * 接口列表：
 * <ul>
 *   <li>GET /operation-log/list   分页查询</li>
 *   <li>GET /operation-log/export 导出 Excel（带权限校验）</li>
 * </ul>
 */
@Slf4j
@Tag(name = "操作日志")
@RestController
@RequestMapping("/operation-log")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;
    private final UserService userService;

    @Operation(summary = "分页查询操作日志")
    @GetMapping("/list")
    public ResultResponse<IPage<OperationLog>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String opType,
            @RequestParam(required = false) String opModule,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        // 一/二级用户可查看，普通业务用户不可（由前端菜单权限控制）；
        // 服务端兜底：要求角色 <= 3（即 admin/一级/二级）。
        userService.checkPermission(3);
        return ResultRes.success(operationLogService.queryPage(
                page, size, opType, opModule, userName, keyword, startTime, endTime));
    }

    @Operation(summary = "导出操作日志")
    @GetMapping("/export")
    public void export(
            HttpServletResponse response,
            @RequestParam(required = false) String opType,
            @RequestParam(required = false) String opModule,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) throws IOException {
        // 导出需要超管或一级用户
        userService.checkPermission(2);
        String fileName = "操作日志_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-disposition", "attachment;filename*=UTF-8''" +
                URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        operationLogService.exportLogs(opType, opModule, userName, keyword, startTime, endTime,
                response.getOutputStream());
    }
}
