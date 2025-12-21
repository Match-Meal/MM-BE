package com.pagoda.matchmeal.mapper;

import com.pagoda.matchmeal.model.dto.response.CommentResponseDto;
import com.pagoda.matchmeal.model.entity.Comment;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 댓글(Comment) 관리 매퍼
 * - 댓글 작성, 조회, 수정, 삭제 및 좋아요 기능
 */
public interface CommentMapper {

    /**
     * 댓글 저장
     */
    void save(Comment comment);

    /**
     * 특정 게시글의 모든 댓글 조회
     *
     * @param userId 조회하는 유저 ID (댓글 좋아요 여부 확인용)
     */
    List<CommentResponseDto> findAllByPostId(@Param("userId") Long userId, @Param("postId") Long postId);

    /**
     * 댓글 단건 조회
     */
    CommentResponseDto findByCommentId(@Param("commentId") Long commentId);

    /**
     * 댓글 내용 수정
     */
    void update(Comment comment);

    /**
     * 댓글 삭제
     */
    void delete(@Param("commentId") Long commentId);

    /**
     * 게시글에 달린 댓글 수 카운트
     */
    int countByPostId(@Param("postId") Long postId);


    // --- 댓글 좋아요 기능 ---

    /**
     * 댓글 좋아요 여부 확인
     */
    boolean existsLike(@Param("userId") Long userId, @Param("commentId") Long commentId);

    /**
     * 댓글 좋아요 추가
     */
    void insertLike(@Param("userId") Long userId, @Param("commentId") Long commentId);

    /**
     * 댓글 좋아요 취소
     */
    void deleteLike(@Param("userId") Long userId, @Param("commentId") Long commentId);
}