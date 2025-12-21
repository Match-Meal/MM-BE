package com.pagoda.matchmeal.service;

import java.time.LocalDate;

public interface AiService {
    String getPeriodFeedback(Long userId, LocalDate startDate, LocalDate endDate);

    String getMenuRecommendation(Long userId, String mealType);
}
