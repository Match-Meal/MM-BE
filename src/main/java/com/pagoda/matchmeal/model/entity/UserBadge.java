package com.pagoda.matchmeal.model.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserBadge {

    private Long userBadgeId;

    private Long userId; // Assuming direct mapping or relation

    private Badge badge;

    private int currentValue; // Current progress count

    private boolean isAcquired;

    private LocalDateTime acquiredAt;
}
