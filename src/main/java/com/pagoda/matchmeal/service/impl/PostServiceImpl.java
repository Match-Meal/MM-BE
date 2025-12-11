package com.pagoda.matchmeal.service.impl;

import com.pagoda.matchmeal.common.exception.CustomException;
import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import com.pagoda.matchmeal.common.response.PageInfoResponseDto;
import com.pagoda.matchmeal.mapper.PostMapper;
import com.pagoda.matchmeal.model.dto.PostSearchCond;
import com.pagoda.matchmeal.model.dto.request.PostRequestDto;
import com.pagoda.matchmeal.model.dto.response.PostDetailResponseDto;
import com.pagoda.matchmeal.model.entity.Post;
import com.pagoda.matchmeal.model.entity.PostFile;
import com.pagoda.matchmeal.service.PostService;
import com.pagoda.matchmeal.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final S3Service s3Service;

    @Override
    @Transactional
    public Long writePost(Long userId, PostRequestDto postRequestDto, List<MultipartFile> files) {
        validateUser(userId);

        Post post = Post.builder()
                .userId(userId)
                .category(postRequestDto.getCategory())
                .title(postRequestDto.getTitle())
                .content(postRequestDto.getContent())
                .build();

        postMapper.savePost(post);

        if (files != null && !files.isEmpty()) {
            savePostFiles(post.getPostId(), files);
        }

        return post.getPostId();
    }

    private static void validateUser(Long userId) {
        if (userId == null) {
            throw new CustomException(ErrorResponseCode.UNAUTHORIZED);
        }
    }

    @Override
    public PageInfoResponseDto<PostDetailResponseDto> getPost(Long userId, String keyword, String searchType, String category, LocalDate startDate, LocalDate endDate, String sortType, Pageable pageable) {

        validateUser(userId);

        PostSearchCond cond = PostSearchCond.builder()
                .userId(userId)
                .category(category)
                .searchType(searchType)
                .keyword(keyword)
                .startDate(startDate)
                .endDate(endDate)
                .sortType(sortType)
                .limit(pageable.getPageSize())
                .offset((int) pageable.getOffset())
                .build();

        List<PostDetailResponseDto> posts = postMapper.getPosts(cond);

        int totalPosts = postMapper.countPosts(cond);

        return PageInfoResponseDto.of(pageable, posts, totalPosts);
    }

    @Override
    public PostDetailResponseDto getPostDetail(Long userId, Long postId) {
        validateUser(userId);
        PostDetailResponseDto postDetail = postMapper.getPostByPostId(postId);
        if (postDetail == null) {
            throw new CustomException(ErrorResponseCode.POST_NOT_FOUND);
        }
        return postDetail;
    }

    @Override
    @Transactional
    public Long updatePost(Long userId, Long postId, PostRequestDto postRequestDto, List<MultipartFile> files) {

        validateUser(userId);

        validatePostOwner(userId, postId);

        Post updateParam = Post.builder()
                .postId(postId)
                .userId(userId)
                .category(postRequestDto.getCategory())
                .title(postRequestDto.getTitle())
                .content(postRequestDto.getContent())
                .build();

        int updateCount = postMapper.updatePost(updateParam);
        if (updateCount == 0) {
            throw new CustomException(ErrorResponseCode.POST_NOT_FOUND);
        }

        if (files != null && !files.isEmpty()) {
            // 2-1. 기존 파일들 조회
            List<PostFile> oldFiles = postMapper.getPostFilesByPostId(postId);

            // 2-2. 기존 파일 S3 삭제 (물리적 삭제)
            deleteS3Files(oldFiles);

            // 2-3. 기존 파일 DB 정보 삭제
            postMapper.deletePostFilesByPostId(postId);

            // 2-4. 새 파일 업로드 및 DB 저장
            savePostFiles(postId, files);
        }

        return postId;
    }

    private void validatePostOwner(Long userId, Long postId) {
        // 1. 게시글 상세 조회
        PostDetailResponseDto postDetail = postMapper.getPostByPostId(postId);

        // 2. 게시글 존재 여부 확인 (방어 로직)
        if (postDetail == null) {
            throw new CustomException(ErrorResponseCode.POST_NOT_FOUND);
        }

        // 3. 작성자 본인 확인 (권한 체크)
        if (!postDetail.getUser().getUserId().equals(userId)) {
            throw new CustomException(ErrorResponseCode.UNAUTHORIZED);
        }
    }

    @Override
    @Transactional
    public void deletePost(Long userId, Long postId) {

        validateUser(userId);

        // 1. 게시글 상세 조회
        validatePostOwner(userId, postId);

        // 1. 삭제 전 기존 파일 조회
        List<PostFile> attachedFiles = postMapper.getPostFilesByPostId(postId);

        // 2. 게시글 Soft Delete (DB) - Mapper 파라미터 순서 주의(@Param 확인 완료)
        postMapper.deletePost(userId, postId);

        // 3. 파일 S3 삭제 (게시글이 성공적으로 안 보이면 파일도 날림)
        // 트랜잭션이 롤백되면 이 로직은 실행되지 않거나, 실행되더라도 DB는 원복됨
        // (주의: S3 삭제는 롤백이 안 되므로, 보통 가장 마지막에 수행하거나 이벤트로 분리하기도 함. 여기선 직관적으로 구현)
        deleteS3Files(attachedFiles);

        // 4. (선택) 파일 메타데이터도 DB에서 지워줄 것인가?
        // 게시글이 soft delete라 살아있다면 파일 정보도 남겨둘 수 있지만,
        // "S3에서 지웠다"는 건 복구 불가를 의미하므로 DB에서도 지우는 게 깔끔함.
        if (attachedFiles != null && !attachedFiles.isEmpty()) {
            postMapper.deletePostFilesByPostId(postId);
        }
    }

    private void savePostFiles(Long postId, List<MultipartFile> files) {
        // 업로드된 파일 정보를 담을 리스트 생성
        List<PostFile> postFileList = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue; // 빈 파일은 스킵

            // S3 업로드
            String fileUrl = s3Service.uploadFile(file, "post");

            // 파일 타입 결정
            String contentType = file.getContentType();
            String fileType = (contentType != null && contentType.startsWith("video")) ? "VIDEO" : "IMAGE";

            // 엔티티 생성 후 리스트에 추가
            PostFile postFile = PostFile.builder()
                    .postId(postId)
                    .fileUrl(fileUrl)
                    .fileType(fileType)
                    .build();

            postFileList.add(postFile);
        }

        // DB에 일괄 저장 (Mapper의 <foreach> 기능 활용)
        if (!postFileList.isEmpty()) {
            postMapper.savePostFiles(postFileList);
        }
    }

    // S3 파일 삭제 공통 로직
    private void deleteS3Files(List<PostFile> files) {
        if (files == null || files.isEmpty()) return;

        for (PostFile file : files) {
            s3Service.deleteFile(file.getFileUrl());
        }
    }
}
