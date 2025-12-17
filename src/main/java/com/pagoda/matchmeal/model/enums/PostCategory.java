package com.pagoda.matchmeal.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PostCategory {

    DIET("식단"),
    FREE("자유"),
    QNA("질문"),
    INFO("정보"),
    NOTICE("공지");

    private final String desc;
}
