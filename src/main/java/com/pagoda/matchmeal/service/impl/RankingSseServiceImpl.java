package com.pagoda.matchmeal.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.pagoda.matchmeal.model.dto.RankingDto;
import com.pagoda.matchmeal.service.RankingSseService;
import com.pagoda.matchmeal.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RankingSseServiceImpl implements RankingSseService {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final RedisService redisService;

    private static final TypeReference<Map<String, List<RankingDto>>> RANKING_TYPE_REF =
            new TypeReference<>() {
            };

    @Override
    public SseEmitter subscribe(String id) {
        // 타임아웃 설정 (기본값보다 길게 설정, 예: 5분)
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        emitters.put(id, emitter);

        // 연결 종료나 타임아웃 시 목록에서 제거
        emitter.onCompletion(() -> emitters.remove(id));
        emitter.onTimeout(() -> emitters.remove(id));
        emitter.onError((e) -> emitters.remove(id));

        // 503 에러 방지를 위한 더미 데이터 전송
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected!"));
        } catch (IOException e) {
            emitters.remove(id);
        }

        sendInitialData(id, emitter);

        return emitter;
    }

    private void sendInitialData(String id, SseEmitter emitter) {
        try {
            // 3-1. "연결됨" 더미 이벤트 전송 (503 에러 방지용)
            emitter.send(SseEmitter.event().name("connect").data("connected!"));

            // 3-2. Redis에서 캐싱된 랭킹 데이터 조회
            Map<String, List<RankingDto>> cachedData = redisService.getObject("RANKING:FULL_DATA", RANKING_TYPE_REF);

            // 3-3. 데이터가 존재하면 즉시 전송 (입장 선물 🎁)
            if (cachedData != null) {
                emitter.send(SseEmitter.event()
                        .name("ranking-update") // 프론트가 기다리는 그 이벤트 이름
                        .data(cachedData));

            }
        } catch (IOException e) {
            emitters.remove(id); // 전송 실패 시 바로 제거
        }
    }

    @Override
    public void broadcastRanking(Map<String, List<RankingDto>> rankingData) {
        emitters.forEach((id, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("ranking-update") // 이벤트 이름
                        .data(rankingData));    // 데이터 (JSON 자동 변환)
            } catch (IOException e) {
                // 전송 실패 시 연결 해제 간주
                emitters.remove(id);
            }
        });
    }
}
