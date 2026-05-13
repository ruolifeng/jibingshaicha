package cn.luyou.controller;

import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Tag(name = "数据备份")
@RestController
@RequestMapping("/backup")
@RequiredArgsConstructor
@Slf4j
public class BackupController {

    @Value("${spring.datasource.url:jdbc:mysql://localhost:3306/disease_track}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:root}")
    private String dbUser;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${app.backup.dir:./backups}")
    private String backupDir;

    /** 手动触发备份，并返回文件供下载 */
    @Operation(summary = "手动备份并下载")
    @GetMapping("/download")
    public void backupAndDownload(HttpServletResponse response) throws IOException {
        String filePath = doBackup();
        if (filePath == null) {
            response.sendError(500, "备份失败，请查看后端日志");
            return;
        }
        File file = new File(filePath);
        response.setContentType("application/octet-stream");
        String encodedName = URLEncoder.encode(file.getName(), StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);
        response.setContentLengthLong(file.length());
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.transferTo(response.getOutputStream());
        }
    }

    /** 获取已有备份文件列表 */
    @Operation(summary = "备份文件列表")
    @GetMapping("/list")
    public ResultResponse<List<Map<String, Object>>> listBackups() {
        List<Map<String, Object>> result = new ArrayList<>();
        Path dir = Paths.get(backupDir);
        if (!Files.exists(dir)) return ResultRes.success(result);

        File[] files = dir.toFile().listFiles(f -> f.getName().endsWith(".sql"));
        if (files == null) return ResultRes.success(result);

        Arrays.stream(files)
                .sorted(Comparator.comparingLong(File::lastModified).reversed())
                .forEach(f -> result.add(Map.of(
                        "name", f.getName(),
                        "size", f.length(),
                        "lastModified", f.lastModified()
                )));
        return ResultRes.success(result);
    }

    /** 执行 mysqldump 备份 */
    private String doBackup() {
        try {
            Path dir = Paths.get(backupDir);
            if (!Files.exists(dir)) Files.createDirectories(dir);

            // 从 JDBC URL 解析 host、port、dbName
            // 格式：jdbc:mysql://host:port/dbName?params
            Pattern pattern = Pattern.compile("jdbc:mysql://([^:/]+)(?::(\\d+))?/(\\w+)");
            Matcher matcher = pattern.matcher(datasourceUrl);
            String dbHost = "localhost";
            String dbPort = "3306";
            String dbName = "disease_monitor";
            if (matcher.find()) {
                dbHost = matcher.group(1);
                dbPort = matcher.group(2) != null ? matcher.group(2) : "3306";
                dbName = matcher.group(3);
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "backup_" + dbName + "_" + timestamp + ".sql";
            String filePath = backupDir + "/" + fileName;

            ProcessBuilder pb = new ProcessBuilder(
                    "mysqldump", "-h", dbHost, "--port", dbPort,
                    "-u", dbUser, "-p" + dbPassword, "--single-transaction", dbName
            );
            pb.redirectOutput(new File(filePath));
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("数据库备份成功：{}", filePath);
                return filePath;
            } else {
                log.error("数据库备份失败，退出码：{}", exitCode);
                return null;
            }
        } catch (Exception e) {
            log.error("数据库备份异常：{}", e.getMessage());
            return null;
        }
    }
}
