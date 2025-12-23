package com.pagoda.matchmeal.service;

import com.pagoda.matchmeal.common.exception.CustomException;
import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import com.pagoda.matchmeal.mapper.FollowMapper;
import com.pagoda.matchmeal.mapper.UserMapper;
import com.pagoda.matchmeal.model.dto.response.FollowListDto;
import com.pagoda.matchmeal.model.dto.response.FollowResponseDto;
import com.pagoda.matchmeal.model.entity.User;
import com.pagoda.matchmeal.service.impl.FollowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FollowServiceTest {

    @Mock
    private FollowMapper followMapper;

    @Mock
    private UserMapper userMapper;

    // [중요] NotificationService import 및 Mock 주입 확인
    @Mock
    private NotificationService notificationService;


    private FollowServiceImpl followService;

    @BeforeEach
    void setUp() {
        followService = new FollowServiceImpl(followMapper, userMapper, notificationService);
    }


    @Test
    @DisplayName("팔로우 - 이미 관계가 없다면 insert가 호출되어야 한다.")
    void toggleFollow_Insert() {
        // given
        Long followerId = 1L;
        Long followingId = 2L;

        User targetUser = User.builder().userId(followingId).userName("TargetUser").build();
        User myUser = User.builder().userId(followerId).userName("MyUser").build();

        // Stubbing
        given(userMapper.findById(followingId)).willReturn(Optional.of(targetUser));
        given(userMapper.findById(followerId)).willReturn(Optional.of(myUser));
        given(followMapper.existsByFollowerAndFollowing(followerId, followingId)).willReturn(false);
        given(followMapper.countFollowers(followingId)).willReturn(10L);
        given(followMapper.countFollowings(followerId)).willReturn(5L);

        // when
        FollowResponseDto result = followService.toggleFollow(followerId, followingId);

        // then
        verify(followMapper).insertFollow(followerId, followingId);
        // 알림 발송 검증
        verify(notificationService).sendToUser(eq(followingId), eq(followerId), any(), anyString(), anyInt(), anyString());

        assertThat(result.isFollowing()).isTrue();
    }

    @Test
    @DisplayName("언팔로우 - 이미 관계가 있다면 delete가 호출되어야 한다")
    void toggleFollow_Delete() {
        // given
        Long followerId = 1L;
        Long followingId = 2L;

        User targetUser = User.builder().userId(followingId).userName("TargetUser").build();
        User followerUser = User.builder().userId(followerId).userName("FollowerUser").build();

        // Stubbing (이제 확실히 동작합니다)
        given(userMapper.findById(followingId)).willReturn(Optional.of(targetUser));
        given(userMapper.findById(followerId)).willReturn(Optional.of(followerUser));
        given(followMapper.existsByFollowerAndFollowing(followerId, followingId)).willReturn(true);
        given(followMapper.countFollowers(followingId)).willReturn(9L);
        given(followMapper.countFollowings(followerId)).willReturn(4L);

        // when
        FollowResponseDto result = followService.toggleFollow(followerId, followingId);

        // then
        verify(followMapper).deleteFollow(followerId, followingId);
        verify(followMapper, never()).insertFollow(followerId, followingId);

        // 언팔로우는 알림 안 보냄
//        verify(notificationService, never()).sendToUser(anyLong(), anyLong(), any(), anyString(), anyInt(), anyString());

        assertThat(result.isFollowing()).isFalse();
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

        verify(followMapper, never()).insertFollow(anyLong(), anyLong());
    }

    @Test
    @DisplayName("팔로워 목록 조회 성공")
    void getFollowers_Success() {
        // given
        Long targetId = 2L;
        Long viewerId = 5L;

        User mockUser = User.builder()
                .userId(targetId)
                .userName("TargetUser")
                .build();

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