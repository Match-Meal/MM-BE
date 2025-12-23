package com.pagoda.matchmeal.controller;

import com.pagoda.matchmeal.service.RankingSseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class RankingController {

    private final RankingSseService rankingSseService;

    @GetMapping(value = "/ranking/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        String userId = UUID.randomUUID().toString();
        return rankingSseService.subscribe(userId);
    }
}
