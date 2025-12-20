package com.pagoda.matchmeal.model.dto.response;

import com.pagoda.matchmeal.model.enums.ChallengeType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeInvitationResponseDto {
    // 초대 정보
    private Long invitationId;
    private LocalDateTime sentAt; // 보낸 시간 (createdAt)

    // 보낸 사람 (Inviter) 정보
    private Long inviterId;
    private String inviterName;
    private String inviterProfileImage;

    // 챌린지 정보
    private Long challengeId;
    private String challengeTitle;
    private ChallengeType type;
    private int targetValue;
    private int goalCount;
    private int currentHeadCount;
    private int maxParticipants;
}