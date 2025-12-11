package com.pagoda.matchmeal.service;

import com.pagoda.matchmeal.model.dto.response.FollowListDto;
import com.pagoda.matchmeal.model.dto.response.FollowResponseDto;

import java.util.List;

public interface FollowService {
    FollowResponseDto toggleFollow(Long followerId, Long followingId);

    List<FollowListDto> getFollowers(Long targetUserId, Long viewerId);

    List<FollowListDto> getFollowings(Long targetUserId, Long viewerId);

}
