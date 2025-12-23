package com.pagoda.matchmeal.model.dto;

import com.pagoda.matchmeal.model.enums.NotificationType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {

    private Long notificationId;
    private Long receiverId;
    private Long senderId;
    private String senderName;
    private String senderProfileImage;

    private NotificationType notificationType;
    private String content;

    private int relatedId;
    private String relatedUrl;

    private boolean isRead;

    private LocalDateTime createdAt;

}
