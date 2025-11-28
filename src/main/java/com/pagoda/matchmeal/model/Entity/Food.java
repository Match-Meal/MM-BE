package com.pagoda.matchmeal.model.Entity;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 음식(Food) 도메인 엔티티
 * - 데이터베이스의 'foods' 테이블과 매핑됩니다.
 */
@Getter
@Builder
public class Food {
    private long foodId;
    private String foodCode;
    private String foodName;
    private String category;
    private double servingSize;
    private String unit;
    private double calories;
    private double carbohydrate;
    private double protein;
    private double fat;
    private LocalDateTime createdAt; // 데이터 생성일시
    private LocalDateTime updatedAt; // 데이터 수정일시
}