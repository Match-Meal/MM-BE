package com.pagoda.matchmeal.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
탄단지 비율 분석 객체
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MacronutrientAnalysisDto {
    // 1. 실제 섭취 비율 (%) - 파이 차트용
    private double carbRatio;    // 예: 55.0 (%)
    private double proteinRatio; // 예: 25.0 (%)
    private double fatRatio;     // 예: 20.0 (%)

    // 2. 권장 비율 (%) - 비교 기준선
    // 사용자 설정이 없으면 기본값 (50 : 20 : 30) 세팅
    private double recommendedCarbRatio;
    private double recommendedProteinRatio;
    private double recommendedFatRatio;

    // 3. 피드백 메시지
    private String feedback; // 예: "탄수화물 비중이 권장보다 5% 높아요!"
}