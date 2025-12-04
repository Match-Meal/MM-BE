package com.pagoda.matchmeal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class OAuth2LoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("구글 로그인 URL 접속 시 구글 인증 서버로 리다이렉트 된다")
    void googleLoginRedirectTest() throws Exception {
        // when & then
        mockMvc.perform(get("/oauth2/authorization/google")) // 1. 로그인 시도 URL
                .andExpect(status().is3xxRedirection()) // 2. 302 리다이렉트 응답 확인
                // 3. 이동하려는 주소가 구글(accounts.google.com)인지 확인
                .andExpect(header().string("Location", containsString("accounts.google.com")));
    }
}