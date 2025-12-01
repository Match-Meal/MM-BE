package com.pagoda.matchmeal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pagoda.matchmeal.config.jwt.JwtTokenProvider;
import com.pagoda.matchmeal.controller.FoodController;
import com.pagoda.matchmeal.model.dto.request.FoodRequestDto;
import com.pagoda.matchmeal.model.entity.User;
import com.pagoda.matchmeal.service.FoodService;
import com.pagoda.matchmeal.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FoodController.class)
class FoodControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FoodService foodService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("음식 생성 API 테스트 - 성공 (인증 주입)")
    void createFood_Success() throws Exception {
        // given
        String socialId = "kakao_12345";
        Long userId = 1L;

        // 1. UserService 가짜 동작 설정
        User mockUser = User.builder().build();
        ReflectionTestUtils.setField(mockUser, "id", userId);
        given(userService.findBySocialId(socialId)).willReturn(mockUser);

        // 2. FoodService 가짜 동작 설정
        given(foodService.addFood(eq(userId), any(FoodRequestDto.class))).willReturn(100L);

        // 3. 요청 데이터 생성
        FoodRequestDto req = new FoodRequestDto();
        ReflectionTestUtils.setField(req, "foodName", "테스트 닭가슴살");

        // ★★★ [핵심] 가짜 인증 객체 생성 (Principal이 String이어야 함) ★★★
        // Controller가 @AuthenticationPrincipal String socialId를 원하기 때문에
        // 첫 번째 인자에 "User객체"가 아니라 "socialId(String)"를 넣어야 합니다.
        Authentication auth = new UsernamePasswordAuthenticationToken(
                socialId, // Principal (String)
                null,     // Credentials
                List.of(new SimpleGrantedAuthority("ROLE_USER")) // Authorities
        );

        // when & then
        mockMvc.perform(post("/foods")
                        .with(authentication(auth)) // ★ 여기에 인증 객체를 넣어주면 302가 안 뜹니다!
                        .with(csrf())               // POST 요청엔 CSRF 토큰 필수
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isOk())         // 이제 200 OK가 뜰 것입니다.
                .andExpect(jsonPath("$.data").value(100L));
    }

    @Test
    @DisplayName("음식 생성 API 테스트 - 실패 (인증 정보 없음)")
    void createFood_Fail_NoAuth() throws Exception {
        // given
        FoodRequestDto req = new FoodRequestDto();
        // (필요하다면 ReflectionTestUtils로 필드 채우기, 근데 어차피 튕겨나갈 거라 상관없음)

        // when & then
        mockMvc.perform(post("/foods")
                        .with(csrf()) // POST 요청은 CSRF 토큰 필요
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andDo(print())
                // [핵심] 컨트롤러 진입 전, Security가 로그인 페이지로 리다이렉트 시킴 (302)
                .andExpect(status().is3xxRedirection());

        // 만약 추후에 SecurityConfig에서 401을 리턴하도록 설정한다면
        // .andExpect(status().isUnauthorized()); 로 바꿔야 합니다.
    }
}