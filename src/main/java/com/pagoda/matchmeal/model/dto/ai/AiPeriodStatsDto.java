package com.pagoda.matchmeal.model.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

// 공통: 기간 영양 통계
@Getter
@Builder
public class AiPeriodStatsDto {
    @JsonProperty("avg_calories")
    private double avgCalories;
    @JsonProperty("total_sodium")
    private double totalSodium;
    @JsonProperty("total_sugar")
    private double totalSugar;
}
