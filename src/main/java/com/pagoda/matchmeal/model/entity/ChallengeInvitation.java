package com.pagoda.matchmeal.model.entity;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChallengeInvitation {
    private Long invitationId;
    private Long challengeId;
    private Long inviterId; // 보낸 사람
    private Long inviteeId; // 받은 사람
    private String status; // PENDING, ACCEPTED, REJECTED
    private LocalDateTime createdAt;

}
