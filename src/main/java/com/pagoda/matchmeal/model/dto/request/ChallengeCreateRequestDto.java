package com.pagoda.matchmeal.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pagoda.matchmeal.model.enums.ChallengeType;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class ChallengeCreateRequestDto {
    private String title;
    private String description;
    private ChallengeType type;
    private int targetValue;
    private LocalDate startDate;
    private LocalDate endDate;
    private int goalCount;

    private int maxParticipants;
    @JsonProperty("isPublic")
    private boolean isPublic;
}
