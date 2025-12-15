package com.pagoda.matchmeal.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
영양소 상태 객체
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NutrientStatusDto {
    private String nutrientName; // "나트륨" or "당류"

    private int currentIntake;      // 사용자의 평균 섭취량
    private int recommendedLimit;   // 권장 상한선 (2000mg or 칼로리의 10% 환산 g)

    // 섭취율 (현재 / 권장 * 100) -> 게이지 바 그리기용
    private int intakePercentage;

    // 상태 (프론트에서 색상 표시용: GREEN, YELLOW, RED)
    private NutrientLevel status;

    public enum NutrientLevel {
        GOOD, WARNING, BAD
    }
}