package com.pagoda.matchmeal.model.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AiMealPlanRequestDto {
    @JsonProperty("user_profile")
    private AiUserProfileDto userProfile;

    @JsonProperty("period_info")
    private AiPeriodInfoDto periodInfo;

    @JsonProperty("flavors")
    private List<String> flavors;
}
