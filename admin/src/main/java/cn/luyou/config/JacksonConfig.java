package cn.luyou.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Jackson 配置：仅将超出 JS 安全整数范围的 Long 序列化为字符串（雪花 ID），
 * 统计计数等小 Long 仍输出 JSON number，避免看板/统计前端做字符串拼接运算。
 * <p>
 * 使用 {@code serializerByType} 注册，不影响 JavaTime 等默认模块。
 */
@Configuration
public class JacksonConfig {

    /** JavaScript Number.MAX_SAFE_INTEGER */
    private static final long JS_MAX_SAFE = 9007199254740991L;

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longSafeSerializeCustomizer() {
        JsonSerializer<Long> serializer = new JsonSerializer<>() {
            @Override
            public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers)
                    throws IOException {
                if (value == null) {
                    gen.writeNull();
                    return;
                }
                if (value > JS_MAX_SAFE || value < -JS_MAX_SAFE) {
                    gen.writeString(value.toString());
                } else {
                    gen.writeNumber(value);
                }
            }
        };
        return builder -> {
            builder.serializerByType(Long.class, serializer);
            builder.serializerByType(Long.TYPE, serializer);
        };
    }
}
