package com.pagoda.matchmeal.model.entity;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 음식(Food) 도메인 엔티티
 * - 데이터베이스의 'foods' 테이블과 매핑됩니다.
 */
@Getter
@Setter
@SuperBuilder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Food extends BaseEntity {
    private Long foodId;
    private Long userId;
    private String foodCode;
    private String foodName;
    private String category;
    private Double servingSize;
    private String unit;
    private Double calories;
    private Double carbohydrate;
    private Double protein;
    private Double fat;
    private LocalDateTime deletedAt;
}