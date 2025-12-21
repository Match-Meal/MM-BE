package com.pagoda.matchmeal.common.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 애플리케이션 공통 설정 클래스
 * - RestTemplate 등 공용 빈 등록
 */
@Configuration
public class AppConfig {

    /**
     * RestTemplate 빈 등록
     * 외부 API 호출 시 무한 대기를 방지하기 위해 연결 및 읽기 타임아웃을 5초로 설정
     *
     * @param builder RestTemplateBuilder
     * @return 설정된 RestTemplate 객체
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                // 외부 API 호출 시 무한 대기 방지를 위한 타임아웃 설정 (5초)
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(5))
                .build();
    }
}