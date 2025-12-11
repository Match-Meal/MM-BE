package com.pagoda.matchmeal.controller;

import com.pagoda.matchmeal.common.response.CommonResponse;
import com.pagoda.matchmeal.common.response.PageInfoResponseDto;
import com.pagoda.matchmeal.common.util.ApiResponseUtil;
import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.model.dto.request.PostRequestDto;
import com.pagoda.matchmeal.model.dto.response.PostDetailResponseDto;
import com.pagoda.matchmeal.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/community")
public class PostController {

    private final PostService postService;

    @PostMapping("/posts")
    public CommonResponse<Long> writePost(
            @AuthenticationPrincipal UserDto userDto,
            @RequestPart("data") PostRequestDto postRequestDto,
            @RequestPart(value = "file", required = false) List<MultipartFile> files
    ) {
        Long userId = (userDto != null) ? userDto.getId() : null;
        return ApiResponseUtil.created(postService.writePost(userId, postRequestDto, files));
    }

    @GetMapping("/posts")
    public CommonResponse<PageInfoResponseDto<PostDetailResponseDto>> getAllPosts(
            @AuthenticationPrincipal UserDto userDto,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "searchType", required = false) String searchType,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "startDate", required = false) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) LocalDate endDate,
            @RequestParam(value = "sortType", required = false) String sortType,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        Long userId = (userDto != null) ? userDto.getId() : null;
        return ApiResponseUtil.success(postService.getPost(userId, keyword, searchType, category, startDate, endDate, sortType, pageable));
    }

    @GetMapping("/posts/{postId}")
    public CommonResponse<PostDetailResponseDto> getPost(
            @AuthenticationPrincipal UserDto userDto,
            @PathVariable("postId") Long postId
    ) {
        Long userId = (userDto != null) ? userDto.getId() : null;
        return ApiResponseUtil.success(postService.getPostDetail(userId, postId));
    }

    @PutMapping("/posts/{postId}")
    public CommonResponse<Long> updatePost(
            @AuthenticationPrincipal UserDto userDto,
            @PathVariable("postId") Long postId,
            @RequestPart("data") PostRequestDto postRequestDto,
            @RequestPart(value = "file", required = false) List<MultipartFile> files
    ) {
        Long userId = (userDto != null) ? userDto.getId() : null;
        return ApiResponseUtil.success(postService.updatePost(userId, postId, postRequestDto, files));
    }

    @DeleteMapping("/posts/{postId}")
    public CommonResponse<Void> deletePost(
            @AuthenticationPrincipal UserDto userDto,
            @PathVariable("postId") Long postId
    ) {
        Long userId = (userDto != null) ? userDto.getId() : null;
        postService.deletePost(userId, postId);
        return ApiResponseUtil.success();
    }
}
