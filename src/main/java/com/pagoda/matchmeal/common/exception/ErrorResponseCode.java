package com.pagoda.matchmeal.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorResponseCode {

    //----------------------------공통코드----------------------------
    SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 에러"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "접근 권한이 없습니다."),

    //----------------------------음식DB 에러코드----------------------------
    FOOD_NOT_FOUND(HttpStatus.NOT_FOUND, "음식 데이터를 찾을 수 없습니다."),

    //----------------------------식단 에러코드----------------------------
    DIET_NOT_FOUND(HttpStatus.NOT_FOUND, "식단 데이터를 찾을 수 없습니다.");

    private final HttpStatus status; // 에러 HTTP 상태 코드
    private final String message; // 에러 메세지
}
