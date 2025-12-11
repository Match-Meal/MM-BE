package com.pagoda.matchmeal.model.dto.response;

import com.pagoda.matchmeal.model.enums.ChallengeType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChallengeResponseDto {
    private Long challengeId;
    private String title;
    private String description;
    private ChallengeType type;
    private int targetValue; // 목표 수치
    
    // 유저별 진행상황
    private Long userChallengeId;
    private int currentProgress; // 현재 달성 횟수
    private boolean isJoined; // 참여 여부
    private boolean isSuccess; // 성공 여부

}
