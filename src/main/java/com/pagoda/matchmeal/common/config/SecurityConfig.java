package com.pagoda.matchmeal.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pagoda.matchmeal.common.config.jwt.JwtAuthenticationFilter;
import com.pagoda.matchmeal.common.config.oauth.HttpCookieOAuth2AuthorizationRequestRepository;
import com.pagoda.matchmeal.common.config.oauth.OAuth2FailureHandler;
import com.pagoda.matchmeal.common.config.oauth.OAuth2SuccessHandler;
import com.pagoda.matchmeal.service.CustomOAuth2UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring Security 설정 클래스
 * - HTTP 보안 설정, CORS, CSRF 비활성화, 세션 미사용(Stateless) 설정
 * - JWT 필터 등록 및 OAuth2 로그인 핸들러 구성
 */
@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;
    private final ObjectMapper objectMapper;

    @Value("${cors.url}")
    private String CORS_ALLOWED_ORIGIN_URL;

    private final HttpCookieOAuth2AuthorizationRequestRepository cookieOAuth2AuthorizationRequestRepository;

    /**
     * SecurityFilterChain 빈 등록
     * 요청 URL별 권한 설정, 예외 처리, OAuth2 로그인, JWT 필터 추가 등 보안 로직을 체이닝 방식으로 구성
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                // CSRF, HTTP Basic 비활성화
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))

                // 세션 사용 x
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // URL 권한 설정
                .authorizeHttpRequests((auth) -> auth
                        .requestMatchers("/user/reactivate").hasRole("WITHDRAWN") // 임시 토큰 가진 사람만 접근 가능
                        .requestMatchers("/user/**").hasAnyRole("USER", "SUBSCRIBER") // 일반 유저 및 구독자
                        .requestMatchers("/ai/**").hasRole("SUBSCRIBER") // ★ AI 기능은 구독자만 접근 가능
                        .requestMatchers("/payment/**").hasAnyRole("USER", "SUBSCRIBER") // 결제 관련 접근 허용
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/css/**", "/images/**", "/js/**", "/login/**", "/favicon.ico", "/error").permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers("/h2-console/**", "/test/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(unauthorizedEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )

                // OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorization -> authorization
                                .authorizationRequestRepository(cookieOAuth2AuthorizationRequestRepository)
                        )
                        // 로그인 성공
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        // 성공 후 핸들러
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 인증되지 않은 사용자(401) 처리 핸들러
     * - 유효한 토큰이 없거나 만료된 경우 JSON 형태의 에러 응답 반환
     */
    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) -> {
            log.warn("Unauthorized Error: {}", authException.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            Map<String, Object> body = new HashMap<>();
            body.put("status", 401);
            body.put("code", "A001"); // ErrorCode Enum과 맞추면 좋음
            body.put("message", "인증되지 않은 사용자입니다.");

            objectMapper.writeValue(response.getOutputStream(), body);
        };
    }

    /**
     * 접근 권한이 없는 사용자(403) 처리 핸들러
     * - 로그인했으나 해당 리소스에 대한 접근 권한(Role)이 부족한 경우 JSON 에러 응답 반환
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            log.warn("Access Denied: {}", accessDeniedException.getMessage());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            Map<String, Object> body = new HashMap<>();
            body.put("status", 403);
            body.put("code", "A002");
            body.put("message", "접근 권한이 없습니다.");

            objectMapper.writeValue(response.getOutputStream(), body);
        };
    }

    /**
     * CORS 설정 빈
     * - 프론트엔드 도메인에서의 요청 허용, 헤더 및 메서드 허용, 자격 증명(Cookie 등) 허용 설정
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.addAllowedOrigin(CORS_ALLOWED_ORIGIN_URL);

        // 모든 헤더와 메서드 허용
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");

        // 쿠키나 인증 정보 포함 허용
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
