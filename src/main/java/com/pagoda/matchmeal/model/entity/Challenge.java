package com.pagoda.matchmeal.model.entity;

import com.pagoda.matchmeal.model.enums.ChallengeType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
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
    
    // 목표 기간 설정
    private LocalDate startDate;
    private LocalDate endDate;
    
    // 성공해야하는 횟수
    private int goalCount;

    private LocalDateTime createdAt;
}
