package com.pagoda.matchmeal.common.config.oauth;

import com.pagoda.matchmeal.common.util.CookieUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * OAuth2 인증 요청(Authorization Request)을 쿠키에 저장/검색하는 저장소
 * - 세션을 사용하지 않는 Stateless 환경(REST API)에서 OAuth2 흐름을 유지하기 위함
 * - 인증 요청 정보와 리다이렉트 URI를 쿠키에 임시 저장
 */
@Component
public class HttpCookieOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    public static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";
    private static final int cookieExpireSeconds = 180;

    /**
     * 쿠키에서 인증 요청 정보 로드
     *
     * @param request HttpServletRequest
     * @return 복원된 OAuth2AuthorizationRequest 객체 (없으면 null)
     */
    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return CookieUtils.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
                .map(cookie -> CookieUtils.deserialize(cookie, OAuth2AuthorizationRequest.class))
                .orElse(null);
    }

    /**
     * 인증 요청 정보를 쿠키에 저장
     * 인증 후 돌아갈 redirect_uri도 함께 쿠키에 저장함
     *
     * @param authorizationRequest 저장할 인증 요청 정보
     * @param request              HttpServletRequest
     * @param response             HttpServletResponse
     */
    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeAuthorizationRequest(request, response);
            return;
        }

        CookieUtils.addCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, CookieUtils.serialize(authorizationRequest), cookieExpireSeconds);
        String redirectUriAfterLogin = request.getParameter(REDIRECT_URI_PARAM_COOKIE_NAME);
        if (StringUtils.hasText(redirectUriAfterLogin)) {
            CookieUtils.addCookie(response, REDIRECT_URI_PARAM_COOKIE_NAME, redirectUriAfterLogin, cookieExpireSeconds);
        }
    }

    /**
     * 인증 요청 정보를 쿠키에서 제거하고 반환
     * (인증 완료 후 정리 작업 시 호출됨)
     *
     * @param request  HttpServletRequest
     * @param response HttpServletResponse
     * @return 삭제 전 로드한 OAuth2AuthorizationRequest
     */
    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        OAuth2AuthorizationRequest authorizationRequest = this.loadAuthorizationRequest(request);
        removeAuthorizationRequestCookies(request, response); // 쿠키 삭제 실행
        return authorizationRequest;
    }

    /**
     * 인증 관련 쿠키 일괄 삭제 헬퍼 메서드
     *
     * @param request  HttpServletRequest
     * @param response HttpServletResponse
     */
    public void removeAuthorizationRequestCookies(HttpServletRequest request, HttpServletResponse response) {
        CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        CookieUtils.deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
    }
}
