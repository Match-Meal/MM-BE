package com.pagoda.matchmeal.service;

import com.pagoda.matchmeal.common.config.jwt.JwtTokenProvider;
import com.pagoda.matchmeal.common.exception.CustomException;
import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import com.pagoda.matchmeal.mapper.UserMapper;
import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.model.entity.User;
import com.pagoda.matchmeal.model.enums.UserRole;
import com.pagoda.matchmeal.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private UserMapper userMapper;
    @Mock private RedisService redisService;
    @Mock private UserService userService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("신규 회원이면 save가 호출되어야 함 (기존 UserServiceTest에서 이동)")
    void processLoginNewUserTest() {
        // given
        String socialId = "12345";
        String email = "test@gmail.com";
        String pictureUrl = "https://google.com/picture.jpg";

        given(userMapper.findBySocialId(socialId)).willReturn(Optional.empty());

        // when
        Map<String, Object> result = authService.processLoginOrRegister(socialId, email, "테스트유저", "google", pictureUrl, null);

        // then
        verify(userMapper).save(argThat(user ->
                user.getProfileImage().equals(pictureUrl)
        ));
        assertThat(result.get("isNew")).isEqualTo(true);
    }

    @Test
    @DisplayName("토큰 재발급 성공 (RTR 적용)")
    void reissueToken_Success() {
        // given
        String oldRefreshToken = "old_refresh_token";
        String userIdStr = "1";
        Long userId = 1L;

        // 1. 토큰 유효성 및 파싱
        given(jwtTokenProvider.validateToken(oldRefreshToken)).willReturn(true);
        given(jwtTokenProvider.getSubject(oldRefreshToken)).willReturn(userIdStr);

        // 2. Redis 조회
        given(redisService.getValues("RT:" + userId)).willReturn(oldRefreshToken);

        // 3. 유저 조회
        User user = User.builder().userId(userId).role(UserRole.ROLE_USER).build();
        given(userMapper.findById(userId)).willReturn(Optional.of(user));

        // 4. 새 토큰 생성 Mocking
        given(userService.convertUserToDto(user)).willReturn(new UserDto());
        given(jwtTokenProvider.createAccessToken(any())).willReturn("new_access");
        given(jwtTokenProvider.createRefreshToken(userId)).willReturn("new_refresh");

        // when
        Map<String, String> result = authService.reissueToken(oldRefreshToken);

        // then
        assertThat(result.get("accessToken")).isEqualTo("new_access");
        assertThat(result.get("refreshToken")).isEqualTo("new_refresh");

        // Redis 업데이트 확인
        verify(redisService).setValues(eq("RT:" + userId), eq("new_refresh"), any());
    }

    @Test
    @DisplayName("Redis에 저장된 토큰과 다르면(탈취 의심) 예외 발생 및 Redis 삭제")
    void reissueToken_Mismatch() {
        // given
        String requestToken = "stolen_token";
        String realToken = "real_token";
        Long userId = 1L;

        given(jwtTokenProvider.validateToken(requestToken)).willReturn(true);
        given(jwtTokenProvider.getSubject(requestToken)).willReturn("1");
        given(redisService.getValues("RT:1")).willReturn(realToken); // Redis엔 다른게 저장됨

        // when & then
        assertThatThrownBy(() -> authService.reissueToken(requestToken))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorResponseCode.UNAUTHORIZED);

        // Redis 삭제 호출 확인 (보안 조치)
        verify(redisService).deleteValues("RT:1");
    }

    @Test
    @DisplayName("로그아웃 시 Redis에서 토큰 삭제")
    void logoutTest() {
        Long userId = 1L;
        authService.logout(userId);
        verify(redisService).deleteValues("RT:" + userId);
    }
}