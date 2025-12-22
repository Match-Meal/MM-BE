package com.pagoda.matchmeal.mapper;

import com.pagoda.matchmeal.model.dto.NotificationDto;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface NotificationMapper {
    // 알림 저장
    void save(NotificationDto notificationDto);

    // 내 알림 목록 조회 (보낸 사람 정보 JOIN 포함)
    List<NotificationDto> findByReceiverId(@Param("receiverId") Long receiverId);

    // 알림 읽음 처리
    void markAsRead(@Param("notificationId") Long notificationId);

    // 안 읽은 알림 개수 (뱃지용)
    int countUnread(@Param("receiverId") Long receiverId);

    // 특정 기간 이전의 읽은 알림 삭제 (DB 용량 관리용)
    void deleteOldNotifications(@Param("days") int days);

    void deleteAllByReceiverId(@Param("receiverId") Long receiverId);
}