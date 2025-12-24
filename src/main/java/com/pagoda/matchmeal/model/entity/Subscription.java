package com.pagoda.matchmeal.model.entity;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {
    private Long subscriptionId;
    private Long userId;
    private String sid;            // 카카오페이 정기 결제 키
    private String cid;            // 가맹점 코드 (TCSUBSCRIP)
    private String tid;            // 결제 고유 번호
    private String partnerOrderId;  // 가맹점 주문번호
    private String itemName;        // 상품명
    private Integer amount;         // 결제 금액
    private String status;          // ACTIVE, INACTIVE
    private LocalDateTime lastApprovedAt; // 마지막 결제 승인 시각
    private LocalDateTime nextBillingAt; // 다음 결제 예정일
}