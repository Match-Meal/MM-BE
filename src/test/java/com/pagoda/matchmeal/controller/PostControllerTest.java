package com.pagoda.matchmeal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pagoda.matchmeal.common.response.PageInfoResponseDto;
import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.model.dto.request.PostRequestDto;
import com.pagoda.matchmeal.model.dto.response.PostDetailResponseDto;
import com.pagoda.matchmeal.service.PostService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class) // 순수 Mockito 환경
class PostControllerTest {

    @InjectMocks
    private PostController postController; // MockService가 주입된 컨트롤러 생성

    @Mock
    private PostService postService; // 가짜 서비스

    private MockMvc mockMvc;

    private ObjectMapper objectMapper; // 수동으로 생성해야 함

    // 테스트용 상수
    private final Long USER_ID = 1L;
    private final Long POST_ID = 100L;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(postController)
                .setCustomArgumentResolvers(
                        new MockUserArgumentResolver(),        // 기존: @AuthenticationPrincipal 처리용
                        new PageableHandlerMethodArgumentResolver() // ★ [추가] Pageable 처리용 리졸버
                )
                .build();
    }

    @Test
    @DisplayName("게시글 작성 성공 - 파일 포함")
    void writePost_Success() throws Exception {
        // given
        PostRequestDto requestDto = new PostRequestDto();
        requestDto.setTitle("테스트 제목");
        requestDto.setContent("테스트 내용");
        requestDto.setCategory("DIET");

        String jsonDto = objectMapper.writeValueAsString(requestDto);
        MockMultipartFile dataPart = new MockMultipartFile(
                "data", "data", "application/json", jsonDto.getBytes(StandardCharsets.UTF_8));

        MockMultipartFile filePart = new MockMultipartFile(
                "file", "image.jpg", "image/jpeg", "image-content".getBytes());

        given(postService.writePost(anyLong(), any(PostRequestDto.class), anyList()))
                .willReturn(POST_ID);

        // when & then
        mockMvc.perform(multipart("/community/posts")
                        .file(dataPart)
                        .file(filePart)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .characterEncoding("UTF-8"))
                .andDo(print())
                .andExpect(status().isOk()) // ★ 201(isCreated) -> 200(isOk)로 변경
                .andExpect(jsonPath("$.status").value(201)) // ★ JSON Body의 status 필드가 201인지 검증
                .andExpect(jsonPath("$.data").value(POST_ID));
    }

    @Test
    @DisplayName("게시글 목록 조회 성공")
    void getAllPosts_Success() throws Exception {
        // given
        PageInfoResponseDto<PostDetailResponseDto> mockResponse =
                PageInfoResponseDto.of(PageRequest.of(0, 20), Collections.emptyList(), 0);

        given(postService.getPost(anyLong(), anyString(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/community/posts")
                        .param("keyword", "식단")
                        .param("category", "DIET")
                        .param("page", "0")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))      // HTTP 상태 코드 검증
                .andExpect(jsonPath("$.message").value("성공"));
    }

    @Test
    @DisplayName("게시글 상세 조회 성공")
    void getPost_Success() throws Exception {
        // given
        PostDetailResponseDto detailDto = new PostDetailResponseDto();
        given(postService.getPostDetail(anyLong(), eq(POST_ID))).willReturn(detailDto);

        // when & then
        mockMvc.perform(get("/community/posts/{postId}", POST_ID))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("게시글 수정 성공")
    void updatePost_Success() throws Exception {
        // given
        PostRequestDto requestDto = new PostRequestDto();
        requestDto.setTitle("수정 제목");

        String jsonDto = objectMapper.writeValueAsString(requestDto);
        MockMultipartFile dataPart = new MockMultipartFile(
                "data", "data", "application/json", jsonDto.getBytes(StandardCharsets.UTF_8));

        given(postService.updatePost(anyLong(), eq(POST_ID), any(PostRequestDto.class), any()))
                .willReturn(POST_ID);

        // when & then
        mockMvc.perform(multipart("/community/posts/{postId}", POST_ID)
                        .file(dataPart)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(POST_ID));
    }

    @Test
    @DisplayName("게시글 삭제 성공")
    void deletePost_Success() throws Exception {
        // given
        // void 메서드라 given 생략 가능

        // when & then
        mockMvc.perform(delete("/community/posts/{postId}", POST_ID))
                .andDo(print())
                .andExpect(status().isOk());

        verify(postService).deletePost(anyLong(), eq(POST_ID));
    }

    // =========================================================================
    // MockUserArgumentResolver (UserDto 주입용)
    // =========================================================================
    static class MockUserArgumentResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.getParameterType().equals(UserDto.class) &&
                    parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            UserDto mockUser = new UserDto();
            try {
                java.lang.reflect.Field idField = UserDto.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(mockUser, 1L);
            } catch (Exception e) {
                // Ignore
            }
            return mockUser;
        }
    }

    @Test
    @DisplayName("게시글 상세 조회 - 첫 방문 (조회수 증가 및 쿠키 생성 확인)")
    void getPost_FirstVisit_IncreaseViewCount() throws Exception {
        // given
        PostDetailResponseDto detailDto = new PostDetailResponseDto();
        given(postService.getPostDetail(anyLong(), eq(POST_ID))).willReturn(detailDto);

        // when & then
        mockMvc.perform(get("/community/posts/{postId}", POST_ID)) // 쿠키 없이 요청
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(cookie().exists("postView")) // ★ 쿠키가 생성되었는지 확인
                .andExpect(cookie().value("postView", "[" + POST_ID + "]")); // 쿠키 값 확인

        // ★ Service의 조회수 증가 메서드가 호출되었는지 검증
        verify(postService).increaseViewCount(POST_ID);
    }

    @Test
    @DisplayName("게시글 상세 조회 - 재방문 (조회수 증가 X)")
    void getPost_Revisit_NoIncreaseViewCount() throws Exception {
        // given
        PostDetailResponseDto detailDto = new PostDetailResponseDto();
        given(postService.getPostDetail(anyLong(), eq(POST_ID))).willReturn(detailDto);

        // 이미 방문한 쿠키 생성
        Cookie visitCookie = new Cookie("postView", "[" + POST_ID + "]");

        // when & then
        mockMvc.perform(get("/community/posts/{postId}", POST_ID)
                        .cookie(visitCookie)) // ★ 쿠키를 달고 요청
                .andDo(print())
                .andExpect(status().isOk());

        // ★ Service의 조회수 증가 메서드가 호출되지 않았는지(0번) 검증
        verify(postService, times(0)).increaseViewCount(POST_ID);
    }

    @Test
    @DisplayName("좋아요 토글 성공")
    void toggleLike_Success() throws Exception {
        // given
        // true: 좋아요 등록, false: 좋아요 취소 (상황에 따라 가정)
        given(postService.toggleLike(anyLong(), eq(POST_ID))).willReturn(true);

        // when & then
        mockMvc.perform(post("/community/posts/{postId}/like", POST_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(true)); // true 반환 확인

        verify(postService).toggleLike(anyLong(), eq(POST_ID));
    }
}