package com.pagoda.matchmeal.model.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * CSV 파일 데이터 매핑용 DTO
 * - Spring Batch의 ItemReader가 CSV 파일을 읽어서 이 객체에 담습니다.
 * - 원본 데이터에 "N/A", "-", 공백 등이 섞여 있을 수 있으므로,
 *      모든 필드를 String으로 선언하여 파싱 에러를 방지합니다.
 * - 이후 Processor 단계에서 검증 후 Food 엔티티(Double 등)로 변환됩니다.
 */
@Getter
@Setter
public class FoodCsvDto {
    private String foodCode;      // 식품코드
    private String foodName;      // 식품명
    private String category;      // 식품대분류명
    private String servingSize;   // 영양성분함량기준량
    private String calories;      // 에너지(kcal)
    private String protein;       // 단백질(g)
    private String fat;           // 지방(g)
    private String carbohydrate;  // 탄수화물(g)
}