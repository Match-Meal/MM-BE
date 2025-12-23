package com.pagoda.matchmeal.model.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RankingDto {

    private String foodName;
    private int eatCount;
}
