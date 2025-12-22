package com.pagoda.matchmeal.service;

import com.pagoda.matchmeal.model.dto.response.AiChatbotResponseDto;

import java.time.LocalDate;
import java.util.List;

public interface AiService {
    String getPeriodFeedback(Long userId, LocalDate startDate, LocalDate endDate);

    String getMenuRecommendation(Long userId, String mealType, List<String> flavors);

    List<AiChatbotResponseDto> getChatHistory(Long userId);

    String chatWithAi(Long userId, String message);

    // 식단 추천 (기간별)
    String getPeriodMealPlan(Long userId, java.time.LocalDate startDate, java.time.LocalDate endDate, java.util.List<String> flavors);
}
