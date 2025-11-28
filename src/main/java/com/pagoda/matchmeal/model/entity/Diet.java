package com.pagoda.matchmeal.model.entity;

import com.pagoda.matchmeal.model.enums.MealType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
public class Diet {

    private Long dietId;        // PK
    private Long userId;        // 사용자 ID

    private LocalDateTime date; // 식사 날짜

    private MealType mealType;  // 아침/점심/저녁/간식

    // ★ 통계용 합계 필드 (DB에 저장해두면 조회 성능 10배 향상)
    private double totalCalories;
    private double totalCarbo;    // 총 탄수
    private double totalProtein;  // 총 단백
    private double totalFat;      // 총 지방

    private String memo;        // 식사 메모 (사진 등은 추후 확장)

    private List<DietDetail> dietDetails; // 포함된 음식들
}
