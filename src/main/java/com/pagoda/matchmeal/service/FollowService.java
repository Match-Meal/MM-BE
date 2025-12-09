package com.pagoda.matchmeal.service;

import com.pagoda.matchmeal.model.dto.response.FollowResponseDto;

public interface FollowService {
    FollowResponseDto toggleFollow(Long followerId, Long followingId);
}
