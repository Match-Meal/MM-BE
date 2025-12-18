package com.pagoda.matchmeal.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DietStatsResponseDto {

    // === [1. 기간 요약 정보] ===
    private int periodTotalDays;    // 조회 기간 (7일 등)
    private int averageCalories;    // 기간 평균 칼로리

    // 탄단지 비율 분석 (5:2:3 검증)
    private MacronutrientAnalysisDto cpfRatioAnalysis;

    // 나트륨 분석 (2000mg 미만 검증)
    private NutrientStatusDto sodiumAnalysis;

    // 당류 분석 (총 열량의 10% 미만 검증)
    private NutrientStatusDto sugarAnalysis;


    // === [2. 일별 상세 데이터] ===
    // 그래프 그리기용 리스트
    private List<DailyDietStatDto> dailyStats;
}