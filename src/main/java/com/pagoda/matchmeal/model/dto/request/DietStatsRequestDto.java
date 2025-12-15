package com.pagoda.matchmeal.model.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

@Getter
@Setter
@ToString
public class DietStatsRequestDto {
    private String periodType; // "WEEKLY", "MONTHLY", "CUSTOM"

    // CUSTOM일 경우에만 필수
    private LocalDate startDate;
    private LocalDate endDate;
}