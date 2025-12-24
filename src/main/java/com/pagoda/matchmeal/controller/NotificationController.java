package com.pagoda.matchmeal.controller;

import com.pagoda.matchmeal.common.response.CommonResponse;
import com.pagoda.matchmeal.common.util.ApiResponseUtil;
import com.pagoda.matchmeal.model.dto.NotificationDto;
import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/notification")
    public CommonResponse<List<NotificationDto>> getMyNotifications(@AuthenticationPrincipal UserDto userDto) {
        return ApiResponseUtil.success(notificationService.getMyNotifications(userDto.getId()));
    }

    @GetMapping("/notification/unread-count")
    public CommonResponse<Integer> getUnreadNotifications(@AuthenticationPrincipal UserDto userDto) {
        return ApiResponseUtil.success(notificationService.getUnreadCount(userDto.getId()));
    }

    @PatchMapping("/notification/{notificationId}/read")
    public CommonResponse<Void> markAsRead(
            @PathVariable("notificationId") Long notificationId,
            @AuthenticationPrincipal UserDto userDto
    ) {
        notificationService.markAsRead(notificationId);
        return ApiResponseUtil.success();
    }

    @DeleteMapping("/notification")
    public CommonResponse<Void> deleteNotification(@AuthenticationPrincipal UserDto userDto) {
        notificationService.deleteAll(userDto.getId());
        return ApiResponseUtil.success();
    }
}
