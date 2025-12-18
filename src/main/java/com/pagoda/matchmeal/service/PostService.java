package com.pagoda.matchmeal.service;

import com.pagoda.matchmeal.common.response.PageInfoResponseDto;
import com.pagoda.matchmeal.model.dto.request.PostRequestDto;
import com.pagoda.matchmeal.model.dto.response.PostDetailResponseDto;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface PostService {

    Long writePost(Long userId, PostRequestDto postRequestDto, List<MultipartFile> files);

    PageInfoResponseDto<PostDetailResponseDto> getPost(Long userId, String keyword, String searchType, String category, LocalDate startDate, LocalDate endDate, String sortType, Pageable pageable);

    PostDetailResponseDto getPostDetail(Long userId, Long postId);

    Long updatePost(Long userId, Long postId, PostRequestDto postRequestDto, List<MultipartFile> files);

    void deletePost(Long userId, Long postId);

    void increaseViewCount(Long postId);

    boolean toggleLike(Long userId, Long postId);
}
