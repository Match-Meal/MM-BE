package com.pagoda.matchmeal.service;

import com.pagoda.matchmeal.common.exception.CustomException;
import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import com.pagoda.matchmeal.mapper.SubscriptionMapper;
import com.pagoda.matchmeal.mapper.UserMapper;
import com.pagoda.matchmeal.model.dto.response.KakaoApproveResponse;
import com.pagoda.matchmeal.model.dto.response.KakaoReadyResponse;
import com.pagoda.matchmeal.model.entity.Subscription;
import com.pagoda.matchmeal.model.entity.User;
import com.pagoda.matchmeal.model.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KakaoPayService {
    private final UserMapper userMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final RestTemplate restTemplate;

    private final Map<Long, String> tidCache = new HashMap<>();

    @Value("${kakaopay.admin-key}") // application.yml에 설정 필요
    private String adminKey;

    /**
     * 1. 결제 준비 요청 (Ready)
     * 사용자가 '구독하기'를 눌렀을 때 호출되어 카카오 결제 URL을 반환합니다.
     */
    public KakaoReadyResponse ready(Long userId) {
        Map<String, Object> params = new HashMap<>();
        params.put("cid", "TCSUBSCRIP");
        params.put("partner_order_id", "sub_order_" + userId);
        params.put("partner_user_id", String.valueOf(userId));
        params.put("item_name", "매치밀 정기구독");
        params.put("quantity", 1);
        params.put("total_amount", 9900);
        params.put("tax_free_amount", 0);
        params.put("approval_url", "http://localhost:8080/payment/success?userId=" + userId);
        params.put("cancel_url", "http://localhost:8080/payment/cancel");
        params.put("fail_url", "http://localhost:8080/payment/fail");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(params, getHeaders());

        KakaoReadyResponse response = restTemplate.postForObject(
                "https://open-api.kakaopay.com/online/v1/payment/ready",
                request, KakaoReadyResponse.class);

        if (response != null) {
            // ★ 중요: 나중에 승인(Approve)할 때 쓰기 위해 tid를 임시 저장합니다.
            tidCache.put(userId, response.getTid());
        }
        return response;
    }

    /**
     * 2. 첫 결제 승인 및 SID 발급 (Approve)
     */
    @Transactional
    public void approveFirstPayment(String pgToken, Long userId) {
        // ★ 중요: 저장해뒀던 tid를 꺼내옵니다.
        String tid = tidCache.get(userId);
        if (tid == null) throw new CustomException(ErrorResponseCode.PAYMENT_TID_NOT_FOUND);

        Map<String, Object> params = new HashMap<>();
        params.put("cid", "TCSUBSCRIP");
        params.put("tid", tid); // 꺼내온 tid 사용
        params.put("partner_order_id", "sub_order_" + userId);
        params.put("partner_user_id", String.valueOf(userId));
        params.put("pg_token", pgToken);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(params, getHeaders());

        KakaoApproveResponse response = restTemplate.postForObject(
                "https://open-api.kakaopay.com/online/v1/payment/approve",
                request, KakaoApproveResponse.class);

        if (response != null && response.getSid() != null) {
            // 구독 정보 저장
            Subscription sub = Subscription.builder()
                    .userId(userId)
                    .sid(response.getSid())
                    .tid(response.getTid())
                    .partnerOrderId(response.getPartner_order_id())
                    .itemName(response.getItem_name())
                    .amount(response.getAmount().getTotal())
                    .status("ACTIVE")
                    .lastApprovedAt(response.getApproved_at())
                    .nextBillingAt(response.getApproved_at().plusMonths(1))
                    .build();
            subscriptionMapper.insertSubscription(sub);

            // 유저 권한 변경
            User user = userMapper.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorResponseCode.USER_NOT_FOUND));
            user.setRole(UserRole.ROLE_SUBSCRIBER);
            userMapper.updateUserRole(user);

            // 사용한 tid는 캐시에서 삭제
            tidCache.remove(userId);
        }
    }

    /**
     * 3. 정기 결제 요청 (Subscription)
     */
    @Transactional
    public void executeRecurringPayment(Subscription sub) {
        Map<String, Object> params = new HashMap<>();
        params.put("cid", "TCSUBSCRIP");
        params.put("sid", sub.getSid());
        params.put("partner_order_id", "sub_order_" + sub.getUserId() + "_" + System.currentTimeMillis());
        params.put("partner_user_id", String.valueOf(sub.getUserId()));
        params.put("item_name", sub.getItemName());
        params.put("quantity", 1);
        params.put("total_amount", sub.getAmount());
        params.put("tax_free_amount", 0);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(params, getHeaders());

        KakaoApproveResponse response = restTemplate.postForObject(
                "https://open-api.kakaopay.com/online/v1/payment/subscription",
                request, KakaoApproveResponse.class);

        if (response != null) {
            sub.setTid(response.getTid());
            sub.setLastApprovedAt(response.getApproved_at());
            sub.setNextBillingAt(response.getApproved_at().plusMonths(1));
            subscriptionMapper.updateNextBilling(sub);
        }
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "SECRET_KEY " + adminKey);
        headers.set("Content-Type", "application/json");
        return headers;
    }

    /**
     * 4. 구독 해지 (Cancel)
     */
    @Transactional
    public void cancelSubscription(Long userId) {
        // 1. 해당 유저의 활성 구독 정보 조회
        Subscription sub = subscriptionMapper.findActiveSubscriptionByUserId(userId);
        if (sub == null) throw new CustomException(ErrorResponseCode.SUBSCRIPTION_NOT_FOUND);

        // 2. 카카오페이 정기결제 비활성화(Inactive) API 호출
        Map<String, Object> params = new HashMap<>();
        params.put("cid", "TCSUBSCRIP");
        params.put("sid", sub.getSid());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(params, getHeaders());

        // 카카오 서버에 비활성화 요청 (응답은 성공 여부만 확인)
        restTemplate.postForObject(
                "https://open-api.kakaopay.com/online/v1/payment/manage/subscription/inactive",
                request, Map.class);

        // 3. DB 상태 변경: 구독을 INACTIVE로 변경
        sub.setStatus("INACTIVE");
        subscriptionMapper.updateSubscriptionStatus(sub);

        // 4. 유저 권한을 다시 ROLE_USER로 변경
        User user = userMapper.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorResponseCode.USER_NOT_FOUND));
        user.setRole(UserRole.ROLE_USER);
        userMapper.updateUserRole(user);
    }

    /**
     * 5. 결제 실패 또는 만료 시 권한 강등 처리 (Batch 전용)
     */
    @Transactional
    public void demoteUser(Subscription sub) {
        sub.setStatus("INACTIVE");
        subscriptionMapper.updateSubscriptionStatus(sub);

        User user = userMapper.findById(sub.getUserId()).orElse(null);
        if (user != null) {
            user.setRole(UserRole.ROLE_USER);
            userMapper.updateUserRole(user);
        }
    }
}