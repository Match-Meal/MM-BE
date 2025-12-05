package com.pagoda.matchmeal.mapper;

import com.pagoda.matchmeal.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface UserMapper {
    // 회원 가입
    void save(User user);

    // UserName 갱신
    void updateUserName(User user);

    // 조회(소셜ID)
    Optional<User> findBySocialId(String socialId);

    Optional<User> findById(Long userId);

    void updateProfile(User user);

    void updateVisibility(User user);

}
