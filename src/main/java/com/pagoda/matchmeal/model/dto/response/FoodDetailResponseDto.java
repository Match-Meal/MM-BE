package com.pagoda.matchmeal.model.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 음식 상세 조회(GET /api/foods/{foodId}) 시 반환되는 DTO
 * - 프론트엔드에서 '수정/삭제' 버튼 노출 여부를 판단할 수 있도록 'isMine' 필드를 추가했습니다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodDetailResponseDto {

    private Long foodId;
    private String foodCode;
    private String foodName;
    private String category;

    // 양 및 영양 성분
    private Double servingSize;
    private String unit;
    private Double calories;
    private Double carbohydrate;
    private Double protein;
    private Double fat;
    private Double sugars;
    private Double sodium;

    // 작성자 정보
    private Long userId; // 작성자 ID (null이면 공용)
    // ★ [프론트엔드 편의성 필드]
    // true: 현재 로그인한 사용자가 등록한 음식 -> 수정/삭제 버튼 보여줌
    // false: 남이 등록했거나 공용 음식 -> 수정/삭제 버튼 숨김
    private boolean isMine; // 현재 로그인한 사용자의 것인지 여부 (Frontend에서 수정 버튼 노출 판단용)

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
