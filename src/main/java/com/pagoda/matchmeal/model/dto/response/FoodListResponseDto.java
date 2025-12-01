package com.pagoda.matchmeal.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 음식 목록 조회(GET /foods) 시 반환되는 DTO
 * - 목록 화면에서는 모든 영양성분 정보가 필요하지 않습니다.
 * - 데이터 전송량을 줄이기 위해 핵심 정보(이름, 카테고리, 칼로리 등)만 선별하여 담았습니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodListResponseDto {

    private Long foodId;
    private String foodName;
    private String category;
    private Double servingSize;
    private String unit;
    private Double calories;

}
