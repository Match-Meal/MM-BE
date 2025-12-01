package com.pagoda.matchmeal.service;

import com.pagoda.matchmeal.common.response.PageInfoResponseDto;
import com.pagoda.matchmeal.model.dto.request.FoodRequestDto;
import com.pagoda.matchmeal.model.dto.response.FoodDetailResponseDto;
import com.pagoda.matchmeal.model.dto.response.FoodListResponseDto;
import org.springframework.data.domain.Pageable;

public interface FoodService {

    Long addFood(Long userId, FoodRequestDto foodRequestDto);

    PageInfoResponseDto<FoodListResponseDto> getFoodList(Long userId, String keyword, String category, boolean userOnly, Pageable pageable);

    FoodDetailResponseDto getFoodDetail(Long foodId, Long userId);

    Long updateFood(Long userId, Long foodId, FoodRequestDto foodRequestDto);

    void deleteFood(Long userId, Long foodId);
}
