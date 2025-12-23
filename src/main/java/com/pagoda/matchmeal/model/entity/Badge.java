package com.pagoda.matchmeal.model.entity;

import com.pagoda.matchmeal.model.enums.BadgeCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Badge {

    private Long badgeId;

    private BadgeCategory category; // COMMUNITY, DIET, CHALLENGE, SUBSCRIPTION

    private String subCategory; // POST_COUNT, DIET_STREAK etc.

    private String name;

    private String description;

    private int targetValue;

    private String imageUrl;
    
    private String grayImageUrl;

    private int tier; // 1, 2, 3... (Higher is better)
}
