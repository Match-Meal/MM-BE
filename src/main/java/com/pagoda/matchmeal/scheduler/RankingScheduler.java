package com.pagoda.matchmeal.scheduler;

import com.pagoda.matchmeal.mapper.RankingMapper;
import com.pagoda.matchmeal.model.dto.RankingDto;
import com.pagoda.matchmeal.service.RankingSseService;
import com.pagoda.matchmeal.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RankingScheduler {

    private final RankingMapper rankingMapper;
    private final RankingSseService rankingSseService;
    private final RedisService redisService;

    private final String[] CATEGORIES = {"ALL", "BREAKFAST", "LUNCH", "DINNER", "SNACK"};

    @Scheduled(fixedRate = 30000)
    public void updateAndBroadcastRanking() {
        // 1. 전체 데이터를 담을 Map 생성
        Map<String, List<RankingDto>> fullRankingData = new HashMap<>();

        // 2. 5가지 카테고리별로 루프 돌면서 DB 조회
        for (String category : CATEGORIES) {
            List<RankingDto> rankList = rankingMapper.getRanking(category);

            // 키값 소문자로 변환하여 저장 (all, breakfast, ...)
            fullRankingData.put(category.toLowerCase(), rankList);
        }

        // 3. Redis에 저장 (새로 들어온 유저를 위해 통째로 저장)
        redisService.setObject("RANKING:FULL_DATA", fullRankingData);

        // 4. 현재 접속 중인 모든 유저에게 한 번에 전송
        rankingSseService.broadcastRanking(fullRankingData);

        // 로그 확인용
        // System.out.println("7일간 랭킹 갱신 완료: " + LocalDateTime.now());
    }
}
