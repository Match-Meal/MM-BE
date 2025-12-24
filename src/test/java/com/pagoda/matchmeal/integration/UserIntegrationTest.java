//package com.pagoda.matchmeal.integration;
//
//import com.pagoda.matchmeal.common.config.jwt.JwtTokenProvider;
//import com.pagoda.matchmeal.mapper.UserMapper;
//import com.pagoda.matchmeal.model.dto.UserDto;
//import com.pagoda.matchmeal.model.dto.UserProfileDto;
//import com.pagoda.matchmeal.model.entity.User;
//import com.pagoda.matchmeal.model.enums.UserRole;
//import com.pagoda.matchmeal.support.IntegrationTestSupport;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.MediaType;
//import org.springframework.mock.web.MockMultipartFile;
//
//import java.nio.charset.StandardCharsets;
//import java.util.List;
//import java.util.Map;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.BDDMockito.given;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//class UserIntegrationTest extends IntegrationTestSupport {
//
//    @Autowired
//    private UserMapper userMapper;
//
//    @Autowired
//    private JwtTokenProvider jwtTokenProvider; // [수정] 타입 및 변수명 변경
//
//    private String accessToken;
//    private Long userId;
//
//    @BeforeEach
//    void setUp() {
//        // 1. 테스트용 유저 생성 (DB Insert)
//        User user = User.builder()
//                .email("test@test.com")
//                .socialId("google_12345")
//                .platform("google")
//                .userName("테스트유저")
//                .role(UserRole.ROLE_USER) // [수정] UserRole 사용
//                .isPublic(true)
//                .statusMessage("화이팅")
//                .profileImage("http://img.com/default.jpg")
//                .build();
//
//        userMapper.save(user); // insert
//        this.userId = user.getUserId();
//
//        // 2. 토큰 발급 (UserDto 직접 빌드)
//        UserDto userDto = UserDto.builder()
//                .id(user.getUserId())
//                .email(user.getEmail())
//                .userName(user.getUserName())
//                .role(user.getRole().name())
//                .build();
//
//        this.accessToken = jwtTokenProvider.createAccessToken(userDto); // [수정] 메서드 호출
//    }
//
//    @Test
//    @DisplayName("유효한 토큰으로 내 정보 조회 시 DB의 최신 정보를 반환해야 한다")
//    void getMyInfo_Success() throws Exception {
//        mockMvc.perform(get("/user/me")
//                        .header("Authorization", "Bearer " + accessToken)
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andDo(print())
//                .andExpect(status().isOk())
//                // [검증] 응답 JSON 구조 (data.xxx) 확인
//                .andExpect(jsonPath("$.data.id").value(userId))
//                .andExpect(jsonPath("$.data.userName").value("테스트유저"))
//                .andExpect(jsonPath("$.data.statusMessage").value("화이팅"));
//    }
//
//    @Test
//    @DisplayName("토큰 없이 호출하면 302 리다이렉트(로그인페이지) 혹은 401 에러가 발생해야 한다")
//    void getMyInfo_Fail_NoToken() throws Exception {
//        mockMvc.perform(get("/user/me"))
//                .andExpect(result -> {
//                    int status = result.getResponse().getStatus();
//                    // 상태 코드가 401이거나 302인지 확인
//                    assertTrue(status == 401 || status == 302,
//                            "상태 코드는 401 또는 302여야 하는데, 실제로는 " + status + "입니다.");
//                });
//    }
//
//    @Test
//    @DisplayName("공개 설정 변경 API 호출 시 DB 값이 변경되어야 한다.")
//    void updateVisibility_Success() throws Exception {
//        // given
//        Map<String, Boolean> request = Map.of("isPublic", false);
//
//        // when
//        mockMvc.perform(patch("/user/visibility")
//                        .header("Authorization", "Bearer " + accessToken)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andDo(print())
//                .andExpect(status().isOk());
//
//        // then
//        User updatedUser = userMapper.findById(userId).orElseThrow();
//        assertThat(updatedUser.getIsPublic()).isFalse();
//    }
//
//    @Test
//    @DisplayName("타인 프로필 조회 API 호출 성공")
//    void getUserProfile_Success() throws Exception {
//        // when & then
//        mockMvc.perform(get("/user/" + userId)
//                        .header("Authorization", "Bearer " + accessToken)
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data.userName").value("테스트유저"));
//    }
//
//    @Test
//    @DisplayName("프로필 업데이트 API (이미지 + JSON) 성공 테스트")
//    void updateProfileWithImage_Success() throws Exception {
//        // given
//        UserProfileDto profileDto = new UserProfileDto();
//        profileDto.setUserName("이미지업로드성공");
//        profileDto.setHeightCm(185.0);
//        profileDto.setAllergies(List.of("복숭아"));
//
//        String profileJson = objectMapper.writeValueAsString(profileDto);
//
//        // JSON 데이터 파트
//        MockMultipartFile dataPart = new MockMultipartFile(
//                "data", "", "application/json", profileJson.getBytes(StandardCharsets.UTF_8)
//        );
//
//        // 파일 데이터 파트
//        MockMultipartFile filePart = new MockMultipartFile(
//                "file", "new.jpg", "image/jpeg", "image_data".getBytes()
//        );
//
//        // S3 Mocking (가짜 URL 리턴)
//        String mockUrl = "https://s3.com/new.jpg";
//        given(s3Service.uploadFile(any(), any(String.class))).willReturn(mockUrl);
//
//        // when
//        mockMvc.perform(multipart(HttpMethod.PUT, "/user/profile")
//                        .file(dataPart)
//                        .file(filePart)
//                        .header("Authorization", "Bearer " + accessToken)
//                        .contentType(MediaType.MULTIPART_FORM_DATA))
//                .andDo(print())
//                .andExpect(status().isOk());
//
//        // then
//        User updatedUser = userMapper.findById(userId).orElseThrow();
//        assertThat(updatedUser.getProfileImage()).isEqualTo(mockUrl);
//        assertThat(updatedUser.getUserName()).isEqualTo("이미지업로드성공");
//    }
//
//    @Test
//    @DisplayName("회원 탈퇴 API 호출 시 성공해야 함")
//    void withdrawUser_Success() throws Exception {
//        // RedisService와 SocialUnlinkService가 IntegrationTestSupport에서 Mocking 되어 있으므로 성공
//        mockMvc.perform(delete("/user/withdraw")
//                        .header("Authorization", "Bearer " + accessToken))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.message").value("성공"));
//    }
//}