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
    private int currentProgress;
    private LocalDate lastCheckedAt;

    // challenges 테이블 정보 (JOIN 데이터)
    private Long challengeId;
    private ChallengeType type;
    private int targetValue;
}
