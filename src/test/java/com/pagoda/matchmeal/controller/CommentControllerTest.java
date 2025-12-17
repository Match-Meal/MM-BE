package com.pagoda.matchmeal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.model.dto.request.CommentRequestDto;
import com.pagoda.matchmeal.service.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

    @InjectMocks
    private CommentController commentController;

    @Mock
    private CommentService commentService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    private final Long USER_ID = 1L;
    private final Long POST_ID = 100L;
    private final Long COMMENT_ID = 500L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(commentController)
                .setCustomArgumentResolvers(new MockUserArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("댓글 작성 성공")
    void writeComment_Success() throws Exception {
        // given
        CommentRequestDto requestDto = new CommentRequestDto();
        requestDto.setContent("테스트 댓글");

        given(commentService.writeComment(eq(USER_ID), eq(POST_ID), any(CommentRequestDto.class)))
                .willReturn(COMMENT_ID);

        // when & then
        mockMvc.perform(post("/community/posts/{postId}/comments", POST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isOk()) // 201 Created
                .andExpect(jsonPath("$.status").value(201)) // ★ JSON Body의 status 필드가 201인지 검증
                .andExpect(jsonPath("$.data").value(COMMENT_ID));
    }

    @Test
    @DisplayName("댓글 수정 성공")
    void updateComment_Success() throws Exception {
        // given
        CommentRequestDto requestDto = new CommentRequestDto();
        requestDto.setContent("수정 댓글");

        given(commentService.updateComment(eq(USER_ID), eq(COMMENT_ID), any(CommentRequestDto.class)))
                .willReturn(COMMENT_ID);

        // when & then
        mockMvc.perform(put("/community/comments/{commentId}", COMMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("댓글 삭제 성공")
    void deleteComment_Success() throws Exception {
        // when & then
        mockMvc.perform(delete("/community/comments/{commentId}", COMMENT_ID))
                .andDo(print())
                .andExpect(status().isOk());

        verify(commentService).deleteComment(USER_ID, COMMENT_ID);
    }

    @Test
    @DisplayName("댓글 좋아요 토글 성공")
    void toggleLike_Success() throws Exception {
        // given
        given(commentService.toggleComment(USER_ID, COMMENT_ID)).willReturn(true);

        // when & then
        mockMvc.perform(post("/community/comments/{commentId}/like", COMMENT_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    // 가짜 유저 주입용 리졸버 (PostControllerTest와 동일)
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
            }
            return mockUser;
        }
    }
}