package com.pagoda.matchmeal.model.entity;

import com.pagoda.matchmeal.model.enums.ChallengeType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Challenge {
    private Long challengeId;
    private Long ownerId; // 생성자 id
    private String title;
    private String description;
    private ChallengeType type;

    private int targetValue; // 목표 수치
    
    // 목표 기간 설정
    private LocalDate startDate;
    private LocalDate endDate;
    
    // 성공해야하는 횟수
    private int goalCount;
    
    private int maxParticipants; // 최대 인원
    private boolean isPublic; // 공개 여부(true: 공개, false: 비공개)
    private String invitationCode; // 참여 코드 (난수 문자열)
    private int currentHeadCount; // 현재 참여 인원

    private LocalDateTime createdAt;

    public boolean getIsPublic() {
        return this.isPublic;
    }

    public boolean isPublic() {
        return this.isPublic;
    }
}
