package com.pagoda.matchmeal.model.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

// 기간 피드백 요청
@Getter
@Builder
public class AiPeriodFeedbackRequestDto {
    @JsonProperty("user_profile")
    private AiUserProfileDto userProfile;

    @JsonProperty("period_info")
    private AiPeriodInfoDto periodInfo;

    @JsonProperty("nutrition_stats")
    private AiPeriodStatsDto nutritionStats;

    @JsonProperty("menu_list")
    private List<String> menuList;
}
