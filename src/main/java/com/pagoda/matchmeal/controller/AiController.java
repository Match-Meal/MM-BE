package com.pagoda.matchmeal.controller;

import com.pagoda.matchmeal.common.response.CommonResponse;
import com.pagoda.matchmeal.common.util.ApiResponseUtil;
import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.model.dto.request.PeriodRequestDto;
import com.pagoda.matchmeal.model.dto.response.AiChatbotResponseDto;
import com.pagoda.matchmeal.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    // 기간별 피드백 (DTO로 날짜 받기)
    @PostMapping("/feedback")
    public CommonResponse<String> periodFeedback(@AuthenticationPrincipal UserDto userDto,
                                                 @RequestBody PeriodRequestDto req) {

        // PeriodRequestDto는 startDate, endDate만 있는 간단한 DTO
        String result = aiService.getPeriodFeedback(userDto.getId(), req.getStartDate(), req.getEndDate());
        return ApiResponseUtil.success(result);
    }

    // 메뉴 추천 (DTO로 식사타입 받기)
    @PostMapping("/recommend")
    public CommonResponse<String> recommend(@AuthenticationPrincipal UserDto userDto,
                                            @RequestBody Map<String, String> body) {
        String mealType = body.getOrDefault("mealType", "식사");
        String result = aiService.getMenuRecommendation(userDto.getId(), mealType);
        return ApiResponseUtil.success(result);
    }

    // 요청 예시: GET /ai/history/1
    @GetMapping("/history/{userId}")
    public CommonResponse<List<AiChatbotResponseDto>> getAiHistory(@PathVariable Long userId) {
        List<AiChatbotResponseDto> history = aiService.getChatHistory(userId);
        return ApiResponseUtil.success(history);
    }
}
