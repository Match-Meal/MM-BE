package com.pagoda.matchmeal.model.dto.response;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@ToString
public class KakaoApproveResponse {
    private String aid;                  // 요청 고유 번호
    private String tid;                  // 결제 고유 번호
    private String cid;                  // 가맹점 코드
    private String sid;                  // ★ 정기 결제용 ID (이후 자동결제 시 필수)
    private String partner_order_id;      // 가맹점 주문번호
    private String partner_user_id;       // 가맹점 회원 id
    private String payment_method_type;   // 결제 수단 (CARD 또는 MONEY)
    private String item_name;             // 상품 이름
    private Integer quantity;             // 상품 수량
    private Amount amount;                // 결제 금액 정보
    private CardInfo card_info;           // 카드 상세 정보 (결제 수단이 카드일 때만)
    private LocalDateTime created_at;      // 결제 준비 요청 시각
    private LocalDateTime approved_at;     // 결제 승인 시각

    // 금액 상세 정보 내부 클래스
    @Getter @Setter @ToString
    public static class Amount {
        private Integer total;            // 전체 결제 금액
        private Integer tax_free;         // 비과세 금액
        private Integer vat;              // 부가세 금액
        private Integer point;            // 사용한 포인트 금액
        private Integer discount;         // 할인 금액
        private Integer green_deposit;    // 컵 보증금
    }

    // 카드 상세 정보 내부 클래스
    @Getter @Setter @ToString
    public static class CardInfo {
        private String kakaopay_purchase_corp;      // 카카오페이 매입사명
        private String kakaopay_purchase_corp_code; // 카카오페이 매입사 코드
        private String kakaopay_issuer_corp;        // 카카오페이 발급사명
        private String kakaopay_issuer_corp_code;   // 카카오페이 발급사 코드
        private String bin;                         // 카드 BIN
        private String card_type;                   // 카드 타입
        private String install_month;               // 할부 개월 수
        private String approved_id;                 // 카드사 승인번호
        private String card_mid;                    // 카드사 가맹점 번호
        private String interest_free_install;       // 무이자할부 여부
        private String installment_type;            // 할부 유형
    }
}