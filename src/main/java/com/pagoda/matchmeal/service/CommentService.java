package com.pagoda.matchmeal.service;

import com.pagoda.matchmeal.model.dto.request.CommentRequestDto;
import com.pagoda.matchmeal.model.dto.response.CommentResponseDto;

import java.util.List;

public interface CommentService {

    Long writeComment(Long userId, Long postId, CommentRequestDto commentRequestDto);

    List<CommentResponseDto> getComments(Long userId, Long postId);

    Long updateComment(Long userId, Long postId, CommentRequestDto commentRequestDto);

    void deleteComment(Long userId, Long commentId);

    boolean toggleComment(Long userId, Long commentId);
}
