package com.pagoda.matchmeal.model.dto.response;

import com.pagoda.matchmeal.model.enums.MealType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DietResponseDto {

    private Long dietId;
    private Long userId;
    private LocalDate eatDate;
    private LocalTime eatTime;
    private MealType mealType;
    private String memo;
    private String dietImgUrl;

    private double totalCalories;
    private double totalCarbohydrate;
    private double totalProtein;
    private double totalFat;

    private List<DietDetailResponseDto> details;

    private LocalDateTime deletedAt;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DietDetailResponseDto {
        private Long dietDetailId;
        private Long foodId;
        private String foodName; // 스냅샷 이름
        private double quantity;
        private String unit;

        // 영양소 스냅샷
        private double calories;
        private double carbohydrate;
        private double protein;
        private double fat;

        private String dietImgUrl;
    }
}
