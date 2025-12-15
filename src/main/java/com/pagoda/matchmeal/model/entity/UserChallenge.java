package com.pagoda.matchmeal.model.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserChallenge extends BaseEntity {

    private Long userChallengeId;
    private Long userId;
    private Long challengeId;

    private String status; // Progress, success, fail

    // 기록 달성 현황
    private int currentCount; // 총 성공 횟수
    private int currentStreak; // 현재 연속 성공 일수 (끊기면 1로 초기화)
    private int maxStreak; // 최대 연속 성공 일수 (기록)

    private LocalDate lastSuccessDate; // 마지막 성공 날짜

}
