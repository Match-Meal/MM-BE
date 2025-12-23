package com.pagoda.matchmeal.service;

import com.pagoda.matchmeal.common.exception.CustomException;
import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import com.pagoda.matchmeal.common.response.PageInfoResponseDto;
import com.pagoda.matchmeal.mapper.FollowMapper;
import com.pagoda.matchmeal.mapper.PostMapper;
import com.pagoda.matchmeal.mapper.UserMapper;
import com.pagoda.matchmeal.model.dto.PostSearchCond;
import com.pagoda.matchmeal.model.dto.UserSimpleDto;
import com.pagoda.matchmeal.model.dto.request.PostRequestDto;
import com.pagoda.matchmeal.model.dto.response.PostDetailResponseDto;
import com.pagoda.matchmeal.model.entity.Post;
import com.pagoda.matchmeal.model.entity.PostFile;
import com.pagoda.matchmeal.model.entity.User;
import com.pagoda.matchmeal.model.enums.PostCategory;
import com.pagoda.matchmeal.service.impl.PostServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Mockito 환경 활성화
class PostServiceImplTest {

    @InjectMocks
    private PostServiceImpl postService;

    @Mock
    private PostMapper postMapper;

    @Mock
    private S3Service s3Service;

    @Mock
    private CommentService commentService;

    // [추가] 알림 관련 Mock
    @Mock
    private NotificationService notificationService;
    @Mock
    private FollowMapper followMapper; // 팔로워 조회용
    @Mock
    private UserMapper userMapper;     // 작성자 이름 조회용

    // 테스트용 상수
    private final Long USER_ID = 1L;
    private final Long POST_ID = 100L;

