package com.pagoda.matchmeal.controller;

import com.pagoda.matchmeal.common.response.CommonResponse;
import com.pagoda.matchmeal.common.util.ApiResponseUtil;
import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.model.dto.response.FollowListDto;
import com.pagoda.matchmeal.model.dto.response.FollowResponseDto;
import com.pagoda.matchmeal.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    /**
     * 팔로우/언팔로우 토글
     * @param userDto
     * @param targetId
     */
    @PostMapping("/{targetId}/follow")
    public ResponseEntity<FollowResponseDto> toggleFollow(
            @AuthenticationPrincipal UserDto userDto,
            @PathVariable Long targetId
    ) {
        if (userDto == null) {
            return ResponseEntity.status(401).build();
        }
        FollowResponseDto response = followService.toggleFollow(userDto.getId(), targetId);
        return ResponseEntity.ok(response);
    }

    /**
     * 팔로워 목록 조회
     * @param userDto
     * @param userId
     */
    @GetMapping("/{userId}/followers")
    public CommonResponse<List<FollowListDto>> getFollowers(
            @AuthenticationPrincipal UserDto userDto, // 조회하는 사람
            @PathVariable Long userId
    ) {

        Long viewerId = (userDto != null) ? userDto.getId() : null;
//        System.out.println("====== [Followers] viewerId: " + viewerId + " ======");
        List<FollowListDto> list = followService.getFollowers(userId, viewerId);
        return ApiResponseUtil.success(list);
    }

    /**
     * 팔로잉 목록 조회
     * @param userDto
     * @param userId
     */
    @GetMapping("/{userId}/followings")
    public CommonResponse<List<FollowListDto>> getFollowings(
            @AuthenticationPrincipal UserDto userDto, // 조회하는 사람
            @PathVariable Long userId
    ) {
        Long viewerId = (userDto != null) ? userDto.getId() : null;
//        System.out.println("====== [DEBUG] 요청자 ID (viewerId): " + viewerId + " ======");
        List<FollowListDto> list = followService.getFollowings(userId, viewerId);
        return ApiResponseUtil.success(list);
    }
}
