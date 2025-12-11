package com.pagoda.matchmeal.mapper;

import com.pagoda.matchmeal.model.dto.response.FollowListDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FollowMapper {
    // 팔로우 하기
    void insertFollow(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    // 언팔로우 하기
    void deleteFollow(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    // 이미 팔로우 중인지 확인 (1: true, 0: false)
    boolean existsByFollowerAndFollowing(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    // 팔로워 수 조회 (나를 팔로우 하는 사람)
    Long countFollowers(Long userId);

    // 팔로잉 수 조회 (내가 팔로우 하는 사람)
    Long countFollowings(Long userId);

    List<FollowListDto> getFollowers(@Param("targetId") Long targetId, @Param("viewerId") Long viewerId);

    List<FollowListDto> getFollowings(@Param("targetId") Long targetId, @Param("viewerId") Long viewerId);
}