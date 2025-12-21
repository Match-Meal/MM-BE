package com.pagoda.matchmeal.service;

import com.pagoda.matchmeal.common.exception.CustomException;
import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import com.pagoda.matchmeal.common.exception.WithdrawnUserException;
import com.pagoda.matchmeal.model.entity.User;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final AuthService authService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // 구글 등의 서비스 구분 (google, kakao)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // 로그인 진행 시 키가 되는 필드 값 (PK)
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();

        // 유저 정보 가져오기(google)
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String socialId = extractSocialId(registrationId, attributes);

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture");

        User user;
        boolean isNew;

        try {
            // resultType = null로 1차 시도
            // 탈퇴 유저인 경우 CustomException(USER_WITHDRAWN_WAITING)이 발생
            Map<String, Object> result = authService.processLoginOrRegister(socialId, email, name, registrationId, picture, null);

            user = (User) result.get("user");
            isNew = (boolean) result.get("isNew");
        } catch (CustomException e) {
            // 탈퇴 대기 유저 발생 시
            if (e.getCode() == ErrorResponseCode.USER_WITHDRAWN_WAITING) {
                throw new WithdrawnUserException(socialId, email, registrationId);
            }
            throw new OAuth2AuthenticationException(new OAuth2Error("SERVER_ERROR"), e);
        }

        // 정상 로그인 성공 시 (Active 유저 or 신규 유저)
        Map<String, Object> newAttributes = new HashMap<>(attributes);
        newAttributes.put("isNew", isNew);
        newAttributes.put("userId", user.getUserId());


        // 결과 반환
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().name())),
                        newAttributes,
                        userNameAttributeName
        );
    }

    // Helper Method
    private String extractSocialId(String registrationId, Map<String, Object> attributes) {
        if ("google".equalsIgnoreCase(registrationId)) {
            return String.valueOf(attributes.get("sub"));
        } else if ("kakao".equalsIgnoreCase(registrationId)) {
            // 카카오는 id가 Long 타입으로 옴 -> String 변환 필수
            return String.valueOf(attributes.get("id"));
        } else if ("naver".equalsIgnoreCase(registrationId)) {
            Map<String, Object> response = (Map<String, Object>) attributes.get("response");
            return String.valueOf(response.get("id"));
        }
        throw new OAuth2AuthenticationException("지원하지 않는 소셜 플랫폼입니다: " + registrationId);
    }
}
