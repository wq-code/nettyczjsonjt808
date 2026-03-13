package net.map591.configurtion;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);

        // 1. Key序列化：String类型
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringRedisSerializer);
        redisTemplate.setHashKeySerializer(stringRedisSerializer);

        // 2. Value序列化：使用自定义的Jackson序列化器
        GenericJackson2JsonRedisSerializer jsonSerializer = createJacksonSerializer();
        redisTemplate.setValueSerializer(jsonSerializer);
        redisTemplate.setHashValueSerializer(jsonSerializer);

        // 3. 开启事务支持（可选）
        redisTemplate.setEnableTransactionSupport(true);

        // 4. 设置连接池配置
        redisTemplate.afterPropertiesSet();


        // 测试连接
        testConnection(redisTemplate);

        return redisTemplate;
    }

    /**
     * 创建支持Java 8时间类型的Jackson序列化器
     */
    private GenericJackson2JsonRedisSerializer createJacksonSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();

        // 1. 注册Java 8时间模块
        objectMapper.registerModule(new JavaTimeModule());

        // 2. 禁用WRITE_DATES_AS_TIMESTAMPS，使用ISO-8601格式
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 3. 配置时区
        objectMapper.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));

        // 4. 配置序列化/反序列化特性
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        // 5. 配置LocalDateTime格式
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        objectMapper.registerModule(javaTimeModule);

        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

    private void testConnection(RedisTemplate<String, Object> redisTemplate) {
        try {
            redisTemplate.opsForValue().set("test:connection", "success", 10, TimeUnit.SECONDS);
            String result = (String) redisTemplate.opsForValue().get("test:connection");
            if ("success".equals(result)) {
                System.out.println("✅ Redis连接测试成功");
            } else {
                System.err.println("❌ Redis连接测试失败");
            }
        } catch (Exception e) {
            System.err.println("❌ Redis连接异常: " + e.getMessage());
            throw new RuntimeException("Redis连接失败", e);
        }
    }
}