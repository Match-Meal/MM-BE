package com.pagoda.matchmeal.controller;

import com.pagoda.matchmeal.common.response.CommonResponse;
import com.pagoda.matchmeal.common.util.ApiResponseUtil;
import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.model.dto.response.BadgeResponseDto;
import com.pagoda.matchmeal.model.enums.BadgeCategory;
import com.pagoda.matchmeal.service.BadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/badges")
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeService badgeService;

    @GetMapping("/my")
    public CommonResponse<Map<BadgeCategory, List<BadgeResponseDto>>> getMyBadges(
            @AuthenticationPrincipal UserDto userDto
    ) {
        Map<BadgeCategory, List<BadgeResponseDto>> result = badgeService.getMyBadges(userDto.getId());
        return ApiResponseUtil.success(result);
    }
}
