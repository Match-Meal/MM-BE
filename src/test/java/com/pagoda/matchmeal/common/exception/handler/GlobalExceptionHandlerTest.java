package com.pagoda.matchmeal.common.exception.handler;

import com.pagoda.matchmeal.common.exception.CustomException;
import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    // ------------------------------------------------------------
    // [설정] 테스트 전용 컨트롤러를 Bean으로 등록하는 설정 클래스
    // ------------------------------------------------------------
    @TestConfiguration
    static class Config {
        @Bean
        public TestController testController() {
            return new TestController();
        }
    }

    // ------------------------------------------------------------
    // [테스트 1] CustomException 처리 테스트
    // ------------------------------------------------------------
    @Test
    @DisplayName("비즈니스 로직에서 CustomException 발생 시 정해진 포맷으로 응답한다")
    void handleCustomException() throws Exception {
        // when
        // TestController의 /test/custom 엔드포인트 호출 (SERVER_ERROR 발생시킴)
        ResultActions result = mockMvc.perform(get("/test/custom"));

        // then
        result.andExpect(status().isInternalServerError()) // 500 Expect
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("서버 에러")) // ErrorResponseCode.SERVER_ERROR 메시지
                .andDo(print());
    }

    // ------------------------------------------------------------
    // [테스트 2] 일반 Exception 처리 테스트
    // ------------------------------------------------------------
    @Test
    @DisplayName("예상치 못한 RuntimeException 발생 시 SERVER_ERROR로 처리한다")
    void handleGeneralException() throws Exception {
        // when
        ResultActions result = mockMvc.perform(get("/test/general"));

        // then
        // GlobalExceptionHandler.handleException() 로직에 따라 SERVER_ERROR가 리턴되어야 함
        result.andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("서버 에러"))
                .andDo(print());
    }

    // ------------------------------------------------------------
    // [테스트 3] @Valid 유효성 검사 실패 테스트
    // ------------------------------------------------------------
    @Test
    @DisplayName("DTO Validation 실패 시 400 에러와 필드 에러 메시지를 반환한다")
    void handleValidationException() throws Exception {
        // given
        // name 필드가 null인 잘못된 JSON 요청 본문
        String invalidJson = "{\"name\": null}";

        // when
        ResultActions result = mockMvc.perform(post("/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson));

        // then
        result.andExpect(status().isBadRequest()) // 400 Expect
                .andExpect(jsonPath("$.status").value(400))
                // GlobalExceptionHandler에서 "필드명 : 메세지" 형태로 가공했는지 확인
                .andExpect(jsonPath("$.message").value("name : 널이어서는 안됩니다")) 
                .andDo(print());
    }

    // ------------------------------------------------------------
    // [내부 클래스] 테스트용 더미 컨트롤러 및 DTO
    // ------------------------------------------------------------
    @RestController
    static class TestController {

        @GetMapping("/test/custom")
        public void throwCustomException() {
            // 제공해주신 ErrorResponseCode에 SERVER_ERROR만 있어서 이것을 사용
            throw new CustomException(ErrorResponseCode.SERVER_ERROR);
        }

        @GetMapping("/test/general")
        public void throwGeneralException() {
            throw new RuntimeException("알 수 없는 에러");
        }

        @PostMapping("/test/validation")
        public void validateInput(@Valid @RequestBody TestDto dto) {
            // Validation 통과 여부만 확인하므로 바디 없음
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    static class TestDto {
        @NotNull(message = "널이어서는 안됩니다") // 테스트용 메시지 지정
        private String name;
    }
}