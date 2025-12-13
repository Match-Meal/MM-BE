package com.pagoda.matchmeal.model.entity;

import com.pagoda.matchmeal.model.enums.PostCategory;
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
    private PostCategory category;
    private String title;
    private String content;
    private int viewCount;

    private LocalDateTime deletedAt;
}
