package com.pagoda.matchmeal.model.entity;

import com.pagoda.matchmeal.model.enums.ChallengeType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Challenge {
    private Long challengeId;
    private String title;
    private String description;
    private ChallengeType type;

    private int targetValue; // 목표 수치
    private int duration; // 챌린지 기간

    private LocalDateTime createdAt;
}
