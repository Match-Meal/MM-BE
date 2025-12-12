package com.pagoda.matchmeal.service;

import com.pagoda.matchmeal.common.exception.CustomException;
import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import com.pagoda.matchmeal.common.response.PageInfoResponseDto;
import com.pagoda.matchmeal.mapper.PostMapper;
import com.pagoda.matchmeal.model.dto.PostSearchCond;
import com.pagoda.matchmeal.model.dto.UserSimpleDto;
import com.pagoda.matchmeal.model.dto.request.PostRequestDto;
import com.pagoda.matchmeal.model.dto.response.PostDetailResponseDto;
import com.pagoda.matchmeal.model.entity.Post;
import com.pagoda.matchmeal.model.entity.PostFile;
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

    // 테스트용 상수
    private final Long USER_ID = 1L;
    private final Long POST_ID = 100L;

    @Test
    @DisplayName("게시글 작성 성공 - 파일 포함")
    void writePost_Success() {
        // given
        PostRequestDto requestDto = new PostRequestDto();
        requestDto.setTitle("제목");
        requestDto.setContent("내용");
        requestDto.setCategory("DIET");

        List<MultipartFile> files = List.of(
                new MockMultipartFile("files", "image.jpg", "image/jpeg", "dummy".getBytes())
        );

        // MyBatis의 useGeneratedKeys 흉내내기 (ID 주입)
        doAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            ReflectionTestUtils.setField(post, "postId", POST_ID);
            return null;
        }).when(postMapper).savePost(any(Post.class));

        given(s3Service.uploadFile(any(), anyString())).willReturn("http://s3-url.com/image.jpg");

        // when
        Long savedId = postService.writePost(USER_ID, requestDto, files);

        // then
        assertThat(savedId).isEqualTo(POST_ID);
        verify(postMapper, times(1)).savePost(any(Post.class)); // 게시글 저장 호출 확인
        verify(s3Service, times(1)).uploadFile(any(), anyString()); // S3 업로드 호출 확인
        verify(postMapper, times(1)).savePostFiles(anyList()); // 파일 정보 DB 저장 호출 확인
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
    @DisplayName("게시글 수정 성공 - 파일 전체 교체 로직 검증")
    void updatePost_Success_WithFileChange() {
        // given
        // 1. 기존 게시글 정보 Mocking (본인 확인 통과용)
        PostDetailResponseDto existingPost = new PostDetailResponseDto();
        UserSimpleDto userDto = new UserSimpleDto();
        ReflectionTestUtils.setField(userDto, "userId", USER_ID); // 작성자 ID 일치시키기
        ReflectionTestUtils.setField(existingPost, "user", userDto);

        given(postMapper.getPostByPostId(POST_ID)).willReturn(existingPost);

        // 2. 업데이트 실행 시 성공 반환 (1행 수정)
        given(postMapper.updatePost(any(Post.class))).willReturn(1);

        // 3. 기존 파일 목록 Mocking
        List<PostFile> oldFiles = List.of(
                PostFile.builder().fileId(1L).fileUrl("http://old-url.com").build()
        );
        given(postMapper.getPostFilesByPostId(POST_ID)).willReturn(oldFiles);

        // 4. 새 파일 준비
        List<MultipartFile> newFiles = List.of(
                new MockMultipartFile("files", "new.jpg", "image/jpeg", "new".getBytes())
        );

        // when
        Long updatedId = postService.updatePost(USER_ID, POST_ID, new PostRequestDto(), newFiles);

        // then
        assertThat(updatedId).isEqualTo(POST_ID);

        // [핵심 로직 검증]
        // 1. 기존 파일 S3 삭제 호출되었는가?
        verify(s3Service).deleteFile("http://old-url.com");
        // 2. 기존 파일 DB 삭제 호출되었는가?
        verify(postMapper).deletePostFilesByPostId(POST_ID);
        // 3. 새 파일 S3 업로드 호출되었는가?
        verify(s3Service).uploadFile(any(), anyString());
        // 4. 새 파일 DB 저장 호출되었는가?
        verify(postMapper).savePostFiles(anyList());
    }

    @Test
    @DisplayName("게시글 수정 실패 - 권한 없음 (작성자가 아님)")
    void updatePost_Fail_Unauthorized() {
        // given
        PostDetailResponseDto otherUserPost = new PostDetailResponseDto();
        UserSimpleDto otherUser = new UserSimpleDto();
        ReflectionTestUtils.setField(otherUser, "userId", 999L); // 다른 사용자 ID
        ReflectionTestUtils.setField(otherUserPost, "user", otherUser);

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
        UserSimpleDto userDto = new UserSimpleDto();
        ReflectionTestUtils.setField(userDto, "userId", USER_ID);
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
        given(postMapper.getPostByPostId(POST_ID)).willReturn(null);

        // when & then
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
    @DisplayName("좋아요 토글 - 안 눌렀을 때 (등록)")
    void toggleLike_Insert() {
        // given
        // 1. 게시글 존재 확인
        given(postMapper.getPostByPostId(POST_ID)).willReturn(new PostDetailResponseDto());
        // 2. 좋아요 여부 -> false (안 누름)
        given(postMapper.existsLike(USER_ID, POST_ID)).willReturn(false);

        // when
        boolean result = postService.toggleLike(USER_ID, POST_ID);

        // then
        assertThat(result).isTrue(); // 결과는 true (등록됨)
        verify(postMapper).insertLike(USER_ID, POST_ID); // insert 호출 확인
        verify(postMapper, times(0)).deleteLike(USER_ID, POST_ID); // delete 호출 안 됨 확인
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