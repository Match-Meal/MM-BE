package com.pagoda.matchmeal.common.config.oauth;

import com.pagoda.matchmeal.common.config.jwt.JwtTokenProvider;
import com.pagoda.matchmeal.common.exception.CustomException;
import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import com.pagoda.matchmeal.mapper.UserMapper;
import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.model.entity.User;
import com.pagoda.matchmeal.service.RedisService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;
    private final RedisService redisService;

    @Value("${cors.url}")
    private String corsUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // CustomOAuth2UserService에서 넘긴 isNew 값 추출
        boolean isNew = Boolean.TRUE.equals(oAuth2User.getAttributes().get("isNew"));
        Long userId = (Long) oAuth2User.getAttributes().get("userId");

        // 1. DB에서 최신 유저 정보 조회
        User user = userMapper.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorResponseCode.USER_NOT_FOUND));

        // 2. DTO 변환
        UserDto userDto = UserDto.builder()
                .id(user.getUserId())
                .socialId(user.getSocialId())
                .userName(user.getUserName())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : "")
                .build();

        // 토큰 생성 (socialId와 Role)
        String accessToken = jwtTokenProvider.createAccessToken(userDto);
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

        // Redis에 Refresh Token 저장 (Key: "RT:{userId}", Value: refreshToken)
        // 유효기간을 설정하여 자동 만료되도록 함
        long refreshTokenExpirationMillis = jwtTokenProvider.getRefreshTokenValidityInMilliseconds();
        redisService.setValues(
                "RT:" + user.getUserId(),
                refreshToken,
                Duration.ofMillis(refreshTokenExpirationMillis)
        );

        log.info("로그인 성공. Redis 저장 완료. UserID: {}", userId);

        // 개발용
        System.out.println("=========================================");
        System.out.println("TEST ACCESS TOKEN: " + accessToken);
        System.out.println("=========================================");

        // redirect
        String targetUrl = UriComponentsBuilder.fromUriString(corsUrl + "/oauth/callback")
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .queryParam("isNew", isNew)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
