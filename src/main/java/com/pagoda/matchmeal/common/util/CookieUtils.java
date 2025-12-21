package com.pagoda.matchmeal.common.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.SerializationUtils;

import java.util.Base64;
import java.util.Optional;

/**
 * HTTP 쿠키 관리 유틸리티 클래스
 * - 쿠키의 생성, 조회, 삭제 기능 제공
 * - 객체(Object)를 쿠키에 저장하기 위한 직렬화/역직렬화(Base64) 기능 포함
 * - 주로 OAuth2 인증 시 Stateless한 환경에서 인증 요청 정보를 유지하기 위해 사용됨
 */
public class CookieUtils {

    /**
     * Request에서 특정 이름의 쿠키를 조회
     *
     * @param request HttpServletRequest
     * @param name    찾을 쿠키의 이름
     * @return 찾은 쿠키 객체 (Optional)
     */
    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return Optional.of(cookie);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * 쿠키를 생성하여 Response에 추가
     * - 보안을 위해 HttpOnly 속성을 true로 설정 (자바스크립트 접근 방지)
     * - Path는 루트("/")로 설정하여 모든 경로에서 접근 가능
     *
     * @param response HttpServletResponse
     * @param name     쿠키 이름
     * @param value    쿠키 값 (문자열)
     * @param maxAge   쿠키 만료 시간 (초 단위)
     */
    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    /**
     * 쿠키 삭제
     * - 동일한 이름의 쿠키를 생성하되, MaxAge를 0으로 설정하여 브라우저가 즉시 삭제하도록 유도
     *
     * @param request  HttpServletRequest
     * @param response HttpServletResponse
     * @param name     삭제할 쿠키 이름
     */
    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                }
            }
        }
    }

    /**
     * 객체 직렬화 (Object -> String)
     * - 쿠키 값에는 문자열만 저장 가능하므로, 자바 객체를 직렬화한 후 Base64 URL Safe 방식으로 인코딩
     *
     * @param object 직렬화할 객체 (Serializable 인터페이스 구현 필요)
     * @return 인코딩된 문자열
     */
    public static String serialize(Object object) {
        return Base64.getUrlEncoder()
                .encodeToString(SerializationUtils.serialize(object));
    }

    /**
     * 쿠키 값 역직렬화 (String -> Object)
     * - Base64 인코딩된 쿠키 값을 디코딩하여 원래의 자바 객체로 복원
     *
     * @param cookie 읽어온 쿠키 객체
     * @param cls    변환할 클래스 타입 (Class<T>)
     * @return 복원된 객체 (T)
     */
    public static <T> T deserialize(Cookie cookie, Class<T> cls) {
        return cls.cast(SerializationUtils.deserialize(
                Base64.getUrlDecoder().decode(cookie.getValue())));
    }
}
