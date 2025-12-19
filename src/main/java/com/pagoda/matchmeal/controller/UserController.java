package com.pagoda.matchmeal.controller;

import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import com.pagoda.matchmeal.common.response.CommonResponse;
import com.pagoda.matchmeal.common.util.ApiResponseUtil;
import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.model.dto.UserProfileDto;
import com.pagoda.matchmeal.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 내 정보 조회
     *
     * @param tokenUser
     * @return UserDto
     */
    @GetMapping("/me")
    public CommonResponse<UserDto> getMyInfo(@AuthenticationPrincipal UserDto tokenUser) {
        if (tokenUser == null) {
            return ApiResponseUtil.failure(ErrorResponseCode.UNAUTHORIZED);
        }

        UserDto myProfile = userService.getMyProfile(tokenUser.getId());

        return ApiResponseUtil.success(myProfile);
    }

    @GetMapping("/{userId}")
    public CommonResponse<UserDto> getUserProfile(@PathVariable Long userId) {
        UserDto userProfile = userService.getUserProfile(userId);
        return ApiResponseUtil.success(userProfile);
    }

    /**
     * 프로필 업데이트
     * Content-Type: multipart/form-data
     *
     * @param userDto
     * @param profileDto
     * @param file
     * @return
     */
    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResponse<Void> updateProfile(
            @AuthenticationPrincipal UserDto userDto,
            @RequestPart(value = "data") UserProfileDto profileDto,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        userService.updateProfile(userDto.getId(), profileDto, file);
        return ApiResponseUtil.success();
    }

    /**
     * 프로필 공개 여부 설정
     *
     * @param userDto
     * @param request
     * @return
     */
    @PatchMapping("/visibility")
    public CommonResponse<Void> updateVisibility(
            @AuthenticationPrincipal UserDto userDto,
            @RequestBody Map<String, Boolean> request
    ) {
        boolean isPublic = request.get("isPublic");
        userService.updateVisibility(userDto.getId(), isPublic);
        return ApiResponseUtil.success();
    }

}
