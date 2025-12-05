package com.pagoda.matchmeal.service;

import com.pagoda.matchmeal.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserService userService;

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
        String socialId = (String) attributes.get("sub");
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");

        // 핵심 비즈니스 로직
        Map<String, Object> result = userService.processLoginOrRegister(socialId, email, name, registrationId);

        User user = (User) result.get("user");
        boolean isNew = (boolean) result.get("isNew");

        Map<String, Object> newAttributes = new HashMap<>(attributes);
        newAttributes.put("isNew", isNew);

        // 결과 반환
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().name())),
                        newAttributes,
                        userNameAttributeName
        );

    }
}
