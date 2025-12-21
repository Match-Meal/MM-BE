package com.pagoda.matchmeal.mapper;

import com.pagoda.matchmeal.model.dto.PostSearchCond;
import com.pagoda.matchmeal.model.dto.response.PostDetailResponseDto;
import com.pagoda.matchmeal.model.entity.Post;
import com.pagoda.matchmeal.model.entity.PostFile;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 게시글(Post) 및 첨부파일 관리 매퍼
 * - 게시글 CRUD, 검색, 좋아요, 조회수 기능 포함
 */
public interface PostMapper {

    // --- 게시글 기본 CRUD ---

    /**
     * 게시글 생성
     */
    void savePost(Post post);

    /**
     * 첨부 파일 대량 저장 (이미지 등)
     */
    void savePostFiles(List<PostFile> postFiles);

    /**
     * 특정 게시글의 첨부 파일 목록 조회
     */
    List<PostFile> getPostFilesByPostId(Long postId);

    /**
     * 특정 게시글의 모든 첨부 파일 삭제
     */
    void deletePostFilesByPostId(Long postId);

    /**
     * 게시글 목록 조회 (검색 조건 및 페이징 적용)
     */
    List<PostDetailResponseDto> getPosts(PostSearchCond cond);

    /**
     * 게시글 상세 조회
     */
    PostDetailResponseDto getPostByPostId(Long postId);

    /**
     * 게시글 수정 (내용, 제목 등)
     */
    int updatePost(Post post);

    /**
     * 게시글 삭제 (작성자 본인 확인 포함)
     */
    void deletePost(@Param("userId") Long userId, @Param("postId") Long postId);

    /**
     * 검색 조건에 맞는 전체 게시글 수 조회 (페이징용)
     */
    int countPosts(PostSearchCond cond);

    /**
     * 모든 게시글 삭제 (테스트 또는 관리자용)
     */
    void deleteAllPosts();

    /**
     * 게시글 조회수 1 증가
     */
    void increaseViewCount(Long postId);


    // --- 좋아요(Like) 기능 ---

    /**
     * 사용자가 해당 게시글에 좋아요를 눌렀는지 확인
     */
    boolean existsLike(@Param("userId") Long userId, @Param("postId") Long postId);

    /**
     * 게시글 좋아요 추가
     */
    void insertLike(@Param("userId") Long userId, @Param("postId") Long postId);

    /**
     * 게시글 좋아요 취소
     */
    void deleteLike(@Param("userId") Long userId, @Param("postId") Long postId);


    // --- 파일 개별 관리 (수정 시 사용) ---

    /**
     * 파일 ID 리스트로 파일 정보 조회
     */
    List<PostFile> getPostFilesByFileIds(List<Long> fileIds);

    /**
     * 특정 파일들만 선택 삭제
     */
    void deletePostFilesByFileIds(List<Long> fileIds);
}