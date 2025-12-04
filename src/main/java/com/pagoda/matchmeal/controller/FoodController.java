package com.pagoda.matchmeal.controller;

import com.pagoda.matchmeal.common.response.CommonResponse;
import com.pagoda.matchmeal.common.response.PageInfoResponseDto;
import com.pagoda.matchmeal.common.util.ApiResponseUtil;
import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.model.dto.request.FoodRequestDto;
import com.pagoda.matchmeal.model.dto.response.FoodDetailResponseDto;
import com.pagoda.matchmeal.model.dto.response.FoodListResponseDto;
import com.pagoda.matchmeal.service.FoodService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class FoodController {

    private final FoodService foodService;

    @PostMapping("/foods")
    public CommonResponse<Long> createFood(@AuthenticationPrincipal UserDto userDto, @RequestBody FoodRequestDto foodRequestDto) {
        return ApiResponseUtil.created("음식 DB 생성 성공", foodService.addFood(userDto.getId(), foodRequestDto));
    }

    @GetMapping("/foods")
    public CommonResponse<PageInfoResponseDto<FoodListResponseDto>> getFoodList(
            @AuthenticationPrincipal UserDto userDto,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "false") boolean userOnly,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        return ApiResponseUtil.success(foodService.getFoodList(userDto.getId(), keyword, category, userOnly, pageable));
    }

    @GetMapping("/foods/{foodId}")
    public CommonResponse<FoodDetailResponseDto> getFoodDetail(@AuthenticationPrincipal UserDto userDto, @PathVariable("foodId") Long foodId) {

        return ApiResponseUtil.success(foodService.getFoodDetail(foodId, userDto.getId()));
    }

    @PatchMapping("/foods/{foodId}")
    public CommonResponse<Long> modifyFood(
            @AuthenticationPrincipal UserDto userDto,
            @PathVariable("foodId") Long foodId,
            @RequestBody FoodRequestDto foodRequestDto
    ) {

        return ApiResponseUtil.success(foodService.updateFood(userDto.getId(), foodId, foodRequestDto));
    }

    @DeleteMapping("/foods/{foodId}")
    public CommonResponse<Void> deleteFood(@AuthenticationPrincipal UserDto userDto, @PathVariable("foodId") Long foodId) {
        foodService.deleteFood(userDto.getId(), foodId);
        return ApiResponseUtil.success(foodId + "번 음식 데이터가 삭제되었습니다.", null);
    }
}
