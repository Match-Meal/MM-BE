package com.pagoda.matchmeal.service.impl;

import com.pagoda.matchmeal.common.exception.CustomException;
import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import com.pagoda.matchmeal.mapper.CommentMapper;
import com.pagoda.matchmeal.mapper.PostMapper;
import com.pagoda.matchmeal.model.dto.request.CommentRequestDto;
import com.pagoda.matchmeal.model.dto.response.CommentResponseDto;
import com.pagoda.matchmeal.model.dto.response.PostDetailResponseDto;
import com.pagoda.matchmeal.model.entity.Comment;
import com.pagoda.matchmeal.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final PostMapper postMapper;

    @Override
    @Transactional
    public Long writeComment(Long userId, Long postId, CommentRequestDto commentRequestDto) {
        validateUser(userId);

        existingPost(postId);

        Long parentCommentId = commentRequestDto.getParentCommentId();
        if (parentCommentId != null) {
            CommentResponseDto parent = commentMapper.findByCommentId(parentCommentId);
            if (parent == null) {
                throw new CustomException(ErrorResponseCode.COMMENT_NOT_FOUND);
            }
            // 부모의 부모가 있다면?
            if (parent.getParentCommentId() != null) {
                parentCommentId = parent.getParentCommentId();
            }
        }

        Comment comment = Comment.builder()
                .postId(postId)
                .userId(userId)
                .parentCommentId(parentCommentId)
                .content(commentRequestDto.getContent())
                .build();

        commentMapper.save(comment);
        return comment.getCommentId();
    }

    @Override
    public List<CommentResponseDto> getComments(Long userId, Long postId) {
        validateUser(userId);

        existingPost(postId);

        List<CommentResponseDto> allByPostId = commentMapper.findAllByPostId(userId, postId);

        List<CommentResponseDto> roots = new ArrayList<>(); // 최상위 부모
        Map<Long, CommentResponseDto> map = new HashMap<>(); // id값으로 객체를 찾기 위한 맵

        for (CommentResponseDto comment : allByPostId) {
            map.put(comment.getCommentId(), comment);
            if (comment.getChildren() == null) {
                comment.setChildren(new ArrayList<>());
            }
            if (comment.getParentCommentId() == null) {
                // 부모 ID가 없으면 -> 최상위 댓글 (Root)
                roots.add(comment);
            } else {
                // 부모 ID가 있으면 -> 부모를 찾아서 그 자식 리스트에 추가
                CommentResponseDto parent = map.get(comment.getParentCommentId());
                if (parent != null) {
                    parent.getChildren().add(comment);
                } else {
                    // (예외 케이스) 부모가 없는데 자식만 있는 경우 (DB 정합성 문제 등)
                    // 안전하게 Root로 처리하거나 무시
                    roots.add(comment);
                }
            }
        }

        return roots;
    }

    @Override
    @Transactional
    public Long updateComment(Long userId, Long commentId, CommentRequestDto commentRequestDto) {
        validateUser(userId);
        CommentResponseDto existingComment = validateCommentOwner(userId, commentId);

        Comment newComment = Comment.builder()
                .commentId(commentId)
                .content(commentRequestDto.getContent())
                .build();

        commentMapper.update(newComment);

        return commentId;
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        validateUser(userId);
        validateCommentOwner(userId, commentId);
        commentMapper.delete(commentId);
    }

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

    private CommentResponseDto validateCommentOwner(Long userId, Long commentId) {
        CommentResponseDto comment = commentMapper.findByCommentId(commentId);

        if (comment == null) {
            throw new CustomException(ErrorResponseCode.COMMENT_NOT_FOUND);
        }

        // 작성자 ID 비교
        if (!comment.getUser().getUserId().equals(userId)) {
            throw new CustomException(ErrorResponseCode.UNAUTHORIZED);
        }

        return comment;
    }
}
