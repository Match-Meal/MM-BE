package com.pagoda.matchmeal.mapper;

import com.pagoda.matchmeal.model.dto.SubscriptionAlertDto;
import com.pagoda.matchmeal.model.entity.Subscription;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface SubscriptionMapper {

    /**
     * 오늘이 결제 예정일이거나 이미 지난 'ACTIVE' 상태의 구독 목록을 조회합니다.
     */
    List<Subscription> findSubscriptionsToBill(LocalDate today);

    void insertSubscription(Subscription subscription);

    Subscription findActiveSubscriptionByUserId(Long userId);

    Subscription findValidSubscriptionByUserId(Long userId);

    void updateNextBilling(Subscription subscription);

    void updateSubscriptionStatus(Subscription subscription);

    List<SubscriptionAlertDto> findUsersWithUpcomingPayment();
}