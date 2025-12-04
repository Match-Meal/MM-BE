package com.pagoda.matchmeal.service;

import com.pagoda.matchmeal.model.dto.UserProfileDto;
import com.pagoda.matchmeal.model.entity.User;

import java.util.HashMap;
import java.util.Map;

public interface UserService {

    // 로그인 처리 (저장/업데이트 후 isNew 여부 반환)
    Map<String, Object> processLoginOrRegister(String socialId, String email, String name, String platform);

    // socialId로 회원 조회
    User findBySocialId(String socialId);

    // 프로필 업데이트
    void updateProfile(Long userId, UserProfileDto profileDto);
}
