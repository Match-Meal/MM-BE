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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * OAuth2 로그인 실패 핸들러
 * - 소셜 로그인 과정에서 예외 발생 시 실행됨
 * - 단순 실패와 '탈퇴한 유저의 재로그인 시도'를 구분하여 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 인증 실패 시 호출되는 메서드
     *
     * @param request   HttpServletRequest
     * @param response  HttpServletResponse
     * @param exception 발생한 인증 예외
     */
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        // 기본 타겟: 로그인 페이지 (에러 파라미터 포함)
        String targetUrl = "http://localhost:5173/login?error=unknown";

        try {
            log.info("OAuth2 로그인 실패 감지. 발생 예외: {}", exception.getClass().getName());

            // 1. 탈퇴 유저 예외인지 확인하는 로직 (핵심!)
            // Spring Security는 종종 Custom Exception을 OAuth2AuthenticationException 등으로 감싸서 던집니다.
            WithdrawnUserException withdrawnEx = findWithdrawnException(exception);

            if (withdrawnEx != null) {
                log.info(">>> 탈퇴 대기 유저 식별됨: {}", withdrawnEx.getSocialId());

                // 2. 데이터 Null 방어 로직
                String socialId = withdrawnEx.getSocialId();
                String email = withdrawnEx.getEmail() != null ? withdrawnEx.getEmail() : "";
                String platform = withdrawnEx.getPlatform();

                // 3. 임시 토큰 발급
                String tempToken = jwtTokenProvider.createTemporaryToken(socialId, email, platform);

                // 4. 프론트엔드 복구 페이지로 리다이렉트 주소 생성
                // "탈퇴한 계정입니다..." -> "%ED%83%88%ED%9A%8C%ED%95%9C..." 로 변환됨
                String message = URLEncoder.encode("탈퇴한 계정입니다. 복구하시겠습니까?", StandardCharsets.UTF_8);

                targetUrl = UriComponentsBuilder.fromUriString("http://localhost:5173/oauth/callback")
                        .queryParam("status", "withdrawn")
                        .queryParam("tempToken", tempToken)
                        .queryParam("message", message) // 인코딩된 메시지 넣기
                        .build().toUriString();

            } else {
                // 일반적인 로그인 실패 (구글 서버 오류 등)
                log.error("일반 소셜 로그인 실패: {}", exception.getMessage());
                targetUrl = "http://localhost:5173/login?error=social_login_failed";
            }

        } catch (Exception e) {
            // 핸들러 내부에서 에러가 나더라도 브라우저가 멈추지 않게 로그인 페이지로 보냄
            log.error("OAuth2FailureHandler 내부 시스템 에러", e);
            targetUrl = "http://localhost:5173/login?error=server_error";
        }

        // 5. 최종 리다이렉트 실행 (이게 없으면 화면이 멈춤)
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    // 예외의 껍질을 벗겨서 WithdrawnUserException을 찾는 헬퍼 메서드
    private WithdrawnUserException findWithdrawnException(Throwable ex) {
        if (ex == null) return null;

        // 현재 예외가 탈퇴 예외라면 바로 반환
        if (ex instanceof WithdrawnUserException) {
            return (WithdrawnUserException) ex;
        }

        // 원인(Cause)이 있다면 재귀적으로 확인 (보통 1~2단계 안에 있음)
        return findWithdrawnException(ex.getCause());
    }
}