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
public class Comment extends BaseEntity {

    private Long commentId;
    private Long userId;
    private Long postId;
    private String content;
    private Long parentCommentId;

    private LocalDateTime deletedAt;
}
