package com.pagoda.matchmeal.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorResponseCode {

    //----------------------------공통코드----------------------------
    SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 에러");

    private final HttpStatus status; // 에러 HTTP 상태 코드
    private final String message; // 에러 메세지
}
