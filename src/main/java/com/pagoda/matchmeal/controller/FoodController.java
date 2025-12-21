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

import java.util.List;

/**
 * 음식 데이터 관리 컨트롤러
 * - 공공 데이터 조회 및 사용자 정의 음식(Custom Food) CRUD
 */
@RestController
@RequiredArgsConstructor
public class FoodController {

    private final FoodService foodService;

    /**
     * 사용자 정의 음식 등록
     */
    @PostMapping("/foods")
    public CommonResponse<Long> createFood(@AuthenticationPrincipal UserDto userDto, @RequestBody FoodRequestDto foodRequestDto) {
        return ApiResponseUtil.created("음식 DB 생성 성공", foodService.addFood(userDto.getId(), foodRequestDto));
    }

    /**
     * 음식 목록 검색 (페이징)
     */
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

    /**
     * 음식 상세 정보 조회
     */
    @GetMapping("/foods/{foodId}")
    public CommonResponse<FoodDetailResponseDto> getFoodDetail(@AuthenticationPrincipal UserDto userDto, @PathVariable("foodId") Long foodId) {

        return ApiResponseUtil.success(foodService.getFoodDetail(foodId, userDto.getId()));
    }

    /**
     * 사용자 정의 음식 수정
     */
    @PatchMapping("/foods/{foodId}")
    public CommonResponse<Long> modifyFood(
            @AuthenticationPrincipal UserDto userDto,
            @PathVariable("foodId") Long foodId,
            @RequestBody FoodRequestDto foodRequestDto
    ) {
        return ApiResponseUtil.success(foodService.updateFood(userDto.getId(), foodId, foodRequestDto));
    }

    /**
     * 사용자 정의 음식 삭제
     */
    @DeleteMapping("/foods/{foodId}")
    public CommonResponse<Void> deleteFood(@AuthenticationPrincipal UserDto userDto, @PathVariable("foodId") Long foodId) {
        foodService.deleteFood(userDto.getId(), foodId);
        return ApiResponseUtil.success(foodId + "번 음식 데이터가 삭제되었습니다.", null);
    }

    /**
     * 음식 카테고리 목록 조회
     */
    @GetMapping("/foods/categories")
    public CommonResponse<List<String>> getCategories() {
        return ApiResponseUtil.success(foodService.getFoodCategories());
    }
}