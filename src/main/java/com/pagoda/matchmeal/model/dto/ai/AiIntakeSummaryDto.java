package com.pagoda.matchmeal.model.dto.ai;

import lombok.Builder;
import lombok.Getter;

// 공통: 섭취 요약 (오늘 누적용)
@Getter
@Builder
public class AiIntakeSummaryDto {
    private double calories;
    private double sodium;
    private double sugar;
}
