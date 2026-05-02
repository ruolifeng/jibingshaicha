package cn.luyou.controller;

import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Tag(name = "文件上传")
@RestController
@RequestMapping("/file")
@Slf4j
public class FileUploadController {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Operation(summary = "上传附件（图片/PDF等）")
    @PostMapping("/upload")
    public ResultResponse<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "文件不能为空");
        }

        // 文件大小限制：20MB
        if (file.getSize() > 20 * 1024 * 1024) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "文件大小不能超过 20MB");
        }

        Path dir = Paths.get(uploadDir);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        // 保留原始扩展名，以 UUID 作为唯一文件名防止冲突
        String originalFilename = file.getOriginalFilename();
        String ext = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String savedName = UUID.randomUUID().toString().replace("-", "") + ext;

        Path target = dir.resolve(savedName);
        file.transferTo(target.toFile());
        log.info("附件上传成功：{} -> {}", originalFilename, savedName);

        // 返回访问路径（前端通过 VITE_BASE_URL 拼接成完整 URL）
        String encodedName = URLEncoder.encode(originalFilename != null ? originalFilename : savedName, StandardCharsets.UTF_8);
        return ResultRes.success("/file/serve/" + savedName + "?name=" + encodedName);
    }

    @Operation(summary = "访问/下载附件")
    @GetMapping("/serve/{filename}")
    public void serve(
            @PathVariable String filename,
            @RequestParam(required = false) String name,
            HttpServletResponse response) throws IOException {
        // 防止路径穿越攻击
        if (filename.contains("..") || filename.contains("/")) {
            response.sendError(400, "非法文件名");
            return;
        }

        Path file = Paths.get(uploadDir).resolve(filename);
        if (!Files.exists(file)) {
            response.sendError(404, "文件不存在");
            return;
        }

        String contentType = Files.probeContentType(file);
        response.setContentType(contentType != null ? contentType : "application/octet-stream");

        String displayName = (name != null && !name.isBlank()) ? name : filename;
        String encodedName = URLEncoder.encode(displayName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "inline; filename*=UTF-8''" + encodedName);
        response.setContentLengthLong(Files.size(file));

        try (FileInputStream fis = new FileInputStream(file.toFile())) {
            fis.transferTo(response.getOutputStream());
        }
    }
}
