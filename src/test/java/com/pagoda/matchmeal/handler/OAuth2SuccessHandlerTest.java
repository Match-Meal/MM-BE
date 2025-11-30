package com.pagoda.matchmeal.handler;

import com.pagoda.matchmeal.config.jwt.JwtTokenProvider;
import com.pagoda.matchmeal.config.oauth.OAuth2SuccessHandler;
import com.pagoda.matchmeal.model.enums.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class) // Mockito 사용 설정
public class OAuth2SuccessHandlerTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider; // 가짜 토큰 생성기

    @Mock
    private HttpServletRequest request; // 가짜 요청

    @Mock
    private HttpServletResponse response; // 가짜 응답

    @Mock
    private Authentication authentication; // 가짜 인증 객체

    @Mock
    private OAuth2User oAuth2User; // 가짜 소셜 유저 정보

    @InjectMocks
    private OAuth2SuccessHandler oAuth2SuccessHandler; // 테스트 대상

    @Test
    @DisplayName("로그인 성공 시 토큰을 생성하고 리다이렉트 한다")
    void successHandlerTest() throws Exception {
        // given (상황 설정)
        String socialId = "google_12345";
        String generatedToken = "eyJhbGciOiJIUzI1NiJ9.fakeToken"; // 가짜 토큰 값

        // 1. authentication에서 oAuth2User를 꺼내도록 설정
        given(authentication.getPrincipal()).willReturn(oAuth2User);

        // 2. oAuth2User에서 attributes(sub)를 꺼내도록 설정
        given(oAuth2User.getAttributes()).willReturn(Map.of("sub", socialId));

        // 3. jwtTokenProvider가 "가짜 토큰"을 반환하도록 설정
        given(jwtTokenProvider.createAccessToken(anyString(), anyString()))
                .willReturn(generatedToken);

        given(response.encodeRedirectURL(anyString())).willAnswer(invocation -> invocation.getArgument(0));

        // when (실행)
        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, authentication);

        // then (검증)
        // accessToken=generatedToken 을 포함하는지 확인
        verify(response).sendRedirect(argThat(url ->
                url.startsWith("http://localhost:8080/test/token") &&
                        url.contains("accessToken=" + generatedToken)
        ));
    }
}