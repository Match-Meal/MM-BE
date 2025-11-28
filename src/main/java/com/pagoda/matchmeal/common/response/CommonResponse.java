package com.pagoda.matchmeal.common.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
public class CommonResponse<T> {

    private final Integer status; // 응답 상태 코드
    private final String message; // 응답 메세지
    private final T data; // 응답 데이터

    /**
     * 데이터를 포함한 응답을 반환할 때 사용합니다.
     * 주로 GET 요청 성공 시 응답합니다.
     * @param status    상태코드
     * @param message   메세지
     * @param data      반환 응답 데이터
     */
    public CommonResponse(Integer status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    /**
     * 데이터 없이 상태코드와 메세지만 반환할 때 사용됩니다.
     * 주로 POST, PUT, DELETE 성공/실패 응답시 사용됩니다.
     * @param status    상태코드
     * @param message   메세지
     */
    public CommonResponse(Integer status, String message) {
        this.status = status;
        this.message = message;
        this.data = null;
    }

    /**
     * 기본생성자 입니다.
     */
    public CommonResponse() {
        this.status = null;
        this.message = null;
        this.data = null;
    }
}
