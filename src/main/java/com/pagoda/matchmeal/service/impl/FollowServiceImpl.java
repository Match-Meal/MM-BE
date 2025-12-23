package com.pagoda.matchmeal.service.impl;

import com.pagoda.matchmeal.common.exception.CustomException;
import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import com.pagoda.matchmeal.mapper.FollowMapper;
import com.pagoda.matchmeal.mapper.UserMapper;
import com.pagoda.matchmeal.model.dto.response.FollowListDto;
import com.pagoda.matchmeal.model.dto.response.FollowResponseDto;
import com.pagoda.matchmeal.model.enums.NotificationType;
import com.pagoda.matchmeal.service.FollowService;
import com.pagoda.matchmeal.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 팔로우(Follow) 비즈니스 로직 구현체
 * - 팔로우/언팔로우 토글 기능
 * - 팔로워 및 팔로잉 목록 조회 기능
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowServiceImpl implements FollowService {

    private final FollowMapper followMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    /**
     * 팔로우 토글(이미 팔로우 중이면 취소, 아니면 팔로우
     *
     * @param followerId
     * @param followingId
     */
    @Override
    @Transactional
    public FollowResponseDto toggleFollow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new CustomException(ErrorResponseCode.SELF_FOLLOW_NOT_ALLOWED);
        }

        // 대상 유저 존재 여부 확인
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

        // 갱신된 카운트 조회
        Long targetFollowerCount = followMapper.countFollowers(followingId);
        Long myFollowingCount = followMapper.countFollowings(followerId);

        String followerName = userMapper.findById(followerId).orElseThrow(() -> new CustomException(ErrorResponseCode.USER_NOT_FOUND)).getUserName();

        notificationService.sendToUser(
                followingId,            // 받는 사람 (팔로우 당한 사람)
                followerId,             // 보낸 사람 (나)
                NotificationType.FOLLOW,
                followerName + "님이 회원님을 팔로우하기 시작했습니다.",
                followerId.intValue(),  // 클릭 시 상대방 프로필로 이동
                "/users/" + followerId
        );

        return FollowResponseDto.builder()
                .isFollowing(currentStatus)
                .followerCount(targetFollowerCount)
                .followingCount(myFollowingCount)
                .build();
    }

    /**
     * 팔로워 목록 조회 (나를 팔로우 하는 사람들)
     *
     * @param targetUserId 조회 대상 유저의 PK (누구의 팔로워를 볼 것인가)
     * @param viewerId     목록을 보는 유저의 PK (리스트 내 유저들과의 맞팔 여부 확인용)
     * @return 팔로워 유저 목록 DTO 리스트
     */
    @Override
    public List<FollowListDto> getFollowers(Long targetUserId, Long viewerId) {
        // 대상 유저 존재 확인
        userMapper.findById(targetUserId).orElseThrow(() -> new CustomException(ErrorResponseCode.USER_NOT_FOUND));

        return followMapper.getFollowers(targetUserId, viewerId);
    }

    /**
     * 팔로잉 목록 조회 (내가 팔로우 하는 사람들)
     *
     * @param targetUserId 조회 대상 유저의 PK (누구의 팔로잉을 볼 것인가)
     * @param viewerId     목록을 보는 유저의 PK (리스트 내 유저들과의 맞팔 여부 확인용)
     * @return 팔로잉 유저 목록 DTO 리스트
     */
    @Override
    public List<FollowListDto> getFollowings(Long targetUserId, Long viewerId) {
        return followMapper.getFollowings(targetUserId, viewerId);
    }

    /**
     * 팔로우 여부 단순 확인
     *
     * @param followerId  팔로우 하는 유저 PK
     * @param followingId 팔로우 받는 유저 PK
     * @return true: 팔로우 중, false: 팔로우 안 함
     */
    @Override
    public boolean isFollowing(Long followerId, Long followingId) {
        return followMapper.existsByFollowerAndFollowing(followerId, followingId);
    }
}