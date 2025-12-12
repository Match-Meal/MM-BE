package com.pagoda.matchmeal.mapper;

import com.pagoda.matchmeal.model.dto.PostSearchCond;
import com.pagoda.matchmeal.model.dto.response.PostDetailResponseDto;
import com.pagoda.matchmeal.model.entity.Post;
import com.pagoda.matchmeal.model.entity.PostFile;
import com.pagoda.matchmeal.model.enums.PostCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // 테스트 끝나고 DB 롤백
class PostMapperTest {

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long testUserId = 1L;

    @BeforeEach
    void setUp() {
        // ★ [데이터 초기화] 외래키 순서 고려해서 삭제 (자식 -> 부모)
        // 1. 게시글 관련 데이터 삭제
        postMapper.deleteAllPosts(); // Mapper 메소드 사용
    }

    @Test
    @DisplayName("게시글 저장 및 상세 조회 테스트")
    void saveAndFindTest() {
        // ...
        Post post = Post.builder()
                .userId(testUserId)
                .category(PostCategory.DIET)
                .title("오늘의 식단")
                .content("닭가슴살 냠냠")
                .build();

        postMapper.savePost(post);

        assertThat(post.getPostId()).isNotNull();

        PostDetailResponseDto result = postMapper.getPostByPostId(post.getPostId());
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("오늘의 식단");
        assertThat(result.getUser().getUserName()).isEqualTo("헬스보이철수");
    }

    @Test
    @DisplayName("첨부파일 일괄 저장 테스트")
    void saveFilesTest() {
        // given
        Post post = Post.builder().userId(testUserId).category(PostCategory.FREE).title("파일테스트").content("내용").build();
        postMapper.savePost(post);

        List<PostFile> files = List.of(
                PostFile.builder().postId(post.getPostId()).fileUrl("image.jpg").fileType("IMAGE").build(),
                PostFile.builder().postId(post.getPostId()).fileUrl("video.mp4").fileType("VIDEO").build()
        );

        // when
        postMapper.savePostFiles(files);

        // then
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM post_files WHERE post_id = ?", Integer.class, post.getPostId());

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("게시글 검색 테스트 (카테고리 + 키워드)")
    void searchTest() {
        // given
        createMockPost(PostCategory.DIET, "다이어트 식단 1");
        createMockPost(PostCategory.DIET, "다이어트 식단 2");
        createMockPost(PostCategory.FREE, "자유게시글");

        // when
        PostSearchCond cond = PostSearchCond.builder()
                .category("DIET")
                .keyword("식단")
                .searchType("TITLE")
                .limit(10)
                .offset(0)
                .build();

        List<PostDetailResponseDto> results = postMapper.getPosts(cond);
        int totalCount = postMapper.countPosts(cond);

        // then
        assertThat(results).hasSize(2);
        assertThat(totalCount).isEqualTo(2);
        assertThat(results.get(0).getTitle()).contains("식단");
    }

    @Test
    @DisplayName("게시글 수정 테스트")
    void updateTest() {
        // given
        Post post = Post.builder().userId(testUserId).category(PostCategory.DIET).title("원래제목").content("원래내용").build();
        postMapper.savePost(post);

        Post updateParam = Post.builder()
                .userId(testUserId)
                .postId(post.getPostId())
                .title("바뀐제목")
                .content("바뀐내용")
                .category(PostCategory.FREE)
                .build();

        // when
        // XML에서 userId를 체크하므로 testUserId를 넘겨줌
        int updateCount = postMapper.updatePost(updateParam);

        // then
        assertThat(updateCount).isEqualTo(1);
        PostDetailResponseDto result = postMapper.getPostByPostId(post.getPostId());
        assertThat(result.getTitle()).isEqualTo("바뀐제목");
        assertThat(result.getCategory()).isEqualTo("FREE");
    }

    @Test
    @DisplayName("게시글 삭제(Soft Delete) 테스트")
    void deleteTest() {
        // given
        Post post = Post.builder().userId(testUserId).category(PostCategory.DIET).title("삭제글").content("내용").build();
        postMapper.savePost(post);
        Long postId = post.getPostId();

        // when
        postMapper.deletePost(testUserId, postId);

        // then
        // 1. 상세 조회 불가 (deleted_at IS NULL 필터링)
        PostDetailResponseDto result = postMapper.getPostByPostId(postId);
        assertThat(result).isNull();

        // 2. 검색 시 카운트 0
        Boolean isSoftDeleted = jdbcTemplate.queryForObject(
                "SELECT count(*) > 0 FROM posts WHERE post_id = ? AND deleted_at IS NOT NULL",
                Boolean.class,
                postId
        );

        assertThat(isSoftDeleted).isTrue();
    }

    // 테스트용 데이터 생성 헬퍼 메서드
    private void createMockPost(PostCategory category, String title) {
        Post post = Post.builder()
                .userId(testUserId)
                .category(category)
                .title(title)
                .content("테스트 내용")
                .build();
        postMapper.savePost(post);
    }

    @Test
    @DisplayName("조회수 증가 쿼리 테스트")
    void increaseViewCountTest() {
        // given
        Post post = Post.builder().userId(testUserId).category(PostCategory.FREE).title("조회수").content("내용").build();
        postMapper.savePost(post); // 초기 view_count = 0

        // when
        postMapper.increaseViewCount(post.getPostId());

        // then
        PostDetailResponseDto result = postMapper.getPostByPostId(post.getPostId());
        assertThat(result.getViewCount()).isEqualTo(1); // 0 -> 1 증가 확인
    }

    @Test
    @DisplayName("좋아요 등록/취소/확인 쿼리 테스트")
    void likeQueryTest() {
        // given
        Post post = Post.builder().userId(testUserId).category(PostCategory.FREE).title("좋아요").content("내용").build();
        postMapper.savePost(post);
        Long postId = post.getPostId();

        // 1. 초기 상태 확인 (좋아요 없음)
        boolean existsBefore = postMapper.existsLike(testUserId, postId);
        assertThat(existsBefore).isFalse();

        // 2. 좋아요 등록 (Insert)
        postMapper.insertLike(testUserId, postId);
        boolean existsAfterInsert = postMapper.existsLike(testUserId, postId);
        assertThat(existsAfterInsert).isTrue();

        // 3. 좋아요 개수 확인 (서브쿼리 등 확인용)
        // (getPostByPostId 등에서 likeCount를 가져오는지 간접 검증 가능)
        // PostDetailResponseDto detail = postMapper.getPostByPostId(postId);
        // assertThat(detail.getLikeCount()).isEqualTo(1);

        // 4. 좋아요 취소 (Delete)
        postMapper.deleteLike(testUserId, postId);
        boolean existsAfterDelete = postMapper.existsLike(testUserId, postId);
        assertThat(existsAfterDelete).isFalse();
    }
}