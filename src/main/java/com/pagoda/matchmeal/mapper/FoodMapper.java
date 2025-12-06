package com.pagoda.matchmeal.mapper;

import com.pagoda.matchmeal.model.dto.FoodSearchCond;
import com.pagoda.matchmeal.model.dto.response.FoodDetailResponseDto;
import com.pagoda.matchmeal.model.dto.response.FoodListResponseDto;
import com.pagoda.matchmeal.model.entity.Food;

import java.util.List;

public interface FoodMapper {

    /**
     * 음식 저장 (INSERT)
     * - DB에 저장 후, 생성된 PK(foodId)가 파라미터로 넘긴 Food 객체에 자동으로 채워집니다.
     *
     * @param food 저장할 음식 엔티티
     */
    void saveFood(Food food);

    /**
     * 음식 목록 조회 (SELECT - 다건)
     * - 검색 조건(키워드, 카테고리, 내 음식 여부)에 따라 동적으로 쿼리가 생성됩니다.
     * - 페이징(LIMIT, OFFSET) 처리가 포함되어 있습니다.
     *
     * @param cond 검색 조건 및 페이징 정보가 담긴 DTO
     * @return 조회된 음식 목록 (요약 정보)
     */
    List<FoodListResponseDto> findFoodList(FoodSearchCond cond);

    /**
     * 전체 데이터 개수 조회 (SELECT - COUNT)
     * - 페이징 계산(TotalPages)을 위해 전체 레코드 수를 조회합니다.
     * - findFoodList와 동일한 검색 조건(WHERE 절)을 사용해야 정확한 개수가 나옵니다.
     *
     * @param cond 검색 조건
     * @return 조건에 맞는 전체 데이터 수
     */
    int countFoodList(FoodSearchCond cond);

    /**
     * 음식 상세 조회 (SELECT - 단건)
     * - 특정 음식의 ID로 모든 상세 정보를 조회합니다.
     *
     * @param foodId 조회할 음식의 PK
     * @return 음식 상세 정보 (없으면 null 반환)
     */
    FoodDetailResponseDto findFoodDetail(Long foodId);

    /**
     * 음식 수정 (UPDATE - 부분 수정)
     * - Food 객체에 값이 존재하는 필드만 골라서 업데이트합니다 (Dynamic Update).
     * - 값이 null인 필드는 기존 값을 유지합니다.
     *
     * @param food 수정할 내용이 담긴 음식 엔티티
     */
    void updateFood(Food food);

    /**
     * 음식 삭제 (DELETE)
     * - 특정 음식을 DB에서 영구 삭제합니다.
     *
     * @param foodId 삭제할 음식의 PK
     */
    void deleteFood(Long foodId);

    // [추가] 서비스 내부 로직 계산용
    Food findById(Long foodId);

    List<String> findAllCategories();
}
