package com.pagoda.matchmeal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class oauth2LoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("구글 로그인 로직 검증")
    void oauth2LoginTest() throws Exception {
        // when & then
        mockMvc.perform(get("/") // 혹은 인증이 필요한 아무 API (예: /api/user/me)
                        // 가상의 구글 로그인 유저 정보를 주입
                        .with(oauth2Login()
                                .attributes(attributes -> {
                                    attributes.put("sub", "google_12345");
                                    attributes.put("name", "테스트유저");
                                    attributes.put("email", "test@gmail.com");
                                })))
                .andExpect(status().isOk()); // 로그인이 성공해서 200 OK가 떨어지는지 확인
        // 만약 리다이렉트 설정이 되어 있다면 status().is3xxRedirection() 으로 확인해야 할 수도 있음
    }

    @Test
    @DisplayName("로그인 없이 접근 시 차단 테스트")
    void loginFailTest() throws Exception {
        mockMvc.perform(get("/api/user/me")) // 인증 필요한 URL
                .andExpect(status().is3xxRedirection()); // 로그인 페이지로 리다이렉트(302) 됨
    }
}
