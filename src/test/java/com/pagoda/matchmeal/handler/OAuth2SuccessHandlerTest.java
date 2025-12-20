package com.pagoda.matchmeal.handler;

import com.pagoda.matchmeal.common.config.jwt.JwtTokenProvider;
import com.pagoda.matchmeal.common.config.oauth.OAuth2SuccessHandler;
import com.pagoda.matchmeal.mapper.UserMapper;
import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.model.entity.User;
import com.pagoda.matchmeal.model.enums.UserRole;
import com.pagoda.matchmeal.service.RedisService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class OAuth2SuccessHandlerTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RedisService redisService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2User oAuth2User;

    // 테스트 대상 (생성자를 통해 수동 주입)
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    @BeforeEach
    void setUp() {
        // 생성자 주입
        oAuth2SuccessHandler = new OAuth2SuccessHandler(jwtTokenProvider, userMapper, redisService);
        // @Value("${cors.url}") 값을 수동으로 주입
        ReflectionTestUtils.setField(oAuth2SuccessHandler, "corsUrl", "http://localhost:5173");
    }

    @Test
    @DisplayName("로그인 성공 시 DB 조회 후 토큰을 생성하고 리다이렉트 한다")
    void successHandlerTest() throws Exception {
        // --- Given (상황 설정) ---
        Long userId = 1L;
        String accessToken = "access.token.test";
        String refreshToken = "refresh.token.test";
        boolean isNewUser = true;

        // 1. Authentication에서 OAuth2User 추출 설정
        given(authentication.getPrincipal()).willReturn(oAuth2User);
        given(oAuth2User.getAttributes()).willReturn(Map.of(
                "userId", userId,  // ★ 중요: 이제 handler는 userId를 꺼내씀
                "isNew", isNewUser
        ));

        // 2. DB 조회 Mocking (User -> DTO 변환을 위해 필수)
        // @SuperBuilder를 사용했으므로 부모 필드(createdAt)도 빌더로 설정 가능
        User mockUser = User.builder()
                .userId(userId)
                .socialId("google_123")
                .role(UserRole.ROLE_USER)
                .createdAt(LocalDateTime.now())
                .build();
        given(userMapper.findById(userId)).willReturn(Optional.of(mockUser));

        // 토큰 생성 Mocking
        given(jwtTokenProvider.createAccessToken(any(UserDto.class))).willReturn(accessToken);
        given(jwtTokenProvider.createRefreshToken(userId)).willReturn(refreshToken);
        given(jwtTokenProvider.getRefreshTokenValidityInMilliseconds()).willReturn(10000L); // 10초


        // response.encodeRedirectURL() 이 null을 반환하면 에러가 남 -> 들어온 URL 그대로 반환하게 설정
        given(response.encodeRedirectURL(anyString())).willAnswer(i -> i.getArgument(0));

        // --- When (실행) ---
        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, authentication);

        // --- Then (검증) ---
        // 1. DB 조회 확인
        verify(userMapper).findById(userId);

        // 2. Redis 저장 확인 (Refresh Token)
        verify(redisService).setValues(
                eq("RT:" + userId),
                eq(refreshToken),
                any(Duration.class)
        );

        // 3. 리다이렉트 URL에 AccessToken과 RefreshToken이 모두 포함되는지 확인
        verify(response).sendRedirect(argThat(url ->
                url.startsWith("http://localhost:5173/oauth/callback") &&
                        url.contains("accessToken=" + accessToken) &&
                        url.contains("refreshToken=" + refreshToken) &&
                        url.contains("isNew=" + isNewUser)
        ));
    }
}