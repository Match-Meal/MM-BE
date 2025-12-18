package com.pagoda.matchmeal.controller;

import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.model.dto.response.FollowListDto;
import com.pagoda.matchmeal.service.FollowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FollowControllerTest {

    @InjectMocks
    private FollowController followController;

    @Mock
    private FollowService followService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // @AuthenticationPrincipal을 Mocking하기 위해 ArgumentResolver를 커스텀 설정
        mockMvc = MockMvcBuilders.standaloneSetup(followController)
                .setCustomArgumentResolvers(new MockAuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("팔로워 목록 조회 - 로그인 상태 (viewerId 전달)")
    void getFollowers_LoggedIn() throws Exception {
        // given
        Long targetId = 2L;
        Long viewerId = 5L; // Mock Resolver가 5L을 반환하도록 설정됨

        List<FollowListDto> mockResponse = List.of(
                FollowListDto.builder().userId(1L).userName("Fan1").isFollowing(false).build()
        );

        // Service 호출 시 viewerId가 5L로 넘어가는지 검증
        given(followService.getFollowers(eq(targetId), eq(viewerId))).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/user/{userId}/followers", targetId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userName").value("Fan1"));

        verify(followService).getFollowers(targetId, viewerId);
    }

    // 비로그인 상태 테스트를 위한 별도 설정이 필요할 수 있음 (Resolver에서 null 반환)
    // 여기서는 간단히 로직 검증에 집중합니다.
}

/**
 * @AuthenticationPrincipal 어노테이션이 붙은 파라미터에
 * 가짜 UserDto를 주입해주는 Mock Resolver
 */
class MockAuthenticationPrincipalArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(UserDto.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        // 테스트 시 로그인한 유저라고 가정하고 ID 5번 UserDto 반환
        return UserDto.builder()
                .id(5L)
                .email("test@test.com")
                .userName("Tester")
                .build();
    }
}