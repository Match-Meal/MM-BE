package com.pagoda.matchmeal.handler;

import com.pagoda.matchmeal.config.jwt.JwtTokenProvider;
import com.pagoda.matchmeal.config.oauth.OAuth2SuccessHandler;
import com.pagoda.matchmeal.mapper.UserMapper;
import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.model.entity.User;
import com.pagoda.matchmeal.model.enums.UserRole;
import com.pagoda.matchmeal.model.enums.UserStatus;
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

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class OAuth2SuccessHandlerTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserMapper userMapper;

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
        // [핵심] @InjectMocks 대신 직접 생성자를 호출하여 Mock 객체를 주입합니다.
        // 이렇게 하면 "Zero interactions" 문제나 주입 실패 문제를 원천 차단할 수 있습니다.
        oAuth2SuccessHandler = new OAuth2SuccessHandler(jwtTokenProvider, userMapper);
    }

    @Test
    @DisplayName("로그인 성공 시 DB 조회 후 토큰을 생성하고 리다이렉트 한다")
    void successHandlerTest() throws Exception {
        // --- Given (상황 설정) ---
        String socialId = "google_12345";
        String generatedToken = "eyJhbGciOiJIUzI1NiJ9.testToken";
        boolean isNewUser = true;

        // 1. Authentication에서 OAuth2User 추출 설정
        given(authentication.getPrincipal()).willReturn(oAuth2User);
        given(oAuth2User.getAttributes()).willReturn(Map.of("sub", socialId, "isNew", isNewUser));

        // 2. DB 조회 Mocking (User -> DTO 변환을 위해 필수)
        // @SuperBuilder를 사용했으므로 부모 필드(createdAt)도 빌더로 설정 가능
        User mockUser = User.builder()
                .id(1L)
                .socialId(socialId)
                .email("test@test.com")
                .userName("테스트유저")
                .role(UserRole.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now()) // 핸들러에서 toString() 호출하므로 null이면 안됨
                .build();

        given(userMapper.findBySocialId(socialId)).willReturn(Optional.of(mockUser));

        // 3. 토큰 생성 Mocking
        // 핸들러가 createAccessToken(UserDto)를 호출하므로 매처를 any(UserDto.class)로 설정
        given(jwtTokenProvider.createAccessToken(any(UserDto.class)))
                .willReturn(generatedToken);

//        // 4. 리다이렉트 URL 생성 과정 Mocking (NPE 방지)
//        // request.getContextPath() 가 null을 반환하면 에러가 날 수 있음
//        given(request.getContextPath()).willReturn("");

        // response.encodeRedirectURL() 이 null을 반환하면 에러가 남 -> 들어온 URL 그대로 반환하게 설정
        given(response.encodeRedirectURL(anyString()))
                .willAnswer(invocation -> invocation.getArgument(0));

        // --- When (실행) ---
        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, authentication);

        // --- Then (검증) ---
        // 1. UserMapper가 호출되었는지 확인
        verify(userMapper).findBySocialId(socialId);

        // 2. JwtTokenProvider가 호출되었는지 확인
        verify(jwtTokenProvider).createAccessToken(any(UserDto.class));

        // 3. 최종적으로 프론트엔드 URL로 리다이렉트 되는지 확인
        verify(response).sendRedirect(argThat(url ->
                url.startsWith("http://localhost:5173/oauth/callback") &&
                url.contains("accessToken=" + generatedToken) &&
                url.contains("isNew=" + isNewUser)
        ));
    }
}