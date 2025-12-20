package com.pagoda.matchmeal.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pagoda.matchmeal.common.config.jwt.JwtAuthenticationFilter;
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
                        .requestMatchers("/user/**").hasRole("USER") // 일반 유저
                        .requestMatchers("/", "/css/**", "/images/**", "/js/**", "/login/**").permitAll()
                        .requestMatchers("/h2-console/**", "/test/**").permitAll()
                        .anyRequest().authenticated()
                )

                // OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        // 로그인 성공
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        // 성공 후 핸들러
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 401 Unauthorized: 인증되지 않은 사용자 (토큰 없음, 만료됨 등)
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

    // 403 Forbidden: 권한 없는 사용자 (ROLE_WITHDRAWN이 일반 API 접근 시 등)
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
