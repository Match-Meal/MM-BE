package com.pagoda.matchmeal.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostFile {

    private Long fileId;
    private Long postId;
    private String fileUrl;
    private String fileType;
}
