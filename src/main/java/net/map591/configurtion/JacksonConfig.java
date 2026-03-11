package net.map591.configurtion;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {
    
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 配置日期时间格式
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        // 处理非常规日期格式
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(
            DateTimeFormatter.ofPattern("yyyy:MM-dd HHmmss")) {
            @Override
            public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                try {
                    return super.deserialize(p, ctxt);
                } catch (DateTimeException e) {
                    // 尝试其他格式
                    String dateStr = p.getValueAsString();
                    if (dateStr.contains(":")) {
                        return LocalDateTime.parse(dateStr, 
                            DateTimeFormatter.ofPattern("yyyy:MM-dd HHmmss"));
                    }
                    throw e;
                }
            }
        });
        
        objectMapper.registerModule(javaTimeModule);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        return objectMapper;
    }
}