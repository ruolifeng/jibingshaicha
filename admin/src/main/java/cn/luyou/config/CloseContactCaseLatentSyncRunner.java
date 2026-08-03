package cn.luyou.config;

import cn.luyou.utils.CloseContactCaseLatentSyncSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 一次性：将密接个案表中终筛为「潜伏感染者」的数据同步补充到潜伏感染在管。
 * <p>
 * 启用：app.sync-close-contact-case-latent=true，启动成功后改回 false。
 */
@Slf4j
@Component
@Order(50)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.sync-close-contact-case-latent", havingValue = "true")
public class CloseContactCaseLatentSyncRunner implements ApplicationRunner {

    private final CloseContactCaseLatentSyncSupport closeContactCaseLatentSyncSupport;

    @Override
    public void run(ApplicationArguments args) {
        int count = closeContactCaseLatentSyncSupport.syncAllLatentCases();
        log.info("密接个案→潜伏在管回填完成，处理 {} 条", count);
    }
}
