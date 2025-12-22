package com.pagoda.matchmeal.model.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class SubscriptionResponseDto {
    private String status;          // ACTIVE, INACTIVE
    private LocalDate nextBillingDate; // 다음 결제 예정일
    private String itemName;        // 상품명
    private Integer amount;         // 결제 금액
}
