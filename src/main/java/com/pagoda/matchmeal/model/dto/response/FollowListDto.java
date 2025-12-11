package com.pagoda.matchmeal.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowListDto {
    private Long userId;
    private String userName;
    private String profileImage;

    @JsonProperty("isFollowing")
    private boolean isFollowing;
}
