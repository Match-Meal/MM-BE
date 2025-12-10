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
//    INVALID_FILE(HttpStatus.)

    //----------------------------음식DB 에러코드----------------------------
    FOOD_NOT_FOUND(HttpStatus.NOT_FOUND, "음식 데이터를 찾을 수 없습니다."),

    //---------------------------- 유저 에러코드----------------------------
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."),

    //----------------------------식단 에러코드----------------------------
    DIET_NOT_FOUND(HttpStatus.NOT_FOUND, "식단 데이터를 찾을 수 없습니다."),

    //----------------------------파일 에러코드----------------------------
    INVALID_FILE(HttpStatus.BAD_REQUEST, "유효하지 않은 파일입니다."),
    FILE_UPLOAD_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드 중 오류가 발생했습니다."),
    FILE_DELETE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제 중 오류가 발생했습니다."),
    INVALID_FILE_EXTENSION(HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식입니다."),

    //----------------------------팔로우 에러코드----------------------------
    SELF_FOLLOW_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "자기 자신을 팔로우 할 수 없습니다.");

    private final HttpStatus status; // 에러 HTTP 상태 코드
    private final String message; // 에러 메세지
}
