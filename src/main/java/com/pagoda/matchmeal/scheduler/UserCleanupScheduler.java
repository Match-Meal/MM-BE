package com.pagoda.matchmeal.scheduler;

import com.pagoda.matchmeal.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserCleanupScheduler {

    private final UserMapper userMapper;

    /**
     * 탈퇴 후 3개월이 지난 계정을 영구 삭제합니다.
     * 실행 주기: 매일 새벽 3시 0분 0초 (서버 부하가 적은 시간)
     * Cron 표현식: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredUsers() {
        log.info("=== [Scheduler] 탈퇴 유저 영구 삭제 작업 시작 ===");

        // 1. 기준 시간 설정 (현재 시간으로부터 3개월 전)
        LocalDateTime thresholdDate = LocalDateTime.now().minusMonths(3);

        try {
            // 2. 삭제 쿼리 실행 (UserMapper에 이미 만들어둔 쿼리 호출)
            // hardDeleteExpiredUsers 쿼리가 int(삭제된 행 개수)를 반환하도록 Mapper 수정 권장
            userMapper.hardDeleteExpiredUsers(thresholdDate);

            log.info("=== [Scheduler] 3개월 지난 탈퇴 계정 정리 완료. 기준일: {} ===", thresholdDate);
        } catch (Exception e) {
            log.error("!!! [Scheduler] 탈퇴 계정 정리 중 오류 발생 !!!", e);
        }
    }
}