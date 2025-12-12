package com.pagoda.matchmeal.mapper;

import com.pagoda.matchmeal.model.dto.response.CommentResponseDto;
import com.pagoda.matchmeal.model.entity.Comment;
import com.pagoda.matchmeal.model.entity.Post;
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
@Transactional // 테스트 종료 후 롤백
class CommentMapperTest {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long testUserId = 1L;
    private Long testPostId;

    @BeforeEach
    void setUp() {
        // 1. 테스트를 위한 유저 데이터 확인 (없으면 생성)
        // H2 DB가 초기화될 때 data.sql 등으로 유저가 없을 수도 있으므로 안전하게 병합(MERGE) 혹은 확인
        Integer userCount = jdbcTemplate.queryForObject("SELECT count(*) FROM users WHERE user_id = ?", Integer.class, testUserId);
        if (userCount == 0) {
            jdbcTemplate.update("INSERT INTO users (user_id, email, user_name, role) VALUES (?, 'test@test.com', '테스트유저', 'ROLE_USER')", testUserId);
        }

        // 2. 테스트용 게시글 생성 (댓글의 부모)
        Post post = Post.builder()
                .userId(testUserId)
                .category(PostCategory.FREE)
                .title("댓글테스트글")
                .content("내용")
                .build();
        postMapper.savePost(post);
        testPostId = post.getPostId();
    }

    @Test
    @DisplayName("댓글 저장 및 단건 조회 테스트")
    void saveAndFindTest() {
        // given
        Comment comment = Comment.builder()
                .userId(testUserId)
                .postId(testPostId)
                .content("테스트 댓글입니다.")
                .build();

        // when
        commentMapper.save(comment);

        // then
        assertThat(comment.getCommentId()).isNotNull(); // ID 생성 확인

        CommentResponseDto savedComment = commentMapper.findByCommentId(comment.getCommentId());
        assertThat(savedComment).isNotNull();
        assertThat(savedComment.getContent()).isEqualTo("테스트 댓글입니다.");
    }

    @Test
    @DisplayName("계층형 댓글 조회 및 정렬 테스트 (부모 -> 자식 순서)")
    void findAllByPostIdTest() {
        // given
        // 1. 부모 댓글 저장 (먼저 작성됨)
        Comment parent = Comment.builder()
                .userId(testUserId).postId(testPostId).content("부모댓글").build();
        commentMapper.save(parent);

        // 2. 다른 부모 댓글 저장 (나중에 작성됨)
        Comment parent2 = Comment.builder()
                .userId(testUserId).postId(testPostId).content("부모댓글2").build();
        commentMapper.save(parent2);

        // 3. 자식 댓글 저장 (부모1의 대댓글)
        Comment child = Comment.builder()
                .userId(testUserId).postId(testPostId)
                .content("대댓글")
                .parentCommentId(parent.getCommentId()) // 부모 ID 지정
                .build();
        commentMapper.save(child);

        // when
        // 조회 (XML의 ORDER BY 로직 검증)
        List<CommentResponseDto> list = commentMapper.findAllByPostId(testUserId, testPostId);

        // then
        assertThat(list).hasSize(3);

        // 정렬 순서 검증:
        // XML 쿼리: ORDER BY COALESCE(parent_id, id) ASC, created_at ASC
        // 예상 순서: 부모1 -> 자식(부모1의 대댓글) -> 부모2

        // 1. 첫 번째: 부모1
        assertThat(list.get(0).getCommentId()).isEqualTo(parent.getCommentId());

        // 2. 두 번째: 자식 (부모1 그룹 내에서 생성 시간 순)
        assertThat(list.get(1).getCommentId()).isEqualTo(child.getCommentId());
        assertThat(list.get(1).getParentCommentId()).isEqualTo(parent.getCommentId());

        // 3. 세 번째: 부모2
        assertThat(list.get(2).getCommentId()).isEqualTo(parent2.getCommentId());

        // 4. 유저 정보 매핑 확인 (<association> 동작 여부)
        assertThat(list.get(0).getUser()).isNotNull();
        assertThat(list.get(0).getUser().getUserName()).isEqualTo("헬스보이철수");
    }

    @Test
    @DisplayName("댓글 수정 테스트")
    void updateTest() {
        // given
        Comment comment = Comment.builder()
                .userId(testUserId).postId(testPostId).content("원래내용").build();
        commentMapper.save(comment);

        // when
        comment.setContent("수정된내용");
        commentMapper.update(comment);

        // then
        CommentResponseDto updated = commentMapper.findByCommentId(comment.getCommentId());
        assertThat(updated.getContent()).isEqualTo("수정된내용");
        // updated_at이 갱신되었는지 확인 (단, 너무 빨라서 같을 수도 있으니 NotNull 체크)
        assertThat(updated.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("댓글 삭제(Soft Delete) 테스트")
    void deleteTest() {
        // given
        Comment comment = Comment.builder()
                .userId(testUserId).postId(testPostId).content("삭제될댓글").build();
        commentMapper.save(comment);
        Long commentId = comment.getCommentId();

        // when
        commentMapper.delete(commentId);

        // then
        // 1. findAllByPostId 조회 시 목록에서 사라져야 함 (XML에 deleted_at IS NULL 조건이 있다면)
        List<CommentResponseDto> list = commentMapper.findAllByPostId(testPostId, testUserId);
        boolean exists = list.stream().anyMatch(c -> c.getCommentId().equals(commentId));
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("댓글 좋아요 등록/취소 테스트")
    void likeTest() {
        // given
        Comment comment = Comment.builder()
                .userId(testUserId).postId(testPostId).content("좋아요댓글").build();
        commentMapper.save(comment);
        Long commentId = comment.getCommentId();

        // 1. 초기 상태: 좋아요 없음
        boolean existsBefore = commentMapper.existsLike(testUserId, commentId);
        assertThat(existsBefore).isFalse();

        // 2. 좋아요 등록
        commentMapper.insertLike(testUserId, commentId);
        boolean existsAfterInsert = commentMapper.existsLike(testUserId, commentId);
        assertThat(existsAfterInsert).isTrue();

        // 3. 좋아요 개수 확인 (findAllByPostId 서브쿼리 검증)
        List<CommentResponseDto> list = commentMapper.findAllByPostId(testUserId, testPostId);
        CommentResponseDto dto = list.stream()
                .filter(c -> c.getCommentId().equals(commentId))
                .findFirst().orElseThrow();
        assertThat(dto.getLikeCount()).isEqualTo(1);

        // 4. 좋아요 취소
        commentMapper.deleteLike(testUserId, commentId);
        boolean existsAfterDelete = commentMapper.existsLike(testUserId, commentId);
        assertThat(existsAfterDelete).isFalse();
    }
}