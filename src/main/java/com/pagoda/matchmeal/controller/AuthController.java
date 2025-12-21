package com.pagoda.matchmeal.controller;

import com.pagoda.matchmeal.common.response.CommonResponse;
import com.pagoda.matchmeal.common.util.ApiResponseUtil;
import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 토큰 재발급
     * Header에 "RefreshToken"이라는 키로 토큰을 담아 보낸다고 가정
     */
    @PostMapping("/reissue")
    public CommonResponse<Map<String, String>> reissue(
            @RequestHeader("RefreshToken") String refreshToken
    ) {
        Map<String, String> tokens = authService.reissueToken(refreshToken);
        return ApiResponseUtil.success(tokens);
    }

    /**
     * 로그아웃
     * Access Token을 통해 유저를 식별하고, Redis의 Refresh Token을 삭제함
     */
    @PostMapping("/logout")
    public CommonResponse<Void> logout(@AuthenticationPrincipal UserDto userDto) {
        authService.logout(userDto.getId());
        SecurityContextHolder.clearContext();
        return ApiResponseUtil.success();
    }
}
