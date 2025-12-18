package com.pagoda.matchmeal.service.impl;

import com.pagoda.matchmeal.common.exception.CustomException;
import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import com.pagoda.matchmeal.mapper.FollowMapper;
import com.pagoda.matchmeal.mapper.UserMapper;
import com.pagoda.matchmeal.model.dto.response.FollowListDto;
import com.pagoda.matchmeal.model.dto.response.FollowResponseDto;
import com.pagoda.matchmeal.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {
    
    private final FollowMapper followMapper;
    private final UserMapper userMapper;

    /**
     * 팔로우 토글(이미 팔로우 중이면 취소, 아니면 팔로우
     * @param followerId
     * @param followingId
     */
    @Override
    @Transactional
    public FollowResponseDto toggleFollow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new CustomException(ErrorResponseCode.SELF_FOLLOW_NOT_ALLOWED);
        }

        // 대상 확인
        userMapper.findById(followingId)
                .orElseThrow(() -> new CustomException(ErrorResponseCode.USER_NOT_FOUND));
        
        // 이미 팔로우 중인지 확인
        boolean exists = followMapper.existsByFollowerAndFollowing(followerId, followingId);
        boolean currentStatus;

        if (exists) {
            followMapper.deleteFollow(followerId, followingId); // 언팔로우
            currentStatus = false;
        } else {
            followMapper.insertFollow(followerId, followingId); // 팔로우
            currentStatus = true;
        }

        Long targetFollowerCount = followMapper.countFollowers(followingId);
        Long myFollowingCount = followMapper.countFollowings(followerId);

        return FollowResponseDto.builder()
                .isFollowing(currentStatus)
                .followerCount(targetFollowerCount)
                .followingCount(myFollowingCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FollowListDto> getFollowers(Long targetUserId, Long viewerId) {
        // 대상 유저 존재 확인
        userMapper.findById(targetUserId).orElseThrow(() -> new CustomException(ErrorResponseCode.USER_NOT_FOUND));

        return followMapper.getFollowers(targetUserId, viewerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FollowListDto> getFollowings(Long targetUserId, Long viewerId) {
        return followMapper.getFollowings(targetUserId, viewerId);
    }

    /**
     * 팔로윙 확인
     * @param followerId
     * @param followingId
     */
    @Override
    public boolean isFollowing(Long followerId, Long followingId) {
        return followMapper.existsByFollowerAndFollowing(followerId, followingId);
    }
}
