package com.pagoda.matchmeal.service.impl;

import com.pagoda.matchmeal.common.exception.CustomException;
import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import com.pagoda.matchmeal.common.response.PageInfoResponseDto;
import com.pagoda.matchmeal.mapper.FoodMapper;
import com.pagoda.matchmeal.model.dto.FoodSearchCond;
import com.pagoda.matchmeal.model.dto.request.FoodRequestDto;
import com.pagoda.matchmeal.model.dto.response.FoodDetailResponseDto;
import com.pagoda.matchmeal.model.dto.response.FoodListResponseDto;
import com.pagoda.matchmeal.model.entity.Food;
import com.pagoda.matchmeal.service.FoodService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 음식 데이터 관리 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FoodServiceImpl implements FoodService {

    private final FoodMapper foodMapper;

    /**
     * 음식 등록 (사용자 정의)
     *
     * @param userId         등록하는 사용자의 PK
     * @param foodRequestDto 음식 이름, 영양 정보 등이 담긴 요청 DTO
     * @return 생성된 음식의 ID (PK)
     */
    @Override
    @Transactional
    public Long addFood(Long userId, FoodRequestDto foodRequestDto) {
        String foodCode = "CUSTOM_" + userId + "_" + UUID.randomUUID().toString().substring(0, 8);

        Food food = Food.builder()
                .userId(userId)
                .foodCode(foodCode)
                .foodName(foodRequestDto.getFoodName())
                .category(foodRequestDto.getCategory())
                .servingSize(foodRequestDto.getServingSize())
                .unit(foodRequestDto.getUnit())
                .calories(foodRequestDto.getCalories())
                .carbohydrate(foodRequestDto.getCarbohydrate())
                .protein(foodRequestDto.getProtein())
                .fat(foodRequestDto.getFat())
                .sugars(foodRequestDto.getSugars())
                .sodium(foodRequestDto.getSodium())
                .build();

        foodMapper.saveFood(food);
        return food.getFoodId();
    }

    /**
     * 음식 목록 조회 (페이징 + 검색)
     *
     * @param userId   조회하는 사용자 ID
     * @param keyword  검색어 (음식 이름)
     * @param category 카테고리 필터
     * @param userOnly true: 내가 등록한 음식만 보기, false: 전체 음식 보기
     * @param pageable 페이징 정보 (page, size)
     * @return 페이징 정보가 포함된 음식 목록 응답 DTO
     */
    @Override
    public PageInfoResponseDto<FoodListResponseDto> getFoodList(Long userId, String keyword, String category, boolean userOnly, Pageable pageable) {
        FoodSearchCond cond = FoodSearchCond.builder()
                .userId(userId)
                .keyword(keyword)
                .category(category)
                .userOnly(userOnly)
                .limit(pageable.getPageSize())
                .offset((int) pageable.getOffset())
                .build();

        List<FoodListResponseDto> content = foodMapper.findFoodList(cond);
        int totalCount = foodMapper.countFoodList(cond);

        return PageInfoResponseDto.of(pageable, content, totalCount);
    }

    /**
     * 음식 상세 조회
     *
     * @param foodId 조회할 음식 ID
     * @param userId 요청자 ID (본인 음식 여부 `isMine` 판별용)
     * @return 음식 상세 정보 DTO
     */
    @Override
    public FoodDetailResponseDto getFoodDetail(Long foodId, Long userId) {
        FoodDetailResponseDto food = foodMapper.findFoodDetail(foodId);

        if (food == null) {
            throw new CustomException(ErrorResponseCode.FOOD_NOT_FOUND);
        }

        // isMine 세팅 (프론트엔드 편의성)
        boolean isMine = food.getUserId() != null && food.getUserId().equals(userId);
        food.setMine(isMine);
        return food;
    }

    /**
     * 음식 정보 수정 (본인 확인)
     *
     * @param userId         요청자 ID
     * @param foodId         수정할 음식 ID
     * @param foodRequestDto 수정할 영양 정보 및 내용
     * @return 수정된 음식 ID
     */
    @Override
    @Transactional
    public Long updateFood(Long userId, Long foodId, FoodRequestDto foodRequestDto) {
        FoodDetailResponseDto existingFood = foodMapper.findFoodDetail(foodId);

        if (existingFood == null) {
            throw new CustomException(ErrorResponseCode.FOOD_NOT_FOUND);
        }
        if (!existingFood.getUserId().equals(userId)) {
            throw new CustomException(ErrorResponseCode.UNAUTHORIZED);
        }

        Food updateFood = Food.builder()
                .foodId(foodId)
                .foodName(foodRequestDto.getFoodName())
                .category(foodRequestDto.getCategory())
                .servingSize(foodRequestDto.getServingSize())
                .unit(foodRequestDto.getUnit())
                .calories(foodRequestDto.getCalories())
                .carbohydrate(foodRequestDto.getCarbohydrate())
                .protein(foodRequestDto.getProtein())
                .fat(foodRequestDto.getFat())
                .sugars(foodRequestDto.getSugars())
                .sodium(foodRequestDto.getSodium())
                .build();

        foodMapper.updateFood(updateFood);
        return updateFood.getFoodId();
    }

    /**
     * 음식 삭제
     *
     * @param userId 요청자 ID (본인 확인용)
     * @param foodId 삭제할 음식 ID
     */
    @Override
    @Transactional
    public void deleteFood(Long userId, Long foodId) {
        FoodDetailResponseDto existingFood = foodMapper.findFoodDetail(foodId);

        if (existingFood == null) {
            throw new CustomException(ErrorResponseCode.FOOD_NOT_FOUND);
        }
        if (!existingFood.getUserId().equals(userId)) {
            throw new CustomException(ErrorResponseCode.UNAUTHORIZED);
        }

        String suffix = "_DEL_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        foodMapper.deleteFood(foodId, suffix);
    }

    /**
     * 음식 카테고리 목록 조회
     *
     * @return 카테고리 이름 리스트 (캐시 적용)
     */
    @Override
    @Cacheable(value = "foodCategories")
    public List<String> getFoodCategories() {
        return foodMapper.findAllCategories();
    }
}