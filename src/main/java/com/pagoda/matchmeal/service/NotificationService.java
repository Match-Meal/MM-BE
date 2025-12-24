package com.pagoda.matchmeal.service;

import com.pagoda.matchmeal.model.dto.NotificationDto;
import com.pagoda.matchmeal.model.enums.NotificationType;

import java.util.List;

public interface NotificationService {

    void sendToUser(Long receiverId, Long senderId, NotificationType type, String content, int relatedId, String relatedUrl);

    void sendToTopic(NotificationType type, String content, String relatedUrl);

    List<NotificationDto> getMyNotifications(Long userId);

    int getUnreadCount(Long userId);

    void markAsRead(Long notificationId);

    void deleteAll(Long userId);
}
