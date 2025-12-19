package com.pagoda.matchmeal.common.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        // 1. [타임아웃 설정] AI 분석이 오래 걸릴 수 있으므로 넉넉하게 잡습니다.
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000) // 연결 타임아웃 (30초)
                .responseTimeout(Duration.ofSeconds(60))             // 응답 타임아웃 (60초 - AI 분석 대기 시간)
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(60, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(60, TimeUnit.SECONDS)));

        // 2. [메모리 버퍼 설정] 대용량 데이터(이미지 등) 통신 시 버퍼 크기 증가
        // 기본값은 256KB라 이미지 처리 시 "DataBufferLimitException"이 발생할 수 있습니다.
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB로 설정
                .build();

        // 3. [WebClient 빌드]
        return WebClient.builder()
                .baseUrl("http://127.0.0.1:8000") // FastAPI 기본 주소 (Service에서 중복 제거 가능)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .build();
    }
}