    @Test
    @DisplayName("게시글 작성 성공 - 파일 포함 (알림 로직 포함)")
    void writePost_Success() {
        // given
        PostRequestDto requestDto = new PostRequestDto();
        requestDto.setTitle("제목");
        requestDto.setContent("내용");
        requestDto.setCategory(PostCategory.DIET); // 일반 카테고리 (팔로워 알림 대상)

        List<MultipartFile> files = List.of(
                new MockMultipartFile("files", "image.jpg", "image/jpeg", "dummy".getBytes())
        );

        // 1. 게시글 ID 생성 흉내
        doAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            ReflectionTestUtils.setField(post, "postId", POST_ID);
            return null;
        }).when(postMapper).savePost(any(Post.class));

        given(s3Service.uploadFile(any(), anyString())).willReturn("http://s3-url.com/image.jpg");

        // [추가] 2. 알림 발송을 위한 Mock 설정
        // 작성자 이름 조회
        User writer = User.builder().userId(USER_ID).userName("Writer").build();
        given(userMapper.findById(USER_ID)).willReturn(Optional.of(writer));

        // 팔로워 목록 조회
        given(followMapper.findFollowerIds(USER_ID)).willReturn(List.of(2L, 3L)); // 팔로워 2명 가정

        // when
        Long savedId = postService.writePost(USER_ID, requestDto, files);

        // then
        assertThat(savedId).isEqualTo(POST_ID);
    }

    @Test
    @DisplayName("게시글 목록 조회 성공 - 페이징 계산 확인")
    void getPost_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<PostDetailResponseDto> mockList = List.of(new PostDetailResponseDto());

        given(postMapper.getPosts(any(PostSearchCond.class))).willReturn(mockList);
        given(postMapper.countPosts(any(PostSearchCond.class))).willReturn(1);

        // when
        PageInfoResponseDto<PostDetailResponseDto> result =
                postService.getPost(USER_ID, "키워드", "TITLE", "DIET", null, null, null, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getPageInfo().getTotalCount()).isEqualTo(1);
        verify(postMapper).getPosts(any(PostSearchCond.class));
    }

    @Test
    @DisplayName("게시글 수정 성공 - 파일 선택 삭제 및 새 파일 추가 검증")
    void updatePost_Success_WithFileChange() {
        // given
        // 1. 기존 게시글 정보 Mocking (본인 확인 및 존재 여부 확인용)
        PostDetailResponseDto existingPost = new PostDetailResponseDto();
        UserSimpleDto userDto = UserSimpleDto.builder().userId(USER_ID).build();
        ReflectionTestUtils.setField(existingPost, "user", userDto);

        given(postMapper.getPostByPostId(POST_ID)).willReturn(existingPost);

        // 2. 업데이트 실행 시 성공 반환 (텍스트 수정)
        given(postMapper.updatePost(any(Post.class))).willReturn(1);

        // ======================================================
        // [수정 포인트 1] 삭제할 파일 ID 설정 및 Mocking
        // ======================================================
        Long deleteFileId = 10L;
        String deleteFileUrl = "http://old-url.com/image.jpg";

        // 3-1. 요청 DTO에 삭제할 파일 ID 리스트 담기
        PostRequestDto requestDto = new PostRequestDto();
        ReflectionTestUtils.setField(requestDto, "title", "수정된 제목");
        ReflectionTestUtils.setField(requestDto, "content", "수정된 내용");
        ReflectionTestUtils.setField(requestDto, "deleteFileIds", List.of(deleteFileId)); // 삭제 요청

        // 3-2. "선택된 파일 ID"로 DB 조회 시 리턴할 객체 Mocking
        List<PostFile> filesToDelete = List.of(
                PostFile.builder().fileId(deleteFileId).fileUrl(deleteFileUrl).build()
        );
        given(postMapper.getPostFilesByFileIds(List.of(deleteFileId))).willReturn(filesToDelete);

        // 4. 새 파일 준비 (추가할 파일)
        List<MultipartFile> newFiles = List.of(
                new MockMultipartFile("files", "new.jpg", "image/jpeg", "new-image-content".getBytes())
        );

        // when
        Long updatedId = postService.updatePost(USER_ID, POST_ID, requestDto, newFiles);

        // then
        assertThat(updatedId).isEqualTo(POST_ID);

        // [핵심 로직 검증]

        // 1. S3 삭제가 "삭제 요청된 파일 URL"에 대해 수행되었는가?
        verify(s3Service).deleteFile(deleteFileUrl);

        // 2. DB 삭제가 "전체 삭제"가 아닌 "ID 기반 선택 삭제"로 호출되었는가?
        // (deletePostFilesByPostId가 아니라 deletePostFilesByFileIds여야 함)
        verify(postMapper).deletePostFilesByFileIds(List.of(deleteFileId));

        // 3. 새 파일 S3 업로드가 수행되었는가?
        verify(s3Service).uploadFile(any(MultipartFile.class), anyString());

        // 4. 새 파일 DB 저장이 수행되었는가? (Append 방식)
        verify(postMapper).savePostFiles(anyList());
    }

    @Test
    @DisplayName("게시글 수정 실패 - 권한 없음 (작성자가 아님)")
    void updatePost_Fail_Unauthorized() {
        // given
        // [수정] Mock 대신 실제 DTO 객체 사용
        UserSimpleDto otherUser = new UserSimpleDto();
        ReflectionTestUtils.setField(otherUser, "userId", 999L); // 다른 사용자 ID

        PostDetailResponseDto otherUserPost = new PostDetailResponseDto();
        ReflectionTestUtils.setField(otherUserPost, "user", otherUser); // user 필드 주입

        given(postMapper.getPostByPostId(POST_ID)).willReturn(otherUserPost);

        // when & then
        assertThatThrownBy(() -> postService.updatePost(USER_ID, POST_ID, new PostRequestDto(), null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorResponseCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("게시글 삭제 성공 - 파일도 함께 삭제")
    void deletePost_Success() {
        // given
        // 1. 게시글 존재 및 권한 확인
        PostDetailResponseDto targetPost = new PostDetailResponseDto();
        UserSimpleDto userDto = UserSimpleDto.builder().userId(USER_ID).build();
        ReflectionTestUtils.setField(targetPost, "user", userDto);

        given(postMapper.getPostByPostId(POST_ID)).willReturn(targetPost);

        // 2. 기존 파일 조회 Mock
        List<PostFile> attachedFiles = List.of(
                PostFile.builder().fileUrl("http://delete-me.com").build()
        );
        given(postMapper.getPostFilesByPostId(POST_ID)).willReturn(attachedFiles);

        // when
        postService.deletePost(USER_ID, POST_ID);

        // then
        // 1. DB Soft Delete 호출 확인 (파라미터 순서 주의: userId, postId)
        verify(postMapper).deletePost(USER_ID, POST_ID);
        // 2. S3 파일 삭제 호출 확인
        verify(s3Service).deleteFile("http://delete-me.com");
        // 3. DB 파일 메타데이터 삭제 호출 확인
        verify(postMapper).deletePostFilesByPostId(POST_ID);
    }

    @Test
    @DisplayName("게시글 삭제 실패 - 존재하지 않는 게시글")
    void deletePost_Fail_NotFound() {
        // given
        // 없는 게시글 조회 시 null 반환
        given(postMapper.getPostByPostId(POST_ID)).willReturn(null);

        // when & then
        // 이제 서비스 코드 순서를 바꿨으므로 NPE가 아니라 POST_NOT_FOUND가 떠야 함
        assertThatThrownBy(() -> postService.deletePost(USER_ID, POST_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorResponseCode.POST_NOT_FOUND);
    }

    @Test
    @DisplayName("조회수 증가 호출 테스트")
    void increaseViewCount_Success() {
        // when
        postService.increaseViewCount(POST_ID);

        // then
        verify(postMapper).increaseViewCount(POST_ID);
    }

    @Test
    @DisplayName("좋아요 토글 - 안 눌렀을 때 (등록) + 알림 발송")
    void toggleLike_Insert() {
        // given
        // [수정] 실제 DTO 생성 (작성자 ID 필요)
        UserSimpleDto postOwner = new UserSimpleDto();
        ReflectionTestUtils.setField(postOwner, "userId", 999L); // 글 작성자 (나 아님)

        PostDetailResponseDto postDto = new PostDetailResponseDto();
        ReflectionTestUtils.setField(postDto, "user", postOwner);
        ReflectionTestUtils.setField(postDto, "postId", POST_ID);

        given(postMapper.getPostByPostId(POST_ID)).willReturn(postDto);
        given(postMapper.existsLike(USER_ID, POST_ID)).willReturn(false); // 안 누른 상태

        // 좋아요 누른 사람(나) 정보 조회 Mock (알림용)
        User liker = User.builder().userId(USER_ID).userName("Liker").build();
        given(userMapper.findById(USER_ID)).willReturn(Optional.of(liker));

        // when
        boolean result = postService.toggleLike(USER_ID, POST_ID);

        // then
        assertThat(result).isTrue();
        verify(postMapper).insertLike(USER_ID, POST_ID);
    }

    @Test
    @DisplayName("좋아요 토글 - 이미 눌렀을 때 (취소)")
    void toggleLike_Delete() {
        // given
        given(postMapper.getPostByPostId(POST_ID)).willReturn(new PostDetailResponseDto());
        // 좋아요 여부 -> true (이미 누름)
        given(postMapper.existsLike(USER_ID, POST_ID)).willReturn(true);

        // when
        boolean result = postService.toggleLike(USER_ID, POST_ID);

        // then
        assertThat(result).isFalse(); // 결과는 false (취소됨)
        verify(postMapper).deleteLike(USER_ID, POST_ID); // delete 호출 확인
        verify(postMapper, times(0)).insertLike(USER_ID, POST_ID); // insert 호출 안 됨 확인
    }
}