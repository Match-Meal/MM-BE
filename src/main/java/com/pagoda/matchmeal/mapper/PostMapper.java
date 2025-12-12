package com.pagoda.matchmeal.mapper;

import com.pagoda.matchmeal.model.dto.PostSearchCond;
import com.pagoda.matchmeal.model.dto.response.PostDetailResponseDto;
import com.pagoda.matchmeal.model.entity.Post;
import com.pagoda.matchmeal.model.entity.PostFile;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PostMapper {

    void savePost(Post post);

    void savePostFiles(List<PostFile> postFiles);

    List<PostFile> getPostFilesByPostId(Long postId);

    void deletePostFilesByPostId(Long postId);

    List<PostDetailResponseDto> getPosts(PostSearchCond cond);

    PostDetailResponseDto getPostByPostId(Long postId);

    int updatePost(Post post);

    void deletePost(@Param("userId") Long userId, @Param("postId") Long postId);

    int countPosts(PostSearchCond cond);

    void deleteAllPosts();

    void increaseViewCount(Long postId);

    boolean existsLike(@Param("userId") Long userId, @Param("postId") Long postId);

    void insertLike(@Param("userId") Long userId, @Param("postId") Long postId);

    void deleteLike(@Param("userId") Long userId, @Param("postId") Long postId);
}
