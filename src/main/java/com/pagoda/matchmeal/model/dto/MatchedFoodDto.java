package com.pagoda.matchmeal.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MatchedFoodDto {

    private Long foodId;

    private String foodName;
    private Double calories;
    private Double carbohydrate;
    private Double protein;
    private Double fat;
    private Double sugars;
    private Double sodium;
}
