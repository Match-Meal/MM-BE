package com.pagoda.matchmeal.common.config.oauth;

import com.pagoda.matchmeal.common.config.jwt.JwtTokenProvider;
import com.pagoda.matchmeal.common.exception.WithdrawnUserException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        String targetUrl = "http://localhost:5173/login"; // 기본 로그인 페이지

        // 우리가 던진 탈퇴유저 예외인지 확인
        if (exception instanceof WithdrawnUserException) {
            WithdrawnUserException withdrawnEx = (WithdrawnUserException) exception;

            // 임시 토큰 발급(ROLE_WITHDRAWN)
            String tempToken = jwtTokenProvider.createTemporaryToken(
                    withdrawnEx.getSocialId(),
                    withdrawnEx.getEmail(),
                    withdrawnEx.getPlatform()
            );

            log.info("탈퇴 대기 유저 로그인 시도. 임시 토큰 발급 완료. socialId: {}", withdrawnEx.getSocialId());

            // 3. 프론트엔드로 리다이렉트 (status=withdrawn, token=임시토큰)
            targetUrl = UriComponentsBuilder.fromUriString("http://localhost:5173/oauth/callback")
                    .queryParam("status", "withdrawn")
                    .queryParam("tempToken", tempToken)
                    .queryParam("message", "탈퇴한 계정입니다. 복구하시겠습니까?")
                    .build().toUriString();
        } else {
            // 그 외 일반적인 로그인 실패
            log.error("소셜 로그인 실패: {}", exception.getMessage());
            targetUrl = UriComponentsBuilder.fromUriString(targetUrl)
                    .queryParam("error", "true")
                    .queryParam("message", exception.getMessage())
                    .build().toUriString();
        }

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
