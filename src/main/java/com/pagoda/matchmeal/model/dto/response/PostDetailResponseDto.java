package com.pagoda.matchmeal.model.dto.response;

import com.pagoda.matchmeal.model.dto.UserSimpleDto;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDetailResponseDto {

    private Long postId;
    private String category;
    private String title;
    private String content;
    private UserSimpleDto user;

    private List<String> images;
    private List<String> videos;

    private int viewCount;
    private int likeCount;
    private int commentCount;

    private List<CommentResponseDto> comments;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
