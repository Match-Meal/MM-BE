package com.pagoda.matchmeal.model.entity;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class DietDetail {

    private Long dietDetailId; // PK
    private Long dietId;       // FK (부모 식사 ID)

    private long foodId;   // 음식 id (FK 역할)
    private String foodName;   // 음식 이름 (스냅샷: 원본 음식이름이 바뀌어도 기록은 유지)

    private double quantity;   // 섭취량 (사용자가 입력한 값)
    private String unit;       // 단위 (g, ml, 개, 인분 등) - 음식 DB의 unit과 맞춤

    // ★ 영양성분 스냅샷 (계산된 결과값 저장) ★
    // 예: 닭가슴살 200g을 먹었다면, 100g당 단백질*2 한 값을 여기에 저장
    private double calories;
    private double carbohydrate;
    private double protein;
    private double fat;
}
