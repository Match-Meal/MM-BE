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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 읽기 전용 트랜잭션 적용 (성능 최적화)
public class FoodServiceImpl implements FoodService {

    private final FoodMapper foodMapper;

    /**
     * 음식 등록
     *
     * @param userId         등록하는 사용자의 PK
     * @param foodRequestDto 요청 데이터 (이름, 칼로리 등)
     * @return 생성된 음식의 ID (PK)
     */
    @Override
    @Transactional
    public Long addFood(Long userId, FoodRequestDto foodRequestDto) {

        // 임의의 음식 코드 (규칙: "CUSTOM_유저ID_UUID앞8자리")
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
                .build();

        foodMapper.saveFood(food);

        return food.getFoodId();
    }

    /**
     * 음식 목록 조회 (페이징 + 검색)
     *
     * @param userId   조회하는 사용자 ID (내 음식 필터링 용도)
     * @param keyword  검색어 (음식 이름)
     * @param category 카테고리 필터
     * @param userOnly true: 내 음식만 보기, false: 전체(공용+내것) 보기
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
     * @param userId 조회 요청한 사용자 ID (권한 체크 및 isMine 설정용)
     * @return 음식 상세 정보 DTO
     */
    @Override
    public FoodDetailResponseDto getFoodDetail(Long foodId, Long userId) {
        FoodDetailResponseDto food = foodMapper.findFoodDetail(foodId);

        if (food == null) {
            throw new CustomException(ErrorResponseCode.FOOD_NOT_FOUND);
        }

        if (food.getUserId() != null) {
            if (!food.getUserId().equals(userId)) {
                throw new CustomException(ErrorResponseCode.UNAUTHORIZED);
            }
        }
        //isMine 세팅 (프론트엔드 편의성)
        boolean isMine = food.getUserId() != null && food.getUserId().equals(userId);
        food.setMine(isMine);
        return food;
    }

    /**
     * 음식 수정 (본인 확인 필수)
     *
     * @param userId         수정 요청한 사용자 ID
     * @param foodId         수정할 음식 ID
     * @param foodRequestDto 수정할 내용 (null인 필드는 수정하지 않음)
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
                .foodId(foodId) // WHERE 절을 위해 필수
                .foodName(foodRequestDto.getFoodName())
                .category(foodRequestDto.getCategory())
                .servingSize(foodRequestDto.getServingSize())
                .unit(foodRequestDto.getUnit())
                .calories(foodRequestDto.getCalories())
                .carbohydrate(foodRequestDto.getCarbohydrate())
                .protein(foodRequestDto.getProtein())
                .fat(foodRequestDto.getFat())
                .build();

        foodMapper.updateFood(updateFood);

        return updateFood.getFoodId();
    }

    /**
     * 음식 삭제 (본인 확인 필수)
     *
     * @param userId 삭제 요청한 사용자 ID
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

    // "categories"라는 이름으로 캐시 저장 (메모리에 저장됨)
    // 이 메소드는 최초 1회만 DB를 조회하고, 그 뒤론 메모리에서 꺼내줌
    @Override
    @Cacheable(value = "foodCategories")
    public List<String> getFoodCategories() {
        return foodMapper.findAllCategories();
    }
}
