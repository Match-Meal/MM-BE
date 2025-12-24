package com.pagoda.matchmeal.service.impl;

import com.pagoda.matchmeal.common.exception.CustomException;
import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import com.pagoda.matchmeal.mapper.CommentMapper;
import com.pagoda.matchmeal.mapper.PostMapper;
import com.pagoda.matchmeal.mapper.UserMapper;
import com.pagoda.matchmeal.model.dto.request.CommentRequestDto;
import com.pagoda.matchmeal.model.dto.response.CommentResponseDto;
import com.pagoda.matchmeal.model.dto.response.PostDetailResponseDto;
import com.pagoda.matchmeal.model.entity.Comment;
import com.pagoda.matchmeal.model.entity.User;
import com.pagoda.matchmeal.model.enums.NotificationType;
import com.pagoda.matchmeal.service.CommentService;
import com.pagoda.matchmeal.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 댓글(Comment) 비즈니스 로직 구현체
 * - 댓글 CRUD 및 계층형(대댓글) 구조 처리
 * - 댓글 좋아요 기능 포함
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    /**
     * 댓글 작성
     * - 부모 댓글 ID가 존재하면 대댓글로 저장하며, 깊이(Depth) 제한 정책에 따라 부모 ID를 조정합니다.
     *
     * @param userId            작성자 PK
     * @param postId            게시글 PK
     * @param commentRequestDto 댓글 내용 및 부모 댓글 ID
     * @return 생성된 댓글 ID
     */
    @Override
    @Transactional
    public Long writeComment(Long userId, Long postId, CommentRequestDto commentRequestDto) {
        validateUser(userId);

        PostDetailResponseDto post = postMapper.getPostByPostId(postId);
        if (post == null) {
            throw new CustomException(ErrorResponseCode.POST_NOT_FOUND);
        }

        Long parentCommentId = commentRequestDto.getParentCommentId();
        Long parentCommentAuthorId = null;

        if (parentCommentId != null) {
            CommentResponseDto parent = commentMapper.findByCommentId(parentCommentId);
            if (parent == null) {
                throw new CustomException(ErrorResponseCode.COMMENT_NOT_FOUND);
            }
            // 대댓글의 대댓글(3뎁스 이상) 방지: 부모의 부모가 있다면 그 ID를 사용 (2뎁스로 평탄화)
            if (parent.getParentCommentId() != null) {
                parentCommentId = parent.getParentCommentId();
            }
            parentCommentAuthorId = parent.getUser().getUserId();
        }

        Comment comment = Comment.builder()
                .postId(postId)
                .userId(userId)
                .parentCommentId(parentCommentId)
                .content(commentRequestDto.getContent())
                .build();

        commentMapper.save(comment);

        // 0. 댓글 작성자 정보 조회 (알림 메시지용)
        User commenter = userMapper.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorResponseCode.USER_NOT_FOUND));

        // 1. 게시글 작성자에게 알림 (내 글에 내가 댓글 단 경우는 제외)
        if (!post.getUser().getUserId().equals(userId)) {
            notificationService.sendToUser(
                    post.getUser().getUserId(), // 수신자: 글 작성자
                    userId,                         // 발신자: 댓글 작성자
                    NotificationType.COMMENT,
                    commenter.getUserName() + "님이 댓글을 남겼습니다.",
                    postId.intValue(),              // relatedId
                    "/community/" + postId          // 이동 경로
            );
        }

        // 2. (대댓글인 경우) 원댓글 작성자에게 알림
        // 조건: 대댓글이어야 함 && 내 댓글에 내가 대댓글 단 경우 제외 && 원댓글 작성자가 글 작성자와 다를 경우(중복 알림 방지)
        if (parentCommentAuthorId != null
                && !parentCommentAuthorId.equals(userId)
                && !parentCommentAuthorId.equals(post.getUser().getUserId())) {

            notificationService.sendToUser(
                    parentCommentAuthorId,          // 수신자: 원댓글 작성자
                    userId,                         // 발신자: 대댓글 작성자
                    NotificationType.COMMENT,
                    commenter.getUserName() + "님이 대댓글을 남겼습니다.",
                    postId.intValue(),
                    "/community/" + postId
            );
        }
        return comment.getCommentId();
    }

    /**
     * 댓글 목록 조회
     * - DB에서 평탄화된 리스트를 조회한 후, 애플리케이션 메모리에서 계층형(부모-자식) 구조로 변환합니다.
     * - 삭제된 댓글(Soft Deleted)의 경우 내용을 마스킹 처리합니다.
     *
     * @param userId 요청자 PK (좋아요 여부 확인용)
     * @param postId 게시글 PK
     * @return 계층 구조가 적용된 댓글 리스트 (Roots)
     */
    @Override
    public List<CommentResponseDto> getComments(Long userId, Long postId) {
        validateUser(userId);
        existingPost(postId);

        List<CommentResponseDto> allByPostId = commentMapper.findAllByPostId(userId, postId);

        List<CommentResponseDto> roots = new ArrayList<>(); // 최상위 부모
        Map<Long, CommentResponseDto> map = new HashMap<>(); // id값으로 객체를 찾기 위한 맵

        for (CommentResponseDto comment : allByPostId) {
            if (comment.isDeleted()) {
                comment.setContent("삭제된 댓글입니다.");
                comment.setUser(null); // 개인정보 보호
            }

            map.put(comment.getCommentId(), comment);
            if (comment.getChildren() == null) {
                comment.setChildren(new ArrayList<>());
            }

            if (comment.getParentCommentId() == null) {
                roots.add(comment); // 최상위 댓글
            } else {
                CommentResponseDto parent = map.get(comment.getParentCommentId());
                if (parent != null) {
                    parent.getChildren().add(comment); // 자식 댓글 추가
                } else {
                    roots.add(comment); // 부모를 못 찾은 경우 안전하게 Root로 처리
                }
            }
        }
        return roots;
    }

    /**
     * 댓글 수정
     *
     * @param userId            요청자 PK (본인 확인)
     * @param commentId         수정할 댓글 PK
     * @param commentRequestDto 수정할 내용
     * @return 수정된 댓글 ID
     */
    @Override
    @Transactional
    public Long updateComment(Long userId, Long commentId, CommentRequestDto commentRequestDto) {
        validateUser(userId);
        validateCommentOwner(userId, commentId);

        Comment newComment = Comment.builder()
                .commentId(commentId)
                .content(commentRequestDto.getContent())
                .build();

        commentMapper.update(newComment);
        return commentId;
    }

    /**
     * 댓글 삭제
     *
     * @param userId    요청자 PK (본인 확인)
     * @param commentId 삭제할 댓글 PK
     */
    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        validateUser(userId);
        validateCommentOwner(userId, commentId);
        commentMapper.delete(commentId);
    }

    /**
     * 댓글 좋아요 토글
     *
     * @param userId    요청자 PK
     * @param commentId 댓글 PK
     * @return true(좋아요 등록됨), false(좋아요 취소됨)
     */
    @Override
    @Transactional
    public boolean toggleComment(Long userId, Long commentId) {
        validateUser(userId);

        if (commentMapper.findByCommentId(commentId) == null) {
            throw new CustomException(ErrorResponseCode.COMMENT_NOT_FOUND);
        }

        boolean isLiked = commentMapper.existsLike(userId, commentId);

        if (isLiked) {
            commentMapper.deleteLike(userId, commentId);
            return false;
        } else {
            commentMapper.insertLike(userId, commentId);
            return true;
        }
    }

    // --- Helper Methods ---

    private static void validateUser(Long userId) {
        if (userId == null) {
            throw new CustomException(ErrorResponseCode.UNAUTHORIZED);
        }
    }

    private void existingPost(Long postId) {
        PostDetailResponseDto post = postMapper.getPostByPostId(postId);
        if (post == null) {
            throw new CustomException(ErrorResponseCode.POST_NOT_FOUND);
        }
    }

    private void validateCommentOwner(Long userId, Long commentId) {
        CommentResponseDto comment = commentMapper.findByCommentId(commentId);
        if (comment == null) {
            throw new CustomException(ErrorResponseCode.COMMENT_NOT_FOUND);
        }
        if (comment.getUser() == null || comment.getUser().getUserId() == null) {
            throw new CustomException(ErrorResponseCode.UNAUTHORIZED);
        }
        if (!comment.getUser().getUserId().equals(userId)) {
            throw new CustomException(ErrorResponseCode.UNAUTHORIZED);
        }
    }
}