package com.pagoda.matchmeal.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserChallenge extends BaseEntity {

    private Long userChallengeId;
    private Long userId;
    private Long challengeId;

    private String status;
    private int currentProgress;

    private LocalDate lastCheckedAt; // 마지막 체크 날짜 (하루 한번 제한용)
}
