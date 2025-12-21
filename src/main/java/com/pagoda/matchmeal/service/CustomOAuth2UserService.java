package com.pagoda.matchmeal.service;

import com.pagoda.matchmeal.common.exception.CustomException;
import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import com.pagoda.matchmeal.common.exception.WithdrawnUserException;
import com.pagoda.matchmeal.model.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final AuthService authService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. 기본 OAuth2User 정보 가져오기
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // 2. 어떤 소셜인지 구분 (google, kakao)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // 3. 소셜별 데이터 추출 (메서드 분리)
        Map<String, Object> attributes = oAuth2User.getAttributes();
        OAuth2UserInfo userInfo = extractUserInfo(registrationId, attributes);

        String socialId = userInfo.socialId;
        String email = userInfo.email;
        String name = userInfo.name;
        String picture = userInfo.picture;

        log.info("[OAuth2 Login] Provider: {}, SocialId: {}, Email: {}", registrationId, socialId, email);

        User user;
        boolean isNew;

        try {
            // 4. 회원가입 또는 로그인 처리 (AuthService 위임)
            Map<String, Object> result = authService.processLoginOrRegister(socialId, email, name, registrationId, picture, null);
            user = (User) result.get("user");
            isNew = (boolean) result.get("isNew");

        } catch (CustomException e) {
            // 탈퇴 대기 유저 처리
            if (e.getCode() == ErrorResponseCode.USER_WITHDRAWN_WAITING) {
                throw new WithdrawnUserException(socialId, email, registrationId);
            }
            throw new OAuth2AuthenticationException(new OAuth2Error("SERVER_ERROR"), e);
        }

        // 5. 시큐리티 세션에 저장할 정보 구성
        Map<String, Object> newAttributes = new HashMap<>(attributes);
        newAttributes.put("isNew", isNew);
        newAttributes.put("userId", user.getUserId());
        newAttributes.put("email", email);

        // userNameAttributeName (google은 "sub", kakao는 "id")
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().name())),
                newAttributes,
                userNameAttributeName
        );
    }

    // [핵심] 카카오 vs 구글 데이터 추출 로직
    private OAuth2UserInfo extractUserInfo(String registrationId, Map<String, Object> attributes) {
        if ("google".equalsIgnoreCase(registrationId)) {
            return new OAuth2UserInfo(
                    String.valueOf(attributes.get("sub")),
                    (String) attributes.get("email"),
                    (String) attributes.get("name"),
                    (String) attributes.get("picture")
            );
        } else if ("kakao".equalsIgnoreCase(registrationId)) {
            // 1. root에서 socialId 꺼내기 (JSON의 "id")
            String socialId = String.valueOf(attributes.get("id"));

            // 2. "kakao_account" 꺼내기 (이메일, 프로필 정보가 여기 있음)
            Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");

            // 3. "profile" 꺼내기 (닉네임, 이미지가 여기 있음)
            Map<String, Object> profile = (account != null) ? (Map<String, Object>) account.get("profile") : null;

            // 4. 데이터 추출 (없을 경우 빈 문자열 처리하여 에러 방지)
            String email = (account != null && account.get("email") != null)
                    ? (String) account.get("email")
                    : ""; // 이메일 동의 안 했을 경우 대비

            String name = (profile != null && profile.get("nickname") != null)
                    ? (String) profile.get("nickname")
                    : "";

            String picture = (profile != null && profile.get("profile_image_url") != null)
                    ? (String) profile.get("profile_image_url")
                    : "";

            return new OAuth2UserInfo(socialId, email, name, picture);
        }

        throw new OAuth2AuthenticationException("지원하지 않는 소셜 플랫폼입니다: " + registrationId);
    }

    // 데이터 전달용 내부 클래스 (DTO)
    private static class OAuth2UserInfo {
        String socialId;
        String email;
        String name;
        String picture;

        public OAuth2UserInfo(String socialId, String email, String name, String picture) {
            this.socialId = socialId;
            this.email = email;
            this.name = name;
            this.picture = picture;
        }
    }
}