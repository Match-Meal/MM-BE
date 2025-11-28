package com.pagoda.matchmeal.model.entity;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class DietDetail {

    private long id;
    private long dietId;            // 식단 id
    private long FoodId;            // 음식 id
    private double quantity;        // 섭취량
    private LocalDateTime createdAt;// 생성일자
    private LocalDateTime updatedAt;// 수정일자
}
