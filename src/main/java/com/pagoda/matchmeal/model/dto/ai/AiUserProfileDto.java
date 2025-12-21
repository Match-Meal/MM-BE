package com.pagoda.matchmeal.model.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

// 공통: 기간정보
@Getter
@Builder
public class AiUserProfileDto {
    private String name;
    private int age;
    private String gender;
    private double bmi;
    @JsonProperty("bmi_status")
    private String bmiStatus;
    private String allergies;
    private String diseases;
}
