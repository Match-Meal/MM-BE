package com.pagoda.matchmeal.service;

import com.pagoda.matchmeal.model.dto.UserProfileDto;
import com.pagoda.matchmeal.model.entity.User;

import java.util.Map;

public interface UserService {

    // 소셜 로그인 시 회원가입 및 정보 업데이트
    User saveOrUpdate(String socialId, String email, String name, String platform);

    // socialId로 회원 조회
    User findBySocialId(String socialId);

    // 프로필 업데이트
    void updateProfile(Long userId, UserProfileDto profileDto);
}
