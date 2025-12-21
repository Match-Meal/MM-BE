package com.pagoda.matchmeal.model.entity;

import com.pagoda.matchmeal.model.enums.AiType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatbot extends BaseEntity {
    private Long aiChatbotId;
    private Long userId;          // FK
    private LocalDate refDate;    // 기준 날짜 (피드백 대상 날짜)
    private AiType aiType;        // FEEDBACK or RECOMMENDATION

    private String userQuestion;  // (선택) 사용자의 질문/요청 메시지
    private String aiResponse;    // AI의 답변 내용

}
