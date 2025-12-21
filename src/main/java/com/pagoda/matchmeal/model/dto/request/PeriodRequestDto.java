package com.pagoda.matchmeal.model.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class PeriodRequestDto {
    private LocalDate startDate;
    private LocalDate endDate;
}
