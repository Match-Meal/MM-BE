package com.pagoda.matchmeal.common.util;

import com.pagoda.matchmeal.common.response.CommonResponse;
import org.springframework.http.HttpStatus;

public class ApiResponseUtil {

    private static final Integer successStatus = HttpStatus.OK.value();
    private static final Integer CreatedStatus = HttpStatus.CREATED.value();
    private static final String successMessage = "성공";
    private static final String CreatedMessage = "생성 성공";

    // 유틸리티 클래스는 객체 생성을 허용하지 않음
    private ApiResponseUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ------------------ 200 성공 --------------------------------------
    /**
     * 데이터 없이 성공 결과만 반환할 때 사용합니다.
     * @return      상태코드 200과 "성공"메세지가 담긴 CommonResponse
     * @param <T>   응답데이터 타입(null 반환)
     */
    public static <T> CommonResponse<T> success() {
        return new CommonResponse<>(successStatus, successMessage, null);
    }

    /**
     * 데이터를 포함한 성공 결과를 반환할 때 사용합니다.
     * @param data  실제로 전달되는 데이터
     * @return      상태코드 200과 "성공" 메세지와 데이터가 담긴 CommonResponse
     * @param <T>   데이터의 타입
     */
    public static <T> CommonResponse<T> success(T data) {
        return new CommonResponse<>(successStatus, successMessage, data);
    }

    /**
     * 성공 결과와 함께 메세지를 직접 지정하고 싶을 때 사용합니다.
     * @param message   지정할 메세지
     * @param data      실제로 전달되는 데이터
     * @return          상태코드 200과 메세지와 데이터가 담긴 CommonResponse
     * @param <T>       데이터 타입
     */
    public static <T> CommonResponse<T> success(String message, T data) {
        return new CommonResponse<>(successStatus, message, data);
    }

    // ------------------ 201 생성 성공 --------------------------------------

    /**
     * 데이터를 포함한 리소스 생성 성공 결과를 반환할 때 사용합니다.
     * @param data  실제로 전달되는 데이터
     * @return      상태코드 201과 "생성 성공" 메세지와 데이터가 담긴 CommonResponse
     * @param <T>   데이터 타입
     */
    public static <T> CommonResponse<T> created(T data) {
        return new CommonResponse<>(CreatedStatus, CreatedMessage, data);
    }

    /**
     * 리소스 생성 성공 결과와 함께 메세지를 직접 지정하고 싶을 때 사용합니다.
     * @param message   지정할 메세지
     * @param data      실제로 전달되는 데이터
     * @return          상태코드 201과 메세지와 데이터가 담긴 CommonResponse
     * @param <T>       데이터의 타입
     */
    public static <T> CommonResponse<T> created(String message, T data) {
        return new CommonResponse<>(CreatedStatus, message, data);
    }

    // ------------------ 커스텀 코드 --------------------------------------

    /**
     * 상태 코드, 메시지, 데이터를 모두 내 마음대로 설정
     * @param status    상태코드
     * @param message   메세지
     * @param data      실제로 전달되는 데이터
     * @return          내 맘대로 설정한 상태코드, 메세지, 데이터가 담긴 CommonResponse
     * @param <T>       데이터의 타입
     */
    public static <T> CommonResponse<T> of(HttpStatus status, String message, T data) {
        return new CommonResponse<>(status.value(), message, data);
    }
}
