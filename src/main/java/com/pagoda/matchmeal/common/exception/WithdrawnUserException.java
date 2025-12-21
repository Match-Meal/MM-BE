package com.pagoda.matchmeal.common.exception;

import lombok.Getter;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

@Getter
public class WithdrawnUserException extends OAuth2AuthenticationException {

    private final String socialId;
    private final String email;
    private final String platform;

    public WithdrawnUserException(String socialId, String email, String platform) {
        super(new OAuth2Error("WITHDRAWN_USER"));
        this.socialId = socialId;
        this.email = email;
        this.platform = platform;
    }
}
