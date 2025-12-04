package com.pagoda.matchmeal.controller;

import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.model.entity.User;
import com.pagoda.matchmeal.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 내 정보 조회
     * @param userDto
     * @return UserDto
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getMyInfo(@AuthenticationPrincipal UserDto userDto) {
        // @AuthenticationPrincipal
        // JwtAuthenticationFilter에서 토큰을 파싱하여 만든 UserDto 객체가 주입
        // DB 조회 없이 메모리 상의 객체를 바로 반환

        if (userDto == null) {
            return ResponseEntity.status(401).build(); // 인증 실패
        }

        return ResponseEntity.ok(userDto);
    }

}
