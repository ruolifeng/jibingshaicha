package cn.luyou.config;

import cn.luyou.crypto.FieldEncryptor;
import cn.luyou.crypto.SensitiveDataInterceptor;
import org.apache.ibatis.plugin.Interceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
public class CryptoConfiguration {

    @Bean
    public FieldEncryptor fieldEncryptor(
            @Value("${app.crypto.key:}") String key,
            @Value("${app.crypto.key-file:./data/field-encrypt.key}") String keyFile) {
        return FieldEncryptor.fromConfiguredKey(key, Path.of(keyFile));
    }

    @Bean
    public Interceptor sensitiveDataInterceptor(FieldEncryptor fieldEncryptor) {
        return new SensitiveDataInterceptor(fieldEncryptor);
    }
}
