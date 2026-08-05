package cn.luyou.controller;

import cn.luyou.common.annotation.OperationLog;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import cn.luyou.model.QuestionnaireAnswer;
import cn.luyou.model.QuestionnaireResponse;
import cn.luyou.service.QuestionnaireResponseService;
import cn.luyou.service.UserService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "问卷回收与填写")
@RestController
@RequiredArgsConstructor
public class QuestionnaireResponseController {

    private final QuestionnaireResponseService responseService;
    private final UserService userService;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Operation(summary = "问卷填写文件上传")
    @PostMapping("/public/fill/upload")
    public ResultResponse<String> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "文件不能为空");
        }
        if (file.getSize() > 50L * 1024 * 1024) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "文件大小超过限制（50MB）");
        }
        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String originalName = file.getOriginalFilename();
            String ext = (originalName != null && originalName.contains("."))
                    ? originalName.substring(originalName.lastIndexOf("."))
                    : "";
            String savedName = UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = dir.resolve(savedName).normalize();
            if (!target.startsWith(dir)) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "非法文件名");
            }
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            String encodedName = URLEncoder.encode(
                    originalName != null ? originalName : savedName, StandardCharsets.UTF_8);
            return ResultRes.success("/file/serve/" + savedName + "?name=" + encodedName);
        } catch (ServiceException e) {
            throw e;
        } catch (IOException e) {
            throw new ServiceException(StatusEnum.SERVICE_ERROR, "文件保存失败");
        }
    }

    @Operation(summary = "生成单个二维码URL")
    @GetMapping("/questionnaire/{id}/qr-url")
    public ResultResponse<String> qrUrl(@PathVariable Long id) {
        return ResultRes.success(responseService.generateQrUrl(id));
    }

    @Operation(summary = "初始化填写")
    @GetMapping("/public/fill/{questionnaireId}")
    public ResultResponse<Map<String, Object>> initFill(@PathVariable Long questionnaireId) {
        return ResultRes.success(responseService.initFill(questionnaireId));
    }

    @Operation(summary = "提交答案")
    @PostMapping("/public/fill/{responseId}/submit")
    public ResultResponse<Void> submit(
            @PathVariable Long responseId,
            @RequestBody List<QuestionnaireAnswer> answers,
            @RequestParam(required = false) String token,
            HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        responseService.submitAnswers(responseId, answers, ip, token);
        return ResultRes.success(null);
    }

    @Operation(summary = "回收数据分页")
    @GetMapping("/questionnaire/{id}/responses")
    public ResultResponse<Page<QuestionnaireResponse>> responsePage(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Boolean submitted) {
        userService.checkPermissionCode("questionnaire:data");
        return ResultRes.success(responseService.responsePage(id, pageNum, pageSize, status, submitted));
    }

    @Operation(summary = "回收详情")
    @GetMapping("/questionnaire/response/{responseId}")
    public ResultResponse<Map<String, Object>> responseDetail(@PathVariable Long responseId) {
        userService.checkPermissionCode("questionnaire:data");
        return ResultRes.success(responseService.responseDetail(responseId));
    }

    @Operation(summary = "导出回收数据")
    @GetMapping("/questionnaire/{id}/export")
    public void exportResponses(
            @PathVariable Long id,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Boolean submitted,
            @RequestParam(required = false, defaultValue = "label") String format,
            HttpServletResponse response) {
        userService.checkPermissionCode("questionnaire:data:export");
        responseService.exportResponses(id, status, submitted, format, response);
    }

    @Operation(summary = "全量回收数据（含答案，供前端 Excel 导出）")
    @GetMapping("/questionnaire/{id}/responses/export-data")
    public ResultResponse<List<Map<String, Object>>> exportData(
            @PathVariable Long id,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Boolean submitted) {
        userService.checkPermissionCode("questionnaire:data:export");
        return ResultRes.success(responseService.listExportData(id, status, submitted));
    }

    @Operation(summary = "问卷统计")
    @GetMapping("/questionnaire/{id}/statistics")
    public ResultResponse<Map<String, Object>> statistics(@PathVariable Long id) {
        userService.checkPermissionCode("questionnaire:data");
        return ResultRes.success(responseService.statistics(id));
    }

    @Operation(summary = "更新回收记录状态")
    @PutMapping("/questionnaire/response/{responseId}/status")
    @OperationLog(type = "update", module = "questionnaire", action = "更新回收状态")
    public ResultResponse<Void> updateResponseStatus(
            @PathVariable Long responseId, @RequestParam Integer status) {
        userService.checkPermissionCode("questionnaire:data");
        responseService.updateResponseStatus(responseId, status);
        return ResultRes.success(null);
    }
}
