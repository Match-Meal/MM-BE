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
        // [중요] ID는 직접 넣지 않고(null), DB가 Auto Increment로 생성하게 둡니다.
        savedUser = User.builder()
                .socialId("google_12345")
                .email("test@test.com")
                .userName("테스트유저")
                .role(UserRole.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        // save가 실행되면 MyBatis가 생성된 ID를 savedUser 객체에 채워줍니다. (useGeneratedKeys 덕분)
        userMapper.save(savedUser);
    }

    @Test
    @DisplayName("유효한 토큰으로 내 정보 조회 시 성공해야 한다")
    void getMyInfoSuccess() throws Exception {
        // given
        UserDto userDto = UserDto.builder()
                .id(savedUser.getId()) // DB에서 생성된 실제 ID 사용 (1, 2, 3...)
                .socialId(savedUser.getSocialId())
                .userName(savedUser.getUserName())
                .role(savedUser.getRole().name())
                .createdAt(savedUser.getCreatedAt().toString())
                .build();

        String token = jwtTokenProvider.createAccessToken(userDto);

        // when & then
        mockMvc.perform(get("/user/me")
                        .header("Authorization", "Bearer " + token))
                .andDo(print())
                .andExpect(status().isOk())
                // ★ [수정] 1L 이라고 박지 말고, 실제 저장된 ID와 같은지 비교합니다.
                // .intValue()를 붙여주는 이유는 JSON 응답이 Integer로 올 수 있기 때문입니다.
                .andExpect(jsonPath("$.id").value(savedUser.getId().intValue()))
                .andExpect(jsonPath("$.socialId").value("google_12345"))
                .andExpect(jsonPath("$.userName").value("테스트유저"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("토큰 없이 호출하면 302 에러가 나야 한다")
    void getMyInfoFail() throws Exception {
        mockMvc.perform(get("/user/me"))
                .andExpect(status().is3xxRedirection());
    }
}
