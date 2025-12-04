package com.pagoda.matchmeal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pagoda.matchmeal.config.jwt.JwtTokenProvider;
import com.pagoda.matchmeal.controller.FoodController;
import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.model.dto.request.FoodRequestDto;
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
    @DisplayName("음식 생성 API 테스트 - 성공 (UserDto 인증 주입)")
    void createFood_Success() throws Exception {
        // given
        Long userId = 1L;
        String socialId = "kakao_12345";

        // 1. [변경] Controller가 사용할 UserDto 객체 생성
        // (UserDto에 @Builder가 있다고 가정했습니다. 없다면 생성자나 Setter를 사용하세요)
        UserDto mockUserDto = UserDto.builder()
                .id(userId)
                .socialId(socialId)
                .build();

        // 2. [삭제] UserService 관련 모킹 삭제
        // 컨트롤러 리팩토링으로 userService.findBySocialId() 호출이 사라졌으므로 불필요합니다.

        // 3. FoodService 가짜 동작 설정
        // 컨트롤러는 UserDto에서 꺼낸 userId(1L)를 서비스에 넘깁니다.
        given(foodService.addFood(eq(userId), any(FoodRequestDto.class))).willReturn(100L);

        // 4. 요청 데이터 생성
        FoodRequestDto req = new FoodRequestDto();
        ReflectionTestUtils.setField(req, "foodName", "테스트 닭가슴살");

        // ★★★ [핵심] 인증 객체 생성 (Principal이 UserDto여야 함) ★★★
        // 컨트롤러의 @AuthenticationPrincipal UserDto userDto 파라미터에 매핑되도록
        // 첫 번째 인자(Principal)에 위에서 만든 mockUserDto 객체를 넣습니다.
        Authentication auth = new UsernamePasswordAuthenticationToken(
                mockUserDto, // Principal (여기 들어간 객체가 컨트롤러의 파라미터로 전달됨)
                null,        // Credentials
                List.of(new SimpleGrantedAuthority("ROLE_USER")) // Authorities
        );

        // when & then
        mockMvc.perform(post("/foods")
                        .with(authentication(auth)) // MockMvc에 인증 정보 주입
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isOk())
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