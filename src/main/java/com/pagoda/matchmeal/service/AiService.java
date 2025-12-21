package com.pagoda.matchmeal.service;

import com.pagoda.matchmeal.model.dto.response.AiChatbotResponseDto;

import java.time.LocalDate;
import java.util.List;

public interface AiService {
    String getPeriodFeedback(Long userId, LocalDate startDate, LocalDate endDate);

    String getMenuRecommendation(Long userId, String mealType);

    List<AiChatbotResponseDto> getChatHistory(Long userId);
}
