package com.pagoda.matchmeal.model.entity;

import com.pagoda.matchmeal.model.enums.MealType;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Diet extends BaseEntity {

    private Long dietId;        // PK
    private Long userId;        // 사용자 ID

    private LocalDate eatDate; // 식사 날짜
    private LocalTime eatTime; // 식사 시간

    private MealType mealType;  // 아침/점심/저녁/간식
    private String memo;        // 식사 메모 (사진 등은 추후 확장)
    private String dietImgUrl;  // 식단 인증샷 이미지 경로

    // ★ 통계용 합계 필드 (DB에 저장해두면 조회 성능 10배 향상)
    private double totalCalories;
    private double totalCarbohydrate;    // 총 탄수
    private double totalProtein;  // 총 단백
    private double totalFat;      // 총 지방
    private double totalSugars;
    private double totalSodium;


    private List<DietDetail> dietDetails; // 포함된 음식들

    private LocalDateTime deletedAt;
}
