package com.pagoda.matchmeal.mapper;

import com.pagoda.matchmeal.model.dto.response.FollowListDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 팔로우/팔로잉 관계 관리 매퍼
 */
@Mapper
public interface FollowMapper {
    /**
     * 팔로우 추가 (관계 생성)
     */
    void insertFollow(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    /**
     * 팔로우 취소 (관계 삭제)
     */
    void deleteFollow(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    /**
     * 팔로우 여부 확인
     *
     * @return true: 이미 팔로우 중, false: 팔로우 안 함
     */
    boolean existsByFollowerAndFollowing(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    /**
     * 나를 팔로우하는 사람 수 조회 (Follower Count)
     */
    Long countFollowers(Long userId);

    /**
     * 내가 팔로우하는 사람 수 조회 (Following Count)
     */
    Long countFollowings(Long userId);

    /**
     * 팔로워 목록 조회
     *
     * @param targetId 누구의 팔로워를 볼 것인가
     * @param viewerId 현재 목록을 보는 사람 (맞팔 여부 확인용)
     */
    List<FollowListDto> getFollowers(@Param("targetId") Long targetId, @Param("viewerId") Long viewerId);

    /**
     * 팔로잉 목록 조회
     *
     * @param targetId 누구의 팔로잉을 볼 것인가
     * @param viewerId 현재 목록을 보는 사람 (맞팔 여부 확인용)
     */
    List<FollowListDto> getFollowings(@Param("targetId") Long targetId, @Param("viewerId") Long viewerId);
}