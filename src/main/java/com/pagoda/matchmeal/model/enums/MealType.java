package com.pagoda.matchmeal.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MealType {
    BREAKFAST("아침"), LUNCH("점심"), DINNER("저녁"), SNACK("간식/야식");

    private final String type;

}
