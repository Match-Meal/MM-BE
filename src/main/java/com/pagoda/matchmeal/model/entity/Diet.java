package com.pagoda.matchmeal.model.entity;

import com.pagoda.matchmeal.model.enums.MealType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
public class Diet {

    private long id;
    private LocalDateTime date;     // 섭취 날짜
    private MealType mealType;      // 식사 구분
    private String memo;            // 메모
    private LocalDateTime createdAt;// 생성일자
    private LocalDateTime updatedAt;// 수정일자
}
