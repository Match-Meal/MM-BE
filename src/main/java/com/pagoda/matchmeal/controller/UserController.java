package com.pagoda.matchmeal.controller;

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
     * @param socialId
     * @return
     */
    @GetMapping("/me")
    public ResponseEntity<User> getMyInfo(@AuthenticationPrincipal String socialId) {
        // @AuthenticationPrincipal
        // JwtAuthenticationFilter에서 SecurityContext에 넣어둔
        // UsernamePasswordAuthenticationToken의 첫 번째 인자(Principal)를 가져옴
        // socialId을 가져움

        if (socialId == null) {
            return ResponseEntity.status(401).build(); // 인증 실패
        }

        // Service를 통해 DB에서 유저 정보 조회
        User user = userService.findBySocialId(socialId);

        if (user != null) {
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

}
