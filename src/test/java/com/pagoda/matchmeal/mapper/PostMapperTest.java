package com.pagoda.matchmeal.mapper;

import com.pagoda.matchmeal.model.dto.PostSearchCond;
import com.pagoda.matchmeal.model.dto.response.PostDetailResponseDto;
import com.pagoda.matchmeal.model.entity.Post;
import com.pagoda.matchmeal.model.entity.PostFile;
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
                .category("DIET")
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
        Post post = Post.builder().userId(testUserId).category("FREE").title("파일테스트").content("내용").build();
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
        createMockPost("DIET", "다이어트 식단 1");
        createMockPost("DIET", "다이어트 식단 2");
        createMockPost("FREE", "자유게시글");

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
        Post post = Post.builder().userId(testUserId).category("DIET").title("원래제목").content("원래내용").build();
        postMapper.savePost(post);

        Post updateParam = Post.builder()
                .userId(testUserId)
                .postId(post.getPostId())
                .title("바뀐제목")
                .content("바뀐내용")
                .category("FREE")
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
        Post post = Post.builder().userId(testUserId).category("DIET").title("삭제글").content("내용").build();
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
    private void createMockPost(String category, String title) {
        Post post = Post.builder()
                .userId(testUserId)
                .category(category)
                .title(title)
                .content("테스트 내용")
                .build();
        postMapper.savePost(post);
    }
}