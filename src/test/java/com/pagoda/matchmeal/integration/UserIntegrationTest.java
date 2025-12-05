package com.pagoda.matchmeal.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    @Autowired
    private ObjectMapper objectMapper;

    private User savedUser;
    private String token;

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
                .isPublic(true)
                .build();

        userMapper.save(savedUser);

        // 토큰 생성
        UserDto tokenDto = UserDto.builder()
                .id(savedUser.getId())
                .socialId(savedUser.getSocialId())
                .role(savedUser.getRole().name())
                .build();
        token = jwtTokenProvider.createAccessToken(tokenDto);
    }

    @Test
    @DisplayName("유효한 토큰으로 내 정보 조회 시 DB의 최신 정보를 반환해야 한다")
    void getMyInfoSuccess() throws Exception {
        // given
        // token 생성 로직을 setUp으로 옮김

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

    @Test
    @DisplayName("공개 설정 변경 API 호출 시 DB 값이 변경되어야 한다.")
    void updateVisibilitySuccess() throws Exception {
        // given
        Map<String, Boolean> request = Map.of("isPublic", false);

        // when
        mockMvc.perform(patch("/user/visibility")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());

        // thend
        User updatedUser = userMapper.findById(savedUser.getId()).orElseThrow();

        if (updatedUser.getIsPublic()) {
            throw new AssertionError("Visibility should be updated to false");
        }
    }

    @Test
    @DisplayName("타인 프로필 조회 API 호출 성공")
    void getUserProfileSuccess() throws Exception {
        // when & then
        mockMvc.perform(get("/user/" + savedUser.getId())
                        .header("Authorization", "Bearer " + token))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("테스트유저"));
    }
}
