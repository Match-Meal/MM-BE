package com.pagoda.matchmeal.model.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PostSearchCond {

    private String keyword;
    private String searchType; // (TITLE, CONTENT, TITLE_CONTENT, WRITER)
    private String category; // 공지(NOTICE), 자유(FREE), 질문(QNA), 식단(DIET)
    private Long userId;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private String sortType; // 정렬 기준 LATEST(최신순), VIEWS(조회수순), LIKES(좋아요순)

    private int limit;
    private long offset;
}
