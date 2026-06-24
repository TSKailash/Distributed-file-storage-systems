package com.kailash.storageservice.config;

import com.kailash.storageservice.model.FileMetaData;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, FileMetaData> redisTemplate(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper){
        RedisTemplate<String, FileMetaData> template=new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        JacksonJsonRedisSerializer<FileMetaData> serializer =
                new JacksonJsonRedisSerializer<>(objectMapper, FileMetaData.class);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();

        return template;
    }
}
