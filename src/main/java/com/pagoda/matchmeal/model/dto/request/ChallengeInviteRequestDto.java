package com.pagoda.matchmeal.model.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChallengeInviteRequestDto {
    private Long targetUserId;
}
