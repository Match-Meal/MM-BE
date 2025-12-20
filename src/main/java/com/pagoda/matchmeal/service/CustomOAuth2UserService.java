package com.pagoda.matchmeal.service;

import com.pagoda.matchmeal.common.exception.CustomException;
import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
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
        String picture = (String) attributes.get("picture");

        User user;
        boolean isNew;

        try {
            // resultType = null로 1차 시도
            // 탈퇴 유저인 경우 CustomException(USER_WITHDRAWN_WAITING)이 발생
            Map<String, Object> result = userService.processLoginOrRegister(socialId, email, name, registrationId, picture, null);

            user = (User) result.get("user");
            isNew = (boolean) result.get("isNew");
        } catch (CustomException e) {
            // 탈퇴 유저(복구 대기 상태)인 경우 처리
            if (e.getErrorCode() == ErrorResponseCode.USER_WITHDRAWN_WAITING) {
                // spring security의 인증 실패 흐름을 넘기기 위해 OAuth2AuthenticationException 발생
                // OAuth2Error 코드를 커스텀하게 설정 ("WITHDRAWN_USER")
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("WITHDRAWN_USER", "탈퇴한 회원입니다. 복구 또는 재가입이 필요합니다.", null),
                        e
                );
            }

            // 그 외에는 일반 인증 에러로 처리
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("SERVER_ERROR"), e
            );
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
}
