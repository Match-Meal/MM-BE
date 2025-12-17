package com.pagoda.matchmeal.model.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostFileResponseDto {
    private Long fileId;   // [핵심] 삭제 요청 시 식별자로 사용
    private String fileUrl;
    private String fileType; // IMAGE, VIDEO 등 (필요 시)
}