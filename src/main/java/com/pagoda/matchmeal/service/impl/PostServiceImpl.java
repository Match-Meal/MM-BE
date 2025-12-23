package com.pagoda.matchmeal.service.impl;

import com.pagoda.matchmeal.common.exception.CustomException;
import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import com.pagoda.matchmeal.common.response.PageInfoResponseDto;
import com.pagoda.matchmeal.mapper.FollowMapper;
import com.pagoda.matchmeal.mapper.PostMapper;
import com.pagoda.matchmeal.mapper.UserMapper;
import com.pagoda.matchmeal.model.dto.PostSearchCond;
import com.pagoda.matchmeal.model.dto.request.PostRequestDto;
import com.pagoda.matchmeal.model.dto.response.CommentResponseDto;
import com.pagoda.matchmeal.model.dto.response.PostDetailResponseDto;
import com.pagoda.matchmeal.model.entity.Post;
import com.pagoda.matchmeal.model.entity.PostFile;
import com.pagoda.matchmeal.model.entity.User;
import com.pagoda.matchmeal.model.enums.NotificationType;
import com.pagoda.matchmeal.model.enums.PostCategory;
import com.pagoda.matchmeal.service.CommentService;
import com.pagoda.matchmeal.service.NotificationService;
import com.pagoda.matchmeal.service.PostService;
import com.pagoda.matchmeal.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 게시글 비즈니스 로직 구현체
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final FollowMapper followMapper;
    private final S3Service s3Service;
    private final CommentService commentService;
    private final NotificationService notificationService;

    /**
     * 게시글 작성
     *
     * @param userId         작성자 PK
     * @param postRequestDto 게시글 제목, 내용, 카테고리 정보 DTO
     * @param files          첨부할 이미지/동영상 파일 리스트 (없으면 null)
     * @return 생성된 게시글 ID (postId)
     */
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

        // 공지사항 알림
        if (post.getCategory() == PostCategory.NOTICE) {
            List<Long> allActiveUserIds = userMapper.findAllActiveUserIds();

            for (Long activeUserId : allActiveUserIds) {
                notificationService.sendToUser(
                        activeUserId,
                        userId,
                        NotificationType.NOTICE,
                        "📢 [공지] " + post.getTitle(),
                        post.getPostId().intValue(),
                        "/community/" + post.getPostId()
                );
            }

        }
        // 일반 게시글일 경우: 나를 팔로우한 사람들에게 알림 발송
        else {
            String writerUser = userMapper.findById(userId).orElseThrow(() -> new CustomException(ErrorResponseCode.USER_NOT_FOUND)).getUserName();

            List<Long> followerIds = followMapper.findFollowerIds(userId);

            for (Long followerId : followerIds) {
                notificationService.sendToUser(
                        followerId,
                        userId,
                        NotificationType.FOLLOWING_POST,
                        writerUser + "님이 새 게시물을 올렸습니다: " + post.getTitle(),
                        post.getPostId().intValue(),
                        "/community/" + post.getPostId()
                );
            }
        }


        return post.getPostId();
    }

    /**
     * 게시글 목록 조회 (검색 및 페이징)
     *
     * @param userId     요청자 ID (권한 체크용)
     * @param keyword    검색어 (제목 or 내용)
     * @param searchType 검색 타입 (title, content, all)
     * @param category   카테고리 필터
     * @param startDate  조회 시작 날짜 (YYYY-MM-DD)
     * @param endDate    조회 종료 날짜 (YYYY-MM-DD, 내부적으로 +1일 처리됨)
     * @param sortType   정렬 기준 (latest, views, likes 등)
     * @param pageable   페이징 정보 (page, size)
     * @return 페이징된 게시글 목록 응답 DTO
     */
    @Override
    public PageInfoResponseDto<PostDetailResponseDto> getPost(Long userId, String keyword, String searchType, String category, LocalDate startDate, LocalDate endDate, String sortType, Pageable pageable) {
        validateUser(userId);

        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.atStartOfDay().plusDays(1) : null;

        PostSearchCond cond = PostSearchCond.builder()
                .userId(userId)
                .category(category)
                .searchType(searchType)
                .keyword(keyword)
                .startDate(startDateTime)
                .endDate(endDateTime)
                .sortType(sortType)
                .limit(pageable.getPageSize())
                .offset((int) pageable.getOffset())
                .build();

        List<PostDetailResponseDto> posts = postMapper.getPosts(cond);
        int totalPosts = postMapper.countPosts(cond);

        return PageInfoResponseDto.of(pageable, posts, totalPosts);
    }

    /**
     * 게시글 상세 조회
     *
     * @param userId 요청자 ID
     * @param postId 조회할 게시글 ID
     * @return 게시글 상세 내용 및 댓글 목록 포함 DTO
     */
    @Override
    public PostDetailResponseDto getPostDetail(Long userId, Long postId) {
        validateUser(userId);
        PostDetailResponseDto postDetail = postMapper.getPostByPostId(postId);

        if (postDetail == null) {
            throw new CustomException(ErrorResponseCode.POST_NOT_FOUND);
        }

        List<CommentResponseDto> comments = commentService.getComments(userId, postId);
        postDetail.setComments(comments);

        return postDetail;
    }

    /**
     * 게시글 수정
     *
     * @param userId         요청자 ID (작성자 본인 확인용)
     * @param postId         수정할 게시글 ID
     * @param postRequestDto 수정될 내용 DTO (삭제할 파일 ID 목록 포함)
     * @param files          새로 추가할 첨부 파일 리스트
     * @return 수정된 게시글 ID
     */
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

        List<Long> deleteFileIds = postRequestDto.getDeleteFileIds();
        if (deleteFileIds != null && !deleteFileIds.isEmpty()) {
            List<PostFile> filesToDelete = postMapper.getPostFilesByFileIds(deleteFileIds);
            deleteS3Files(filesToDelete);
            postMapper.deletePostFilesByFileIds(deleteFileIds);
        }

        if (files != null && !files.isEmpty()) {
            savePostFiles(postId, files);
        }

        return postId;
    }

    /**
     * 게시글 삭제
     *
     * @param userId 요청자 ID (작성자 본인 확인용)
     * @param postId 삭제할 게시글 ID
     */
    @Override
    @Transactional
    public void deletePost(Long userId, Long postId) {
        validateUser(userId);
        validatePostOwner(userId, postId);

        List<PostFile> attachedFiles = postMapper.getPostFilesByPostId(postId);

        postMapper.deletePost(userId, postId);
        deleteS3Files(attachedFiles);

        if (attachedFiles != null && !attachedFiles.isEmpty()) {
            postMapper.deletePostFilesByPostId(postId);
        }
    }

    /**
     * 조회수 증가
     *
     * @param postId 대상 게시글 ID
     */
    @Override
    @Transactional
    public void increaseViewCount(Long postId) {
        postMapper.increaseViewCount(postId);
    }

    /**
     * 좋아요 토글 (등록/취소)
     *
     * @param userId 좋아요 누른 사용자 ID
     * @param postId 대상 게시글 ID
     * @return true(좋아요 등록됨), false(좋아요 취소됨)
     */
    @Override
    @Transactional
    public boolean toggleLike(Long userId, Long postId) {
        validateUser(userId);

        PostDetailResponseDto post = postMapper.getPostByPostId(postId);
        if (post == null) {
            throw new CustomException(ErrorResponseCode.POST_NOT_FOUND);
        }

        boolean isLiked = postMapper.existsLike(userId, postId);


        if (isLiked) {
            postMapper.deleteLike(userId, postId);
            return false;
        } else {
            postMapper.insertLike(userId, postId);
            if (!post.getUser().getUserId().equals(userId)) {
                User likeUser = userMapper.findById(userId).orElseThrow(() -> new CustomException(ErrorResponseCode.USER_NOT_FOUND));// 좋아요 누른 사람 이름

                notificationService.sendToUser(
                        post.getUser().getUserId(), // 받는 사람 (글 작성자)
                        userId,                           // 보낸 사람 (좋아요 누른 사람)
                        NotificationType.POST_LIKE,
                        likeUser.getUserName() + "님이 회원님의 게시글을 좋아합니다.",
                        postId.intValue(),
                        "/community/" + postId
                );
            }
            return true;
        }
    }

    // --- Helper Methods ---

    private void savePostFiles(Long postId, List<MultipartFile> files) {
        List<PostFile> postFileList = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String fileUrl = s3Service.uploadFile(file, "post");
            String contentType = file.getContentType();
            String fileType = (contentType != null && contentType.startsWith("video")) ? "VIDEO" : "IMAGE";

            PostFile postFile = PostFile.builder()
                    .postId(postId)
                    .fileUrl(fileUrl)
                    .fileType(fileType)
                    .build();
            postFileList.add(postFile);
        }
        if (!postFileList.isEmpty()) {
            postMapper.savePostFiles(postFileList);
        }
    }

    private void deleteS3Files(List<PostFile> files) {
        if (files == null || files.isEmpty()) return;
        for (PostFile file : files) {
            s3Service.deleteFile(file.getFileUrl());
        }
    }

    private static void validateUser(Long userId) {
        if (userId == null) {
            throw new CustomException(ErrorResponseCode.UNAUTHORIZED);
        }
    }

    private void validatePostOwner(Long userId, Long postId) {
        PostDetailResponseDto postDetail = postMapper.getPostByPostId(postId);
        if (postDetail == null) {
            throw new CustomException(ErrorResponseCode.POST_NOT_FOUND);
        }
        if (postDetail.getUser() == null || postDetail.getUser().getUserId() == null) {
            throw new CustomException(ErrorResponseCode.UNAUTHORIZED);
        }
        if (!postDetail.getUser().getUserId().equals(userId)) {
            throw new CustomException(ErrorResponseCode.UNAUTHORIZED);
        }
    }
}