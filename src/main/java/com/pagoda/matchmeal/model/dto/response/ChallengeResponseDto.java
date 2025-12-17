package com.pagoda.matchmeal.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pagoda.matchmeal.model.enums.ChallengeType;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeResponseDto {
    private Long challengeId;
    private String title;
    private String description;
    private ChallengeType type;
    
    private int targetValue; // 목표 수치
    private LocalDate startDate;
    private LocalDate endDate;
    private int goalCount; // 목표 횟수
    
    // 유저별 진행상황
    private Long userChallengeId;

    @JsonProperty("isJoined")
    private boolean isJoined; // 참여 여부
    private String status; // 성공 여부

    private int currentCount;
    private int currentStreak;
    private int maxStreak;
    private int progressPercent; // 현재 달성 횟수

    private LocalDate lastSuccessDate;
}
