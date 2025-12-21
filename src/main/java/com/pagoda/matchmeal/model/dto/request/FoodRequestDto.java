package com.pagoda.matchmeal.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 음식 등록(POST) 및 수정(PATCH) 요청 시 사용하는 DTO
 * - 수정 시, 변경하고 싶은 필드만 값이 들어오고 나머지는 null이 될 수 있습니다.
 * - 따라서 기본형(double) 대신 래퍼 클래스(Double)를 사용하여 null 체크가 가능하도록 했습니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodRequestDto {

    private String foodName;
    private String category;
    private Double servingSize;
    private String unit;
    private Double calories;
    private Double carbohydrate;
    private Double protein;
    private Double fat;
    private Double sugars;
    private Double sodium;

}
