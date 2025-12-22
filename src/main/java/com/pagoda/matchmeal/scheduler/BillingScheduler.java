package com.pagoda.matchmeal.scheduler;

import com.pagoda.matchmeal.mapper.SubscriptionMapper;
import com.pagoda.matchmeal.model.entity.Subscription;
import com.pagoda.matchmeal.service.KakaoPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingScheduler {

    private final KakaoPayService kakaoPayService;
    private final SubscriptionMapper subscriptionMapper;

    /**
     * 매일 오전 9시에 실행되어 정기 결제 대상자에게 결제를 요청합니다.
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void executeAutoBilling() {
        LocalDate today = LocalDate.now();
        List<Subscription> targetList = subscriptionMapper.findSubscriptionsToBill(today);

        for (Subscription sub : targetList) {
            try {
                // 정기 결제 시도
                kakaoPayService.executeRecurringPayment(sub);
            } catch (Exception e) {
                log.error("결제 실패 또는 기한 만료 - 유저 ID: {}, 사유: {}", sub.getUserId(), e.getMessage());

                // 결제 실패 시 즉시 일반 유저로 강등 (한 달 기한 만료 처리)
                kakaoPayService.demoteUser(sub);
            }
        }
    }
}