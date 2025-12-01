package com.pagoda.matchmeal.controller;

import com.pagoda.matchmeal.common.exception.CustomException;
import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import com.pagoda.matchmeal.common.response.CommonResponse;
import com.pagoda.matchmeal.common.response.PageInfoResponseDto;
import com.pagoda.matchmeal.common.util.ApiResponseUtil;
import com.pagoda.matchmeal.model.dto.request.FoodRequestDto;
import com.pagoda.matchmeal.model.dto.response.FoodDetailResponseDto;
import com.pagoda.matchmeal.model.dto.response.FoodListResponseDto;
import com.pagoda.matchmeal.model.entity.User;
import com.pagoda.matchmeal.service.FoodService;
import com.pagoda.matchmeal.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class FoodController {

    private final FoodService foodService;
    private final UserService userService;

    @PostMapping("/foods")
    public CommonResponse<Long> createFood(@AuthenticationPrincipal String socialId, @RequestBody FoodRequestDto foodRequestDto) {
        if (socialId == null) {
            return ApiResponseUtil.failure(new CustomException(ErrorResponseCode.SERVER_ERROR).getCode()); // 인증 실패
        }

        // Service를 통해 DB에서 유저 정보 조회
        User user = userService.findBySocialId(socialId);
        return ApiResponseUtil.created("음식 DB 생성 성공", foodService.addFood(user.getId(), foodRequestDto));
    }

    @GetMapping("/foods")
    public CommonResponse<PageInfoResponseDto<FoodListResponseDto>> getFoodList(
            @AuthenticationPrincipal String socialId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "false") boolean userOnly,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        if (socialId == null) {
            return ApiResponseUtil.failure(new CustomException(ErrorResponseCode.SERVER_ERROR).getCode()); // 인증 실패
        }

        // Service를 통해 DB에서 유저 정보 조회
        User user = userService.findBySocialId(socialId);
        return ApiResponseUtil.success(foodService.getFoodList(user.getId(), keyword, category, userOnly, pageable));
    }

    @GetMapping("/foods/{foodId}")
    public CommonResponse<FoodDetailResponseDto> getFoodDetail(@AuthenticationPrincipal String socialId, @PathVariable("foodId") Long foodId) {
        if (socialId == null) {
            return ApiResponseUtil.failure(new CustomException(ErrorResponseCode.SERVER_ERROR).getCode()); // 인증 실패
        }

        // Service를 통해 DB에서 유저 정보 조회
        User user = userService.findBySocialId(socialId);
        return ApiResponseUtil.success(foodService.getFoodDetail(foodId, user.getId()));
    }

    @PatchMapping("/foods/{foodId}")
    public CommonResponse<Long> modifyFood(
            @AuthenticationPrincipal String socialId,
            @PathVariable("foodId") Long foodId,
            @RequestBody FoodRequestDto foodRequestDto
    ) {
        if (socialId == null) {
            return ApiResponseUtil.failure(new CustomException(ErrorResponseCode.SERVER_ERROR).getCode()); // 인증 실패
        }

        // Service를 통해 DB에서 유저 정보 조회
        User user = userService.findBySocialId(socialId);
        return ApiResponseUtil.success(foodService.updateFood(user.getId(), foodId, foodRequestDto));
    }

    @DeleteMapping("/foods/{foodId}")
    public CommonResponse<Void> deleteFood(@AuthenticationPrincipal String socialId, @PathVariable("foodId") Long foodId) {
        if (socialId == null) {
            return ApiResponseUtil.failure(new CustomException(ErrorResponseCode.SERVER_ERROR).getCode());
        }
        User user = userService.findBySocialId(socialId);
        foodService.deleteFood(user.getId(), foodId);
        return ApiResponseUtil.success(foodId + "번 음식 데이터가 삭제되었습니다.", null);
    }
}
