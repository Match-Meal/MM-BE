package com.pagoda.matchmeal.model.dto;

import com.pagoda.matchmeal.model.enums.ChallengeType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeSearchCondition {
    private ChallengeType type;
    private LocalDate startDate;
    private LocalDate endDate;
    private String keyword;
}
