package com.pagoda.matchmeal.model.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ChallengeParticipantDto {
    private Long userId;
    private String userName;
    private String profileImage;
    private int progressPercent;
}
