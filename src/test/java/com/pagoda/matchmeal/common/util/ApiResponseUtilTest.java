package com.pagoda.matchmeal.common.util;

import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import com.pagoda.matchmeal.common.response.CommonResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseUtilTest {

    @Test
    @DisplayName("성공 응답 생성 테스트 - 데이터 없음")
    void success_no_data() {
        // given & when
        CommonResponse<Void> response = ApiResponseUtil.success();

        // then
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("성공");
        assertThat(response.getData()).isNull();
    }

    @Test
    @DisplayName("성공 응답 생성 테스트 - 데이터 포함")
    void success_with_data() {
        // given
        String testData = "테스트 데이터";

        // when
        CommonResponse<String> response = ApiResponseUtil.success(testData);

        // then
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getData()).isEqualTo(testData);
    }

    @Test
    @DisplayName("생성(Created) 응답 테스트")
    void created_test() {
        // when
        CommonResponse<String> response = ApiResponseUtil.created("ID-123");

        // then
        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getMessage()).isEqualTo("생성 성공");
        assertThat(response.getData()).isEqualTo("ID-123");
    }

    @Test
    @DisplayName("실패 응답 생성 테스트 - ErrorCode 사용")
    void failure_with_code() {
        // when
        CommonResponse<Void> response = ApiResponseUtil.failure(ErrorResponseCode.SERVER_ERROR);

        // then
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getMessage()).isEqualTo("서버 에러"); // Enum에 적은 메시지 확인
    }
}