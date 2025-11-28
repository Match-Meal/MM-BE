package com.pagoda.matchmeal.common.exception.handler;

import com.pagoda.matchmeal.common.exception.CustomException;
import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import com.pagoda.matchmeal.common.response.CommonResponse;
import com.pagoda.matchmeal.common.util.ApiResponseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 전역 예외 처리 핸들러 (Global Exception Handler)
 * 시스템 전반에서 발생하는 모든 예외를 잡아 'CommonResponse' 포맷으로 통일하여 응답합니다.
 * ResponseEntityExceptionHandler를 상속받아 Spring MVC 표준 예외(400, 404, 405 등)도
 * 우리가 정의한 포맷으로 일관되게 처리합니다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = Exception.class)
    protected ResponseEntity<CommonResponse<Object>> handleException(Exception e) {
        log.error("Exception", e);
        return toErrrorResponseEntity(ErrorResponseCode.SERVER_ERROR);
    }

    @ExceptionHandler(value = CustomException.class)
    protected ResponseEntity<CommonResponse<Object>> handleCustomException(CustomException e) {
        log.warn("CustomException", e);
        return toErrrorResponseEntity(e.getCode());
    }

    protected ResponseEntity<CommonResponse<Object>> toErrrorResponseEntity(ErrorResponseCode customResponseCode) {
        return ResponseEntity
                .status(customResponseCode.getStatus())
                .body(ApiResponseUtil.failure(customResponseCode));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        log.error("Exception", exception);
        CommonResponse<Object> response = ApiResponseUtil.failure(statusCode.value(), exception.getMessage());
        return ResponseEntity.status(statusCode).body(response);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.warn("ValidationException", ex);

        FieldError fistFieldError = ex.getBindingResult().getFieldErrors().get(0);
        CommonResponse<Object> response = ApiResponseUtil.failure(status.value(), fistFieldError.getField() + " : " + fistFieldError.getDefaultMessage());

        return ResponseEntity.status(status).body(response);
    }
}