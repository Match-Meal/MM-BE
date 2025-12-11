package com.pagoda.matchmeal.model.dto.response;

import com.pagoda.matchmeal.model.dto.UserSimpleDto;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponseDto {

    private Long commentId;
    private String content;

    private UserSimpleDto user;

    private int likeCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
