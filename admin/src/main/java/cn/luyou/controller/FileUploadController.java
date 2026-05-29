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

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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

        Path dir = resolveUploadDir();
        String originalFilename = file.getOriginalFilename();
        String ext = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String savedName = UUID.randomUUID().toString().replace("-", "") + ext;

        Path target = dir.resolve(savedName).normalize();
        if (!target.startsWith(dir)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "非法文件名");
        }

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("附件保存失败，dir={}, file={}", dir, savedName, e);
            throw new ServiceException(StatusEnum.SERVICE_ERROR, "附件保存失败，请检查服务器上传目录权限");
        }
        log.info("附件上传成功：{} -> {}", originalFilename, savedName);

        String encodedName = URLEncoder.encode(
                originalFilename != null ? originalFilename : savedName,
                StandardCharsets.UTF_8);
        return ResultRes.success("/file/serve/" + savedName + "?name=" + encodedName);
    }

    @Operation(summary = "访问/下载附件")
    @GetMapping("/serve/{filename}")
    public void serve(
            @PathVariable String filename,
            @RequestParam(required = false) String name,
            HttpServletResponse response) throws IOException {
        if (filename.contains("..") || filename.contains("/")) {
            response.sendError(400, "非法文件名");
            return;
        }

        Path dir = resolveUploadDir();
        Path file = dir.resolve(filename).normalize();
        if (!file.startsWith(dir)) {
            response.sendError(400, "非法文件名");
            return;
        }
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

        try (InputStream in = Files.newInputStream(file)) {
            in.transferTo(response.getOutputStream());
        }
    }

    /** 解析为绝对路径，避免 multipart transferTo 在相对目录下失败 */
    private Path resolveUploadDir() throws IOException {
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        return dir;
    }
}
