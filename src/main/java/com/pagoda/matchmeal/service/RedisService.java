package com.pagoda.matchmeal.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    // RefreshToken 저장 (Key: "RT:{userId}", Value: token, Duration: 만료시간)
    public void setValues(String key, String data, Duration duration) {
        ValueOperations<String, String> values = redisTemplate.opsForValue();
        values.set(key, data, duration);
    }

    // 값 가져오기
    public String getValues(String key) {
        ValueOperations<String, String> values = redisTemplate.opsForValue();
        return values.get(key);
    }

    // 값 삭제
    public void deleteValues(String key) {
        redisTemplate.delete(key);
    }

    public void setObject(String key, Object data) {
        try {
            String jsonString = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(key, jsonString);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis 저장 실패", e);
        }
    }

    public <T> T getObject(String key, TypeReference<T> typeReference) {
        String jsonString = redisTemplate.opsForValue().get(key);
        if (jsonString == null) {
            return null;
        }
        try {
            return objectMapper.readValue(jsonString, typeReference);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis 조회 실패", e);
        }
    }

}
