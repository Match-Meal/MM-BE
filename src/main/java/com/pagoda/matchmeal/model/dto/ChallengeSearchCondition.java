package com.pagoda.matchmeal.model.dto;

import com.pagoda.matchmeal.model.enums.ChallengeType;
import lombok.*;

import java.time.LocalDate;

@AllArgsConstructor
public class ChallengeSearchCondition {
    private ChallengeType type;
    private LocalDate startDate;
    private LocalDate endDate;
    private String keyword;

    // [중요] 기본 생성자
    public ChallengeSearchCondition() {}

    // [중요] 수동 Getter/Setter 추가 (MyBatis가 100% 인식함)
    public ChallengeType getType() { return type; }
    public void setType(ChallengeType type) { this.type = type; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}
