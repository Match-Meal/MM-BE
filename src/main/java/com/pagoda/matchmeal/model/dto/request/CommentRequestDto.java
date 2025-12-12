package com.pagoda.matchmeal.model.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class CommentRequestDto {

    private String content;
    private Long parentCommentId;
}
