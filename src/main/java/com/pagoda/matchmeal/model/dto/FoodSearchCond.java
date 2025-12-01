package com.pagoda.matchmeal.model.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 음식 검색 및 필터링 조건을 담는 VO (Value Object)
 * - Controller에서 받은 파라미터들을 Mapper(MyBatis)에 전달하기 위한 객체입니다.
 */
@Getter
@Builder
public class FoodSearchCond {
    private Long userId;
    // 검색어 (음식 이름 LIKE 검색)
    private String keyword;
    // 카테고리 필터 (일치 검색)
    private String category;
    // true: 내 음식만 조회 / false: (내 음식 + 공용 음식) 함께 조회
    private boolean userOnly;

    private int offset;
    private int limit;
}
