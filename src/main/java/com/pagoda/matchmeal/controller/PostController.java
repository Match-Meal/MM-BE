package com.pagoda.matchmeal.controller;

import com.pagoda.matchmeal.common.response.CommonResponse;
import com.pagoda.matchmeal.common.response.PageInfoResponseDto;
import com.pagoda.matchmeal.common.util.ApiResponseUtil;
import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.model.dto.request.PostRequestDto;
import com.pagoda.matchmeal.model.dto.response.PostDetailResponseDto;
import com.pagoda.matchmeal.service.PostService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

/**
 * 커뮤니티 게시글 컨트롤러
 * - 게시글 CRUD, 검색, 좋아요, 조회수 증가(쿠키 기반)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/community")
public class PostController {

    private final PostService postService;

    /**
     * 게시글 작성
     */
    @PostMapping("/posts")
    public CommonResponse<Long> writePost(
            @AuthenticationPrincipal UserDto userDto,
            @RequestPart("data") PostRequestDto postRequestDto,
            @RequestPart(value = "file", required = false) List<MultipartFile> files
    ) {
        Long userId = (userDto != null) ? userDto.getId() : null;
        return ApiResponseUtil.created(postService.writePost(userId, postRequestDto, files));
    }

    /**
     * 게시글 목록 조회 (검색 및 필터링)
     */
    @GetMapping("/posts")
    public CommonResponse<PageInfoResponseDto<PostDetailResponseDto>> getAllPosts(
            @AuthenticationPrincipal UserDto userDto,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "searchType", required = false) String searchType,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "startDate", required = false) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) LocalDate endDate,
            @RequestParam(value = "sortType", required = false) String sortType,
            @PageableDefault(page = 0, size = 15) Pageable pageable
    ) {
        Long userId = (userDto != null) ? userDto.getId() : null;
        return ApiResponseUtil.success(postService.getPost(userId, keyword, searchType, category, startDate, endDate, sortType, pageable));
    }

    /**
     * 게시글 상세 조회 (조회수 증가 로직 포함)
     */
    @GetMapping("/posts/{postId}")
    public CommonResponse<PostDetailResponseDto> getPost(
            @AuthenticationPrincipal UserDto userDto,
            @PathVariable("postId") Long postId,
            HttpServletRequest request, // 쿠키 조회용
            HttpServletResponse response // 쿠키 저장용
    ) {
        Long userId = (userDto != null) ? userDto.getId() : null;

        // 조회수 증가 (중복 방지 처리)
        viewCountUp(postId, request, response);

        return ApiResponseUtil.success(postService.getPostDetail(userId, postId));
    }

    /**
     * 게시글 수정
     */
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

    /**
     * 게시글 삭제
     */
    @DeleteMapping("/posts/{postId}")
    public CommonResponse<Void> deletePost(
            @AuthenticationPrincipal UserDto userDto,
            @PathVariable("postId") Long postId
    ) {
        Long userId = (userDto != null) ? userDto.getId() : null;
        postService.deletePost(userId, postId);
        return ApiResponseUtil.success();
    }

    /**
     * 게시글 좋아요 토글
     */
    @PostMapping("/posts/{postId}/like")
    public CommonResponse<Boolean> toggleLike(
            @AuthenticationPrincipal UserDto userDto,
            @PathVariable("postId") Long postId
    ) {
        Long userId = (userDto != null) ? userDto.getId() : null;

        return ApiResponseUtil.success(postService.toggleLike(userId, postId));
    }

    /**
     * 조회수 증가 로직 (쿠키 기반 중복 방지)
     * - 클라이언트 쿠키에 "postView=[1][2]..." 형태로 읽은 게시글 ID를 저장
     * - 해당 ID가 쿠키에 없을 때만 DB 조회수를 1 증가시킴 (24시간 유효)
     */
    private void viewCountUp(Long postId, HttpServletRequest request, HttpServletResponse response) {
        Cookie oldCookie = null;

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("postView")) {
                    oldCookie = cookie;
                }
            }
        }

        if (oldCookie != null) {
            // 쿠키가 있는데 해당 게시 ID가 없다면 (처음봄)
            if (!oldCookie.getValue().contains("[" + postId.toString() + "]")) {
                postService.increaseViewCount(postId); //DB 업뎃
                oldCookie.setValue(oldCookie.getValue() + "_[" + postId + "]");
                oldCookie.setPath("/");
                oldCookie.setMaxAge(60 * 60 * 24); // 24시간
                response.addCookie(oldCookie);
            }
        } else {
            // 쿠키 자체가 없으면 (완전 처음)
            postService.increaseViewCount(postId);
            Cookie newCookie = new Cookie("postView", "[" + postId + "]");
            newCookie.setPath("/");
            newCookie.setMaxAge(60 * 60 * 24);
            response.addCookie(newCookie);
        }
    }
}