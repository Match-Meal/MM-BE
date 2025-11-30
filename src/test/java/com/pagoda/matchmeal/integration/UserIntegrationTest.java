package com.pagoda.matchmeal.integration;

import com.pagoda.matchmeal.config.jwt.JwtTokenProvider;
import com.pagoda.matchmeal.mapper.UserMapper;
import com.pagoda.matchmeal.model.entity.User;
import com.pagoda.matchmeal.model.enums.UserRole;
import com.pagoda.matchmeal.model.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // 테스트 끝나고 DB 롤백
public class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        // 테스트 시작 전 DB에 데이터 1개 저장
        User user = User.builder()
                .socialId("google_12345")
                .email("test@test.com")
                .userName("테스트유저")
                .role(UserRole.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();
        userMapper.save(user);
    }

    @Test
    @DisplayName("유효한 토큰으로 내 정보 조회 시 성공해야 한다")
    void getMyInfoSuccess() throws Exception {
        // 1. 토큰 생성 (socialId가 DB에 있는 것과 일치해야 함)
        String token = jwtTokenProvider.createAccessToken("google_12345", "ROLE_USER");

        // 2. API 호출
        mockMvc.perform(get("/user/me")
                        .header("Authorization", "Bearer " + token)) // 헤더에 토큰 주입
                .andDo(print()) // 콘솔에 결과 출력
                .andExpect(status().isOk()) // 200 OK 인가?
                .andExpect(jsonPath("$.userName").value("테스트유저")) // 이름이 맞는가?
                .andExpect(jsonPath("$.email").value("test@test.com"));
    }

    @Test
    @DisplayName("토큰 없이 호출하면 403 Forbidden 에러가 나야 한다")
    void getMyInfoFail() throws Exception {
        mockMvc.perform(get("/user/me"))
                .andExpect(status().isForbidden()); // 403 에러 예상
    }
}
