package com.pagoda.matchmeal.model.entity;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Post extends BaseEntity {

    private Long postId;
    private Long userId;
    private String category;
    private String title;
    private String content;
    private int viewCount;

    private LocalDateTime deletedAt;
}
