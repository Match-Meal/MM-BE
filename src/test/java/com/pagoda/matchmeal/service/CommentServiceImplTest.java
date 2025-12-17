package com.pagoda.matchmeal.service;

import com.pagoda.matchmeal.common.exception.CustomException;
import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import com.pagoda.matchmeal.mapper.CommentMapper;
import com.pagoda.matchmeal.mapper.PostMapper;
import com.pagoda.matchmeal.model.dto.request.CommentRequestDto;
import com.pagoda.matchmeal.model.dto.response.CommentResponseDto;
import com.pagoda.matchmeal.model.dto.response.PostDetailResponseDto;
import com.pagoda.matchmeal.model.entity.Comment;
import com.pagoda.matchmeal.service.impl.CommentServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @InjectMocks
    private CommentServiceImpl commentService;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private PostMapper postMapper;

    private final Long USER_ID = 1L;
    private final Long POST_ID = 100L;

    @Test
    @DisplayName("댓글 작성 - 일반 댓글")
    void writeComment_Root() {
        // given
        CommentRequestDto dto = new CommentRequestDto();
        dto.setContent("댓글");

        // 게시글 존재 확인 Mock
        given(postMapper.getPostByPostId(POST_ID)).willReturn(new PostDetailResponseDto());

        // ID 생성 흉내
        doAnswer(invocation -> {
            Comment c = invocation.getArgument(0);
            ReflectionTestUtils.setField(c, "commentId", 10L);
            return null;
        }).when(commentMapper).save(any(Comment.class));

        // when
        Long commentId = commentService.writeComment(USER_ID, POST_ID, dto);

        // then
        assertThat(commentId).isEqualTo(10L);
        verify(commentMapper).save(any(Comment.class));
    }

    @Test
    @DisplayName("댓글 작성 - 대댓글 (깊이 제한 로직 확인)")
    void writeComment_Reply_DepthLimit() {
        // given
        // 1. 할아버지 댓글 (ID: 10)
        CommentResponseDto grandParent = new CommentResponseDto();
        ReflectionTestUtils.setField(grandParent, "commentId", 10L);

        // 2. 부모 댓글 (ID: 20, parentId: 10) -> 이미 대댓글인 상태
        CommentResponseDto parent = new CommentResponseDto();
        ReflectionTestUtils.setField(parent, "commentId", 20L);
        ReflectionTestUtils.setField(parent, "parentCommentId", 10L);

        // 3. 요청 DTO (부모(20)에게 답글을 달려고 함)
        CommentRequestDto dto = new CommentRequestDto();
        dto.setContent("손자댓글 시도");
        dto.setParentCommentId(20L);

        given(postMapper.getPostByPostId(POST_ID)).willReturn(new PostDetailResponseDto());
        given(commentMapper.findByCommentId(20L)).willReturn(parent); // 부모 조회

        // when
        commentService.writeComment(USER_ID, POST_ID, dto);

        // then
        // ★ 핵심: 저장되는 Comment의 parentId가 20(부모)이 아니라 10(할아버지)으로 바뀌었는지 검증
        verify(commentMapper).save(argThat(comment ->
                comment.getParentCommentId().equals(10L)
        ));
    }

    @Test
    @DisplayName("댓글 목록 조회 - 계층형 구조 조립 확인")
    void getComments_TreeStructure() {
        // given
        given(postMapper.getPostByPostId(POST_ID)).willReturn(new PostDetailResponseDto());

        // DB에서 가져온 Flat 리스트 (순서: 부모 -> 자식)
        CommentResponseDto parent = CommentResponseDto.builder().commentId(1L).content("부모").build();
        CommentResponseDto child = CommentResponseDto.builder().commentId(2L).content("자식").parentCommentId(1L).build();

        List<CommentResponseDto> flatList = new ArrayList<>();
        flatList.add(parent);
        flatList.add(child);

        given(commentMapper.findAllByPostId(USER_ID, POST_ID)).willReturn(flatList);

        // when
        List<CommentResponseDto> result = commentService.getComments(USER_ID, POST_ID);

        // then
        // 1. 최상위 결과는 1개여야 함 (부모만)
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("부모");

        // 2. 부모 안에 자식이 들어있어야 함
        assertThat(result.get(0).getChildren()).hasSize(1);
        assertThat(result.get(0).getChildren().get(0).getContent()).isEqualTo("자식");
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 작성자 불일치")
    void deleteComment_Fail_Unauthorized() {
        // given
        CommentResponseDto otherComment = new CommentResponseDto();
        // 작성자가 다른 사람 (ID: 999)
        com.pagoda.matchmeal.model.dto.UserSimpleDto otherUser = new com.pagoda.matchmeal.model.dto.UserSimpleDto();
        ReflectionTestUtils.setField(otherUser, "userId", 999L);
        otherComment.setUser(otherUser);

        given(commentMapper.findByCommentId(1L)).willReturn(otherComment);

        // when & then
        assertThatThrownBy(() -> commentService.deleteComment(USER_ID, 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorResponseCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("댓글 좋아요 토글")
    void toggleComment_Success() {
        // given
        given(commentMapper.findByCommentId(1L)).willReturn(new CommentResponseDto());
        given(commentMapper.existsLike(USER_ID, 1L)).willReturn(false); // 안 누른 상태

        // when
        boolean result = commentService.toggleComment(USER_ID, 1L);

        // then
        assertThat(result).isTrue();
        verify(commentMapper).insertLike(USER_ID, 1L);
    }
}