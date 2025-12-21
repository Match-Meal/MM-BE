package com.pagoda.matchmeal.model.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

// 메뉴 추천 요청
@Getter
@Builder
public class AiRecommendRequestDto {
    @JsonProperty("user_profile")
    private AiUserProfileDto userProfile;

    @JsonProperty("current_intake")
    private AiIntakeSummaryDto currentIntake;

    @JsonProperty("meal_type")
    private String mealType;
}
