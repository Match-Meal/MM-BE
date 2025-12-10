package com.pagoda.matchmeal.model.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FollowResponseDto {
    // 팔로우 통계
    private boolean isFollowing; // 팔로우 상태
    private Long followerCount; // 나를 팔로우
    private Long followingCount; // 내가 팔로우
}
