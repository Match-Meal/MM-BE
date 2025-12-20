package com.pagoda.matchmeal.model.dto.response;

import com.pagoda.matchmeal.model.dto.MatchedFoodDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FoodAnalysisResponseDto {

    // AI 분석
    private String predictedName; // 최우선 음식명
    private List<String> candidates; // 후보

    // DB 매칭 섹션
    private List<MatchedFoodDto> matchedFoods;
}
