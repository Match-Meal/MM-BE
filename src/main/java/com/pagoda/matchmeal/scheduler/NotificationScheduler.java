package com.pagoda.matchmeal.scheduler;

import com.pagoda.matchmeal.mapper.NotificationMapper;
import com.pagoda.matchmeal.mapper.SubscriptionMapper;
import com.pagoda.matchmeal.mapper.UserMapper;
import com.pagoda.matchmeal.model.dto.SubscriptionAlertDto;
import com.pagoda.matchmeal.model.enums.NotificationType;
import com.pagoda.matchmeal.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final SubscriptionMapper subscriptionMapper;

    /**
     * 1. 매일 아침 8시: 식단 기록 알림
     * 대상: 탈퇴하지 않은 모든 활성 유저
     */
    @Scheduled(cron = "0 0 8 * * *") // 초 분 시 일 월 요일
    public void sendMorningDietAlert() {
        log.info("⏰ [Scheduler] 아침 식단 알림 발송 시작");

        List<Long> allActiveUserIds = userMapper.findAllActiveUserIds();

        for (Long userId : allActiveUserIds) {
            notificationService.sendToUser(
                    userId,
                    null,
                    NotificationType.DAILY_DIET,
                    "☀️ 좋은 아침입니다! 식단을 기록하며 하루를 건강하게 시작해보세요.",
                    0,
                    "/diet/record"
            );
        }
        log.info("✅ [Scheduler] 아침 식단 알림 발송 완료 (총 {}명)", allActiveUserIds.size());
    }

    /**
     * 2. 매일 오후 3시: 구독 결제일 임박 알림
     * 대상: 결제일이 7일, 3일, 1일, 0일(당일) 남은 유저
     */
    @Scheduled(cron = "0 0 15 * * *")
    public void sendSubscriptionPaymentAlert() {
        log.info("⏰ [Scheduler] 구독 결제일 알림 체크 시작");

        // 1. 결제일이 다가온 유저 조회 (D-Day 포함)
        List<SubscriptionAlertDto> dueUsers = subscriptionMapper.findUsersWithUpcomingPayment();

        // 2. 알림 전송
        for (SubscriptionAlertDto user : dueUsers) {
            String message;
            if (user.getDDay() == 0) {
                message = "💳 정기 결제일입니다. 결제 수단을 확인해주세요.";
            } else if (user.getDDay() == 7 ||  user.getDDay() == 3 ||   user.getDDay() == 1) {
                message = "💳 정기 결제일이 " + user.getDDay() + "일 남았습니다.";
            } else continue;

            notificationService.sendToUser(
                    user.getUserId(),
                    null,
                    NotificationType.PAYMENT_ALERT,
                    message,
                    0,
                    "/settings"
            );
        }
        log.info("✅ [Scheduler] 구독 결제 알림 발송 완료 (총 {}명)", dueUsers.size());
    }

    /**
     * 3. 매일 새벽 4시: 오래된 알림 데이터 삭제 (DB 정리)
     * - 조건: 읽음 처리됨(is_read=1) AND 생성된 지 30일 지남
     */
    @Scheduled(cron = "0 0 4 * * *") // 매일 새벽 4시 0분 0초 실행
    public void deleteOldNotifications() {
        int days = 30; // 30일 지난 데이터 삭제 (정책에 따라 60, 90 등으로 변경 가능)

        log.info("🧹 [Scheduler] 오래된 알림 데이터 정리 시작 (기준: {}일 전)", days);

        // Mapper 호출 (반환값이 삭제된 행의 개수일 경우 활용 가능)
        try {
            notificationMapper.deleteOldNotifications(days);
            log.info("✅ [Scheduler] 오래된 알림 삭제 완료");
        } catch (Exception e) {
            log.error("❌ [Scheduler] 알림 삭제 중 오류 발생", e);
        }
    }
}
