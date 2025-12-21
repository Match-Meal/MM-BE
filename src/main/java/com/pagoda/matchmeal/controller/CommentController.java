package com.pagoda.matchmeal.controller;

import com.pagoda.matchmeal.common.response.CommonResponse;
import com.pagoda.matchmeal.common.util.ApiResponseUtil;
import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.model.dto.request.CommentRequestDto;
import com.pagoda.matchmeal.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 커뮤니티 댓글 관리 컨트롤러
 * - 댓글 작성, 수정, 삭제, 좋아요 API 제공
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/community")
public class CommentController {

    private final CommentService commentService;

    /**
     * 댓글 작성
     */
    @PostMapping("/posts/{postId}/comments")
    public CommonResponse<Long> writeComment(
            @AuthenticationPrincipal UserDto userDto,
            @PathVariable("postId") Long postId,
            @RequestBody CommentRequestDto commentRequestDto
    ) {
        Long userId = (userDto != null) ? userDto.getId() : null;
        return ApiResponseUtil.created(commentService.writeComment(userId, postId, commentRequestDto));
    }

    /**
     * 댓글 수정
     */
    @PutMapping("/comments/{commentId}")
    public CommonResponse<Long> updateComment(
            @AuthenticationPrincipal UserDto userDto,
            @PathVariable("commentId") Long commentId,
            @RequestBody CommentRequestDto commentRequestDto
    ) {
        Long userId = (userDto != null) ? userDto.getId() : null;
        return ApiResponseUtil.success(commentService.updateComment(userId, commentId, commentRequestDto));
    }

    /**
     * 댓글 삭제
     */
    @DeleteMapping("/comments/{commentId}")
    public CommonResponse<Void> deleteComment(
            @AuthenticationPrincipal UserDto userDto,
            @PathVariable("commentId") Long commentId
    ) {
        Long userId = (userDto != null) ? userDto.getId() : null;
        commentService.deleteComment(userId, commentId);
        return ApiResponseUtil.success();
    }

    /**
     * 댓글 좋아요 토글
     */
    @PostMapping("/comments/{commentId}/like")
    public CommonResponse<Boolean> likeComment(
            @AuthenticationPrincipal UserDto userDto,
            @PathVariable("commentId") Long commentId
    ) {
        Long userId = (userDto != null) ? userDto.getId() : null;
        return ApiResponseUtil.success(commentService.toggleComment(userId, commentId));
    }
}