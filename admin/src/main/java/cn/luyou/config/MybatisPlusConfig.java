package cn.luyou.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 全局配置
 *
 * @author ruolifeng
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 注册 MyBatis-Plus 插件（分页插件等）
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        // 与前端 PAGE_SIZE_OPTIONS 最大项一致，避免选择 1000 条时被默认 500 截断
        pagination.setMaxLimit(1000L);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }

    /**
     * 可配置 worker / datacenter 的雪花 ID 生成器（供 IdType.ASSIGN_ID 使用）
     */
    @Bean
    public IdentifierGenerator identifierGenerator(
            @Value("${app.snowflake.worker-id:1}") long workerId,
            @Value("${app.snowflake.datacenter-id:1}") long datacenterId) {
        return new DefaultIdentifierGenerator(workerId, datacenterId);
    }
}
