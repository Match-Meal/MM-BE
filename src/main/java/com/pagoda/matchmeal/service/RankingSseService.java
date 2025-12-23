package com.pagoda.matchmeal.service;

import com.pagoda.matchmeal.model.dto.RankingDto;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

public interface RankingSseService {

    SseEmitter subscribe(String id);

    void broadcastRanking(Map<String, List<RankingDto>> rankingData);
}
