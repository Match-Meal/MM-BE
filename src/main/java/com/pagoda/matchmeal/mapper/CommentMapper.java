package com.pagoda.matchmeal.mapper;

import com.pagoda.matchmeal.model.dto.response.CommentResponseDto;
import com.pagoda.matchmeal.model.entity.Comment;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CommentMapper {

    void save(Comment comment);

    List<CommentResponseDto> findAllByPostId(@Param("userId") Long userId, @Param("postId") Long postId);

    CommentResponseDto findByCommentId(@Param("commentId") Long commentId);

    void update(Comment comment);

    void delete(@Param("commentId") Long commentId);

    int countByPostId(@Param("postId") Long postId);

    // 좋아요

    boolean existsLike(@Param("userId") Long userId, @Param("commentId") Long commentId);

    void insertLike(@Param("userId") Long userId, @Param("commentId") Long commentId);

    void deleteLike(@Param("userId") Long userId, @Param("commentId") Long commentId);
}
