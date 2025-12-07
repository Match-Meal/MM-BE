package com.pagoda.matchmeal.service;

import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.model.dto.UserProfileDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface UserService {

    // 로그인 처리 (저장/업데이트 후 isNew 여부 반환)
    Map<String, Object> processLoginOrRegister(String socialId, String email, String name, String platform);

    // userId값으로 프로필 조회
    UserDto getMyProfile(Long userId);

    // 프로필 업데이트
    void updateProfile(Long userId, UserProfileDto profileDto, MultipartFile file);

    // 프로필 공개여부 설정
    UserDto getUserProfile(Long targetUserId);


    void updateVisibility(Long userId, boolean isPublic);


}
