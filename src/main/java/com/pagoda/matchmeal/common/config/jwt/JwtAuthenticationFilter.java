package com.pagoda.matchmeal.common.config.jwt;

import com.pagoda.matchmeal.model.dto.UserDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 인증 필터
 * 모든 HTTP 요청에 대해 헤더의 토큰을 검사하고, 유효한 토큰일 경우 SecurityContext에 인증 정보를 등록함
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 필터 로직 수행
     * 1. Request Header에서 토큰 추출
     * 2. 토큰 유효성 검증
     * 3. 유효 시 사용자 정보를 추출하여 SecurityContextHolder에 저장
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // Request Header에서 토큰 추출
        String token = resolveToken(request);

        // validationToken으로 토큰 유효성 검사
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            // 토큰에서 socialId 가져오기
            UserDto userDto = jwtTokenProvider.getUserDto(token);

            // SecurityContext에 인증 정보 저장
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDto,
                            null,
                            Collections.singleton(new SimpleGrantedAuthority(userDto.getRole()))
                    );

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Request Header에서 JWT 추출
     * "Authorization: Bearer <token>" 형태에서 "Bearer " 접두어를 제거하고 토큰만 반환
     *
     * @param request HttpServletRequest
     * @return 토큰 문자열 (없으면 null)
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
