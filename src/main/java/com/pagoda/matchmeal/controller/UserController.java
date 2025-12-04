package com.pagoda.matchmeal.controller;

import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.model.dto.UserProfileDto;
import com.pagoda.matchmeal.model.entity.User;
import com.pagoda.matchmeal.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 내 정보 조회
     * @param tokenUser
     * @return UserDto
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getMyInfo(@AuthenticationPrincipal UserDto tokenUser) {
        if (tokenUser == null) {
            return ResponseEntity.status(401).build();
        }

        UserDto myProfile = userService.getMyProfile(tokenUser.getId());

        return ResponseEntity.ok(myProfile);
    }

    /**
     * 프로필 업데이트
     * @param userDto
     * @param profileDto
     * @return
     */
    public ResponseEntity<Void> updateProfile(
            @AuthenticationPrincipal UserDto userDto,
            @RequestBody UserProfileDto profileDto
    ) {
        userService.updateProfile(userDto.getId(), profileDto);
        return ResponseEntity.ok().build();
    }

}
