package cn.luyou.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 定时自动备份任务
 * - 每天 03:00 自动执行 mysqldump
 * - 保留最近 30 份备份，超出自动删除最旧的
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoBackupTask {

    @Value("${spring.datasource.url:jdbc:mysql://localhost:3306/disease_track}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:root}")
    private String dbUser;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${app.backup.dir:./backups}")
    private String backupDir;

    private static final int MAX_BACKUPS = 30;

    @Scheduled(cron = "0 0 3 * * ?")
    public void autoBackup() {
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
            String filePath = backupDir + "/auto_backup_" + dbName + "_" + timestamp + ".sql";

            ProcessBuilder pb = new ProcessBuilder(
                    "mysqldump", "-h", dbHost, "--port", dbPort,
                    "-u", dbUser, "-p" + dbPassword, "--single-transaction", dbName
            );
            pb.redirectOutput(new File(filePath));
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("自动备份成功：{}", filePath);
                pruneOldBackups(dir);
            } else {
                log.error("自动备份失败，退出码：{}", exitCode);
            }
        } catch (Exception e) {
            log.error("自动备份异常：{}", e.getMessage());
        }
    }

    /** 清理超过 MAX_BACKUPS 份的旧备份 */
    private void pruneOldBackups(Path dir) {
        File[] files = dir.toFile().listFiles(f -> f.getName().endsWith(".sql"));
        if (files == null || files.length <= MAX_BACKUPS) return;
        Arrays.stream(files)
                .sorted(Comparator.comparingLong(File::lastModified))
                .limit(files.length - MAX_BACKUPS)
                .forEach(f -> {
                    if (f.delete()) log.info("清理旧备份：{}", f.getName());
                });
    }
}
