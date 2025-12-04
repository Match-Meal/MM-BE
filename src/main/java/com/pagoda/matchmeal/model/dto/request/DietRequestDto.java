package com.pagoda.matchmeal.model.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pagoda.matchmeal.model.enums.MealType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class DietRequestDto {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    private LocalDate eatDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss", timezone = "Asia/Seoul")
    private LocalTime eatTime;
    private MealType mealType;
    private String memo;

    private List<DietDetailRequestDto> foods = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    public static class DietDetailRequestDto {
        // 선택 1 기존 음식 선택 시
        private Long foodId;

        // 선택 2 직접 입력 시 (foodId가 없을 때 사용)
        private String foodName;
        private double calories;
        private double carbohydrate;
        private double protein;
        private double fat;

        // 공통 필수
        private double quantity; // 먹은 양
        private String unit;     // 단위

        // 음식 DB에 저장 체크박스 값
        private boolean saveToMyFoods;
    }
}
