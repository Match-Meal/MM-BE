package com.pagoda.matchmeal.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/*
일별 상세 데이터 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyDietStatDto {
    private LocalDate date;       // 날짜 (X축)

    private int totalCalories;    // 총 섭취 칼로리

    // 그래프용 순수 섭취량 (g/mg)
    private int carbsG;           // 탄수화물 (g)
    private int proteinG;         // 단백질 (g)
    private int fatG;             // 지방 (g)
    private int sugarG;           // 당류 (g)
    private int sodiumMg;         // 나트륨 (mg)

    private int dietScore;        // 그날의 식단 점수 (85점 등)
}