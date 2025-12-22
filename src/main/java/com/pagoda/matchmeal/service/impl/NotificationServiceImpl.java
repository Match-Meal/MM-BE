package com.pagoda.matchmeal.service.impl;

import com.pagoda.matchmeal.mapper.NotificationMapper;
import com.pagoda.matchmeal.model.dto.NotificationDto;
import com.pagoda.matchmeal.model.enums.NotificationType;
import com.pagoda.matchmeal.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationMapper notificationMapper;

    /**
     * 1. [개인 알림 전송] - 대부분의 알림은 이걸 사용합니다.
     * DB에 저장하고, 해당 유저에게만 실시간 알림을 보냅니다.
     * 사용처: 결제일 알림, 팔로우, 댓글, 좋아요, 챌린지, (개인화된) 식단알림
     */
    @Override
    @Transactional
    public void sendToUser(Long receiverId, Long senderId, NotificationType type, String content, int relatedId, String relatedUrl) {
        // 1-1. DTO 생성 (DB 저장용)
        NotificationDto notification = NotificationDto.builder()
                .receiverId(receiverId)
                .senderId(senderId) // 시스템 알림이면 null
                .notificationType(type)
                .content(content)
                .relatedId(relatedId)
                .relatedUrl(relatedUrl)
                .isRead(false)
                .build();

        // 1-2. DB 저장 (MyBatis)
        notificationMapper.save(notification);

        // 1-3. 실시간 전송 (WebSocket -> /user/{id}/queue/notifications)
        // convertAndSendToUser는 내부적으로 "/user/{username}/..." 경로로 변환해서 보냅니다.
        // 클라이언트는 "/user/queue/notifications"를 구독해야 합니다.
        messagingTemplate.convertAndSendToUser(
                String.valueOf(receiverId), // 받는 사람 식별자 (User ID)
                "/queue/notifications",     // 구독 경로
                notification                // 보낼 데이터
        );
    }

    /**
     * 2. [전체 방송] - 공지사항 등
     * DB 저장은 하지 않고(또는 별도 공지 테이블 사용), 접속한 모든 유저에게 팝업을 띄웁니다.
     * 사용처: 공지사항, 긴급 점검 알림
     */
    @Override
    public void sendToTopic(NotificationType type, String content, String relatedUrl) {
        NotificationDto notification = NotificationDto.builder()
                .receiverId(0L) // 전체 알림이라 특정 수신자 없음 (표시용)
                .notificationType(type)
                .content(content)
                .relatedUrl(relatedUrl)
                .build();

        // 2-1. 실시간 전송 (WebSocket -> /topic/global)
        // 클라이언트는 "/topic/global"을 구독해야 합니다.
        messagingTemplate.convertAndSend("/topic/global", notification);
    }

    @Override
    public List<NotificationDto> getMyNotifications(Long userId) {
        return notificationMapper.findByReceiverId(userId);
    }

    @Override
    public int getUnreadCount(Long userId) {
        return notificationMapper.countUnread(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        notificationMapper.markAsRead(notificationId);
    }

    @Override
    @Transactional
    public void deleteAll(Long userId) {
        notificationMapper.deleteAllByReceiverId(userId);
    }
}
