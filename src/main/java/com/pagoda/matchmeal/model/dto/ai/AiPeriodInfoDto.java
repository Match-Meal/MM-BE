package com.pagoda.matchmeal.model.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

// 사용자 프로필
@Getter
@Builder
public class AiPeriodInfoDto {
    @JsonProperty("start_date")
    private String startDate;
    @JsonProperty("end_date")
    private String endDate;
    @JsonProperty("total_days")
    private long totalDays;
    @JsonProperty("recorded_meals")
    private int recordedMeals;
}
