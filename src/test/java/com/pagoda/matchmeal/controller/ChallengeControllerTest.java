package com.pagoda.matchmeal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pagoda.matchmeal.annotation.WithCustomMockUser;
import com.pagoda.matchmeal.config.jwt.JwtTokenProvider;
import com.pagoda.matchmeal.model.dto.ChallengeSearchCondition;
import com.pagoda.matchmeal.model.dto.request.ChallengeCreateRequestDto;
import com.pagoda.matchmeal.model.dto.response.ChallengeResponseDto;
import com.pagoda.matchmeal.model.enums.ChallengeType;
import com.pagoda.matchmeal.service.ChallengeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChallengeController.class)
@AutoConfigureMockMvc(addFilters = false) // Security 필터 비활성화 (순수 컨트롤러 로직만 테스트)
class ChallengeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChallengeService challengeService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("챌린지 생성 - 성공 (201 Created)")
    @WithCustomMockUser
        // 인증된 가짜 유저
    void createChallenge_Success() throws Exception {
        // given
        ChallengeCreateRequestDto requestDto = new ChallengeCreateRequestDto();
        // DTO 필드 세팅 (Reflection or Builder 사용 가정, 혹은 public 필드라면 직접 할당)
        // requestDto.setTitle("테스트 챌린지"); ...

        // Service가 1L을 반환한다고 가정
        given(challengeService.createChallenge(any(), any(ChallengeCreateRequestDto.class)))
                .willReturn(1L);

        // when & then
        mockMvc.perform(post("/challenge")
                        .with(csrf()) // POST 요청 시 CSRF 토큰 필요
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated()) // 201 확인
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.data").value(1L));
    }

    @Test
    @DisplayName("공개 챌린지 검색 - 성공 (200 OK)")
    @WithCustomMockUser
    void searchChallenges_Success() throws Exception {
        // given
        ChallengeResponseDto responseDto = ChallengeResponseDto.builder()
                .challengeId(1L)
                .title("검색된 챌린지")
                .type(ChallengeType.CALORIE_LIMIT)
                .build();

        // 어떤 조건이 오든 리스트 반환 모킹
        given(challengeService.searchChallenges(any(ChallengeSearchCondition.class)))
                .willReturn(List.of(responseDto));

        // when & then
        mockMvc.perform(get("/challenge/search")
                        .param("type", "CALORIE_LIMIT")
                        .param("keyword", "검색")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("검색된 챌린지"))
                .andExpect(jsonPath("$.data[0].type").value("CALORIE_LIMIT"));
    }

    @Test
    @DisplayName("내 챌린지 전체 조회 - 성공 (200 OK)")
    @WithCustomMockUser
    void getAllChallenges_Success() throws Exception {
        // given
        ChallengeResponseDto responseDto = ChallengeResponseDto.builder()
                .challengeId(10L)
                .title("내 챌린지")
                .currentStreak(5)
                .build();

        given(challengeService.getAllChallenges(any()))
                .willReturn(List.of(responseDto));

        // when & then
        mockMvc.perform(get("/challenge")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].currentStreak").value(5));
    }

    @Test
    @DisplayName("공개 챌린지 참여 - 성공")
    @WithCustomMockUser
    void joinPublicChallenge_Success() throws Exception {
        // given
        Long challengeId = 1L;

        // when & then
        mockMvc.perform(post("/challenge/{challengeId}/join", challengeId)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());

        // verify: 서비스 메서드가 호출되었는지 검증
        verify(challengeService).joinPublicChallenge(any(), eq(challengeId));
    }

    @Test
    @DisplayName("비공개 챌린지 코드로 참여 - 성공")
    @WithCustomMockUser
    void joinByCode_Success() throws Exception {
        // given
        String code = "ABC12345";

        // when & then
        mockMvc.perform(post("/challenge/join/code")
                        .param("code", code) // 쿼리 파라미터
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());

        verify(challengeService).joinByCode(any(), eq(code));
    }

    @Test
    @DisplayName("친구 초대 - 성공")
    @WithCustomMockUser
    void inviteUser_Success() throws Exception {
        // given
        Long challengeId = 1L;
        Long targetUserId = 99L;

        // when & then
        mockMvc.perform(post("/challenge/{challengeId}/invite", challengeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(targetUserId)) // Body에 ID 전송
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());

        verify(challengeService).inviteUser(any(), eq(challengeId), eq(targetUserId));
    }
}