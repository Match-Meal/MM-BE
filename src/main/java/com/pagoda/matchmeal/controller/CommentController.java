package com.pagoda.matchmeal.controller;

import com.pagoda.matchmeal.common.response.CommonResponse;
import com.pagoda.matchmeal.common.util.ApiResponseUtil;
import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.model.dto.request.CommentRequestDto;
import com.pagoda.matchmeal.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/community")
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/posts/{postId}/comments")
    public CommonResponse<Long> writeComment(
            @AuthenticationPrincipal UserDto userDto,
            @PathVariable("postId") Long postId,
            @RequestBody CommentRequestDto commentRequestDto
    ) {
        Long userId = (userDto != null) ? userDto.getId() : null;
        return ApiResponseUtil.created(commentService.writeComment(userId, postId, commentRequestDto));
    }

    @PutMapping("/comments/{commentId}")
    public CommonResponse<Long> updateComment(
            @AuthenticationPrincipal UserDto userDto,
            @PathVariable("commentId") Long commentId,
            @RequestBody CommentRequestDto commentRequestDto
    ) {
        Long userId = (userDto != null) ? userDto.getId() : null;
        return ApiResponseUtil.success(commentService.updateComment(userId, commentId, commentRequestDto));
    }

    @DeleteMapping("/comments/{commentId}")
    public CommonResponse<Void> deleteComment(
            @AuthenticationPrincipal UserDto userDto,
            @PathVariable("commentId") Long commentId
    ) {
        Long userId = (userDto != null) ? userDto.getId() : null;
        commentService.deleteComment(userId, commentId);
        return ApiResponseUtil.success();
    }

    @PostMapping("/comments/{commentId}/like")
    public CommonResponse<Boolean> likeComment(
            @AuthenticationPrincipal UserDto userDto,
            @PathVariable("commentId") Long commentId
    ) {
        Long userId = (userDto != null) ? userDto.getId() : null;
        return ApiResponseUtil.success(commentService.toggleComment(userId, commentId));
    }
}
