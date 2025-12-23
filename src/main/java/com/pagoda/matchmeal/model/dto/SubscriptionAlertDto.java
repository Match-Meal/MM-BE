package com.pagoda.matchmeal.model.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionAlertDto {

    private Long userId;
    private int dDay;
}
