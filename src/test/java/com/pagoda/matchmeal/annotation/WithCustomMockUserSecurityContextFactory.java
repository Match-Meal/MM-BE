package com.pagoda.matchmeal.annotation;

import com.pagoda.matchmeal.model.dto.UserDto;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.Collections;

public class WithCustomMockUserSecurityContextFactory implements WithSecurityContextFactory<WithCustomMockUser> {

    @Override
    public SecurityContext createSecurityContext(WithCustomMockUser annotation) {
        // 실제 컨트롤러가 사용하는 UserDto 객체 생성
        UserDto userDto = new UserDto();
        userDto.setId(annotation.userId());
        userDto.setEmail(annotation.email());
        userDto.setRole(annotation.role());

        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDto, // Principal에 UserDto 주입
                "",
                Collections.emptyList()
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        return context;
    }
}