package com.pagoda.matchmeal.common.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class CustomException extends RuntimeException {

    private final ErrorResponseCode code; // 발생한 에러의 종류
    private List<String> messages; // 상세 에러 메시지 목록
    private String responseBody; // 에러와 함께 클라이언트에 전달할 구체적인 데이터

    /**
     * 일반적 생성자
     * 단순히 에러 코드만으로 예외를 발생시킬 때 사용합니다.
     * @param code  발생한 에러 코드 Enum
     */
    public CustomException(ErrorResponseCode code) {
        this.code = code;
    }

    /**
     * 에러 코드와 함께 상세 메시지나 데이터를 전달해야 할 때 사용하는 생성자
     * @param code          발생한 에러 코드 Enum
     * @param messages      상세 에러 메시지 리스트
     * @param responseBody  에러 관련 추가 데이터
     */
    public CustomException(ErrorResponseCode code, List<String> messages, String responseBody) {
        this.code = code;
        this.messages = messages;
        this.responseBody = responseBody;
    }
}