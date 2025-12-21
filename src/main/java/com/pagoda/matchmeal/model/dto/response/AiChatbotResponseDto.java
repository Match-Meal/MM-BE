package com.pagoda.matchmeal.model.dto.response;

import com.pagoda.matchmeal.model.enums.AiType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatbotResponseDto {
    private AiType aiType;      // AI 종류 (예: COACH, ANALYSIS 등)
    private String question;    // 사용자의 질문 (추가됨)
    private String answer;      // AI의 답변 (추가됨)
    private String date;        // 식단 날짜 등 참조 날짜 (추가됨)
    private LocalDateTime createdAt; // 생성 일시 (추가됨)
}
