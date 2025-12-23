package com.pagoda.matchmeal.model.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PeriodMealPlanRequestDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private List<String> flavors;
}
