package com.pagoda.matchmeal.mapper;

import com.pagoda.matchmeal.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface UserMapper {
    // 회원 가입
    void save(User user);

    // 소셜 정보 갱신
    void update(User user);

    // 조회(소셜ID)
    Optional<User> findBySocialId(String socialId);


}
