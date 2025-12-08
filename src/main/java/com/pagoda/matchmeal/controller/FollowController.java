package com.pagoda.matchmeal.controller;

import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<Void> toggleFollow(
            @AuthenticationPrincipal UserDto userDto,
            @PathVariable Long targetId
    ) {
        if (userDto == null) {
            return ResponseEntity.status(401).build();
        }
        followService.toggleFollow(userDto.getId(), targetId);
        return ResponseEntity.ok().build();
    }
}
