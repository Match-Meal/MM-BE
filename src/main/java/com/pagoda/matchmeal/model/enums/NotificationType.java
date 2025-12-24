package com.pagoda.matchmeal.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotificationType {

    // 1. 아침 식단 기록 알림
    DAILY_DIET("식단 기록"),

    // 2. 공지사항
    NOTICE("공지사항"),

    // 3. 팔로우/팔로잉
    FOLLOW("팔로우"),

    // 4. 팔로잉 유저 게시물 업로드
    FOLLOWING_POST("새 게시물"),

    // 5. 게시글 좋아요/댓글
    POST_LIKE("좋아요"),
    COMMENT("댓글"),

    // 6. 구독 결제일 알림 (7일전, 3일전, 하루전, 당일 등)
    PAYMENT_ALERT("결제 알림"),

    // 7. 챌린지 초대
    CHALLENGE_INVITE("챌린지 초대");

    private final String description;
}
