package com.pagoda.matchmeal.config;

import com.pagoda.matchmeal.config.jwt.JwtAuthenticationFilter;
import com.pagoda.matchmeal.config.oauth.OAuth2SuccessHandler;
import com.pagoda.matchmeal.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                // CSRF, HTTP Basic 비활성화
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))

                // 세션 사용 x
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // URL 권한 설정
                .authorizeHttpRequests((auth) -> auth
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
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
