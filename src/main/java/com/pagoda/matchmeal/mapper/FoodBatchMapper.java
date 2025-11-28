package com.pagoda.matchmeal.mapper;

import com.pagoda.matchmeal.model.entity.Food;

import java.util.List;

public interface FoodBatchMapper {
    /**
     * 음식 데이터를 DB에 저장하거나 업데이트합니다.
     * - XML의 <insert id="insertFood"> 쿼리를 실행합니다.
     * - ON DUPLICATE KEY UPDATE 구문을 통해 중복 시 갱신 처리합니다.
     *
     * @param food 저장할 음식 엔티티
     */
    void insertFoods(Food food);

    /**
     * (테스트용) 저장된 모든 음식 데이터를 조회합니다.
     * - 데이터가 정상적으로 들어갔는지 검증할 때 사용합니다.
     * * @return 저장된 음식 리스트
     */
    int countAll();

    /**
     * (테스트용) 전체 데이터 개수를 조회합니다.
     * - Batch 실행 후 건수 확인용입니다.
     *
     * @return 전체 행(Row) 개수
     */
    List<Food> findAll();
}
