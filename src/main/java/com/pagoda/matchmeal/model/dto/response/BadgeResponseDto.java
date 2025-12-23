package com.pagoda.matchmeal.model.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BadgeResponseDto {
    private Long badgeId;
    private String name;
    private String description;
    private String imageUrl; // Gray or Color based on acquisition
    private boolean isAcquired;
    private int currentValue;
    private int targetValue;
    private int tier;
}
