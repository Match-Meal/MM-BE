package com.pagoda.matchmeal.model.dto.response;

import com.pagoda.matchmeal.model.enums.ChallengeType;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveChallengeDto {
    // user_challenges 테이블 정보
    private Long userChallengeId;
    private int currentCount;
    private int currentStreak;
    private int maxStreak;
    private LocalDate lastSuccessDate;

    private String status;

    // challenges 테이블 정보 (JOIN 데이터)
    private Long challengeId;
    private String title;

    private ChallengeType type;
    private int targetValue;
    private LocalDate startDate;
    private LocalDate endDate;
    private int goalCount;
}
