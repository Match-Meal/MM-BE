package com.pagoda.matchmeal.config.oauth;

import com.pagoda.matchmeal.common.exception.CustomException;
import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import com.pagoda.matchmeal.config.jwt.JwtTokenProvider;
import com.pagoda.matchmeal.mapper.UserMapper;
import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.model.entity.User;
import com.pagoda.matchmeal.model.enums.UserRole;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // CustomOAuth2UserService에서 넘긴 isNew 값 추출
        boolean isNew = Boolean.TRUE.equals(oAuth2User.getAttributes().get("isNew"));
        String socialId = (String) oAuth2User.getAttributes().get("sub");

        // 1. DB에서 최신 유저 정보 조회
        User user = userMapper.findBySocialId(socialId)
                .orElseThrow(() -> new CustomException(ErrorResponseCode.USER_NOT_FOUND));

        // 2. DTO 변환
        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .socialId(user.getSocialId())
                .userName(user.getUserName())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .build();

        // 토큰 생성 (socialId와 Role)
        String accessToken = jwtTokenProvider.createAccessToken(userDto);

        // 개발용
        System.out.println("=========================================");
        System.out.println("TEST ACCESS TOKEN: " + accessToken);
        System.out.println("=========================================");

        // redirect
        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:5173/oauth/callback")
                .queryParam("accessToken", accessToken)
                .queryParam("isNew", isNew)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
