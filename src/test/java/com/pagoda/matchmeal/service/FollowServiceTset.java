package com.pagoda.matchmeal.service;

import com.pagoda.matchmeal.common.exception.CustomException;
import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import com.pagoda.matchmeal.mapper.FollowMapper;
import com.pagoda.matchmeal.mapper.UserMapper;
import com.pagoda.matchmeal.model.dto.response.FollowListDto;
import com.pagoda.matchmeal.model.dto.response.FollowResponseDto;
import com.pagoda.matchmeal.model.entity.User;
import com.pagoda.matchmeal.service.impl.FollowServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FollowServiceTset {

    @Mock
    private FollowMapper followMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private FollowServiceImpl followService;

    @Test
    @DisplayName("팔로우 - 이미 관계가 없다면 insert가 호출되어야 한다.")
    void toggleFollow_Insert() {
        // given
        Long followerId = 1L; // 나
        Long followingId = 2L; // 상대방

        // 상대 유저 존재 가정
        given(userMapper.findById(followingId)).willReturn(Optional.of(User.builder().id(followerId).build()));

        // 팔로우 중이 아님(false)
        given(followMapper.existsByFollowerAndFollowing(followerId, followingId)).willReturn(false);

        long expectedTargetFollowerCount = 10L;
        long expectedMyFollowingCount = 5L;

        given(followMapper.countFollowers(followingId)).willReturn(expectedTargetFollowerCount);
        given(followMapper.countFollowings(followerId)).willReturn(expectedMyFollowingCount);

        // when
        FollowResponseDto result = followService.toggleFollow(followerId, followingId);

        // then
        // insertFollow 호출
        verify(followMapper, times(1)).insertFollow(followerId, followingId);
        verify(followMapper, never()).deleteFollow(followerId, followingId);

        // 반환된 DTO 값 검증
        assertThat(result.isFollowing()).isTrue(); // 팔로우 상태여야 함
        assertThat(result.getFollowerCount()).isEqualTo(expectedTargetFollowerCount);
        assertThat(result.getFollowingCount()).isEqualTo(expectedMyFollowingCount);
    }

    @Test
    @DisplayName("언팔로우 - 이미 관계가 있다면 delete가 호출되어야 한다")
    void toggleFollow_Delete() {
        // given
        Long followerId = 1L;
        Long followingId = 2L;

        given(userMapper.findById(followingId)).willReturn(Optional.of(User.builder().id(followingId).build()));

        // 현재 팔로우 중임 (true)
        given(followMapper.existsByFollowerAndFollowing(followerId, followingId)).willReturn(true);

        long expectedTargetFollowerCount = 9L;
        long expectedMyFollowingCount = 4L;

        given(followMapper.countFollowers(followingId)).willReturn(expectedTargetFollowerCount);
        given(followMapper.countFollowings(followerId)).willReturn(expectedMyFollowingCount);

        // when
        FollowResponseDto result = followService.toggleFollow(followerId, followingId);

        // then
        // deleteFollow는 호출되고, insertFollow는 호출되지 않아야 함
        verify(followMapper, times(1)).deleteFollow(followerId, followingId);
        verify(followMapper, never()).insertFollow(followerId, followingId);

        // 반환된 DTO 값 검증
        assertThat(result.isFollowing()).isFalse(); // 언팔로우 상태여야 함
        assertThat(result.getFollowerCount()).isEqualTo(expectedTargetFollowerCount);
        assertThat(result.getFollowingCount()).isEqualTo(expectedMyFollowingCount);
    }

    @Test
    @DisplayName("자기 자신을 팔로우하면 예외가 발생해야 한다.")
    void toggleFollow_self_Exception() {
        // given
        Long myId = 1L;

        // when & then
        assertThatThrownBy(() -> followService.toggleFollow(myId, myId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorResponseCode.SELF_FOLLOW_NOT_ALLOWED);

        // DB 조회나 로직이 실행되지 않아야 함
        verify(userMapper, never()).findById(anyLong());
        verify(followMapper, never()).existsByFollowerAndFollowing(anyLong(), anyLong());
    }

    @Test
    @DisplayName("존재하지 않는 유저를 팔로우하면 예외가 발생해야 한다")
    void toggleFollow_UserNotFound_Exception() {
        // given
        Long followerId = 1L;
        Long followingId = 999L; // 없는 유저

        // 유저 조회 시 empty 반환
        given(userMapper.findById(followingId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> followService.toggleFollow(followerId, followingId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorResponseCode.USER_NOT_FOUND);

        // 팔로우 로직은 실행되지 않아야 함
        verify(followMapper, never()).insertFollow(anyLong(), anyLong());
        
    }

    @Test
    @DisplayName("팔로워 목록 조회 성공")
    void getFollowers_Success() {
        // given
        Long targetId = 2L;
        Long viewerId = 5L;

        // [수정 2] Mapper는 UserDto가 아니라 User(Entity)를 반환합니다.
        // 따라서 Mock 객체도 User 타입으로 생성해야 합니다.
        User mockUser = User.builder()
                .id(targetId)
                .userName("TargetUser")
                .build();

        // given에서 UserDto가 아닌 User 객체를 리턴하도록 설정
        given(userMapper.findById(targetId))
                .willReturn(Optional.of(mockUser));

        List<FollowListDto> mockList = List.of(
                FollowListDto.builder().userId(1L).isFollowing(true).build(),
                FollowListDto.builder().userId(3L).isFollowing(false).build()
        );

        given(followMapper.getFollowers(targetId, viewerId)).willReturn(mockList);

        // when
        List<FollowListDto> result = followService.getFollowers(targetId, viewerId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).isFollowing()).isTrue();

        verify(userMapper).findById(targetId);
        verify(followMapper).getFollowers(targetId, viewerId);
    }

    @Test
    @DisplayName("팔로워 목록 조회 실패 - 대상 유저 없음")
    void getFollowers_Fail_UserNotFound() {
        // given
        Long targetId = 999L;
        Long viewerId = 5L;

        // 유저가 없다고 가정 (Optional.empty 반환)
        given(userMapper.findById(targetId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> followService.getFollowers(targetId, viewerId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorResponseCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("팔로잉 목록 조회 성공")
    void getFollowings_Success() {
        // given
        Long targetId = 2L;
        Long viewerId = 5L;

        // getFollowings는 현재 유저 존재 체크 로직이 없으므로 바로 매퍼 호출 Mocking
        List<FollowListDto> mockList = List.of(
                FollowListDto.builder().userId(10L).userName("User10").isFollowing(true).build()
        );

        given(followMapper.getFollowings(targetId, viewerId)).willReturn(mockList);

        // when
        List<FollowListDto> result = followService.getFollowings(targetId, viewerId);

        // then
        assertThat(result).hasSize(1);
        verify(followMapper).getFollowings(targetId, viewerId);
    }

}
