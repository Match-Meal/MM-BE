package com.pagoda.matchmeal.integration;

import com.pagoda.matchmeal.config.jwt.JwtTokenProvider;
import com.pagoda.matchmeal.mapper.UserMapper;
import com.pagoda.matchmeal.model.dto.UserDto;
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

import java.time.LocalDateTime;

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

    private User savedUser;

    @BeforeEach
    void setUp() {
        savedUser = User.builder()
                .socialId("google_12345")
                .email("test@test.com")
                .userName("테스트유저")
                .role(UserRole.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .allergies("복숭아,갑각류") // 테스트용 알레르기 데이터 추가
                .statusMessage("화이팅")
                .build();

        userMapper.save(savedUser);
    }

    @Test
    @DisplayName("유효한 토큰으로 내 정보 조회 시 DB의 최신 정보를 반환해야 한다")
    void getMyInfoSuccess() throws Exception {
        // given
        // 토큰 생성에는 ID와 Role만 있으면 됨 (변경된 JwtTokenProvider 로직 반영)
        UserDto tokenDto = UserDto.builder()
                .id(savedUser.getId())
                .socialId(savedUser.getSocialId()) // Subject
                .role(savedUser.getRole().name())
                .build();

        String token = jwtTokenProvider.createAccessToken(tokenDto);

        // when & then
        mockMvc.perform(get("/user/me")
                        .header("Authorization", "Bearer " + token))
                .andDo(print())
                .andExpect(status().isOk())
                // 기본 정보 확인
                .andExpect(jsonPath("$.id").value(savedUser.getId().intValue()))
                .andExpect(jsonPath("$.userName").value("테스트유저"))
                // [중요] DB에서 가져온 프로필 정보가 제대로 매핑되었는지 확인
                .andExpect(jsonPath("$.statusMessage").value("화이팅"))
                .andExpect(jsonPath("$.allergies[0]").value("복숭아"))
                .andExpect(jsonPath("$.allergies[1]").value("갑각류"));
    }

    @Test
    @DisplayName("토큰 없이 호출하면 302 리다이렉트(로그인페이지) 혹은 401 에러가 발생해야 한다")
    void getMyInfoFail() throws Exception {
        // SecurityConfig 설정에 따라 302(Login Page Redirect) 또는 401(Unauthorized)이 뜸
        // oauth2Login()이 활성화되어 있으면 보통 302로 리다이렉트 됨
        mockMvc.perform(get("/user/me"))
                .andExpect(status().is3xxRedirection());
    }
}
