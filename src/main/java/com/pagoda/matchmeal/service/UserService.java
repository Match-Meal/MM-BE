package com.pagoda.matchmeal.service;

import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.model.dto.UserProfileDto;
import com.pagoda.matchmeal.model.entity.User;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    // userId값으로 프로필 조회
    UserDto getMyProfile(Long userId);

    // 프로필 업데이트
    void updateProfile(Long userId, UserProfileDto profileDto, MultipartFile file);

    // 프로필 공개여부 설정
    UserDto getUserProfile(Long targetUserId);


    void updateVisibility(Long userId, boolean isPublic);

    void withdrawUser(Long userId);

    UserDto convertUserToDto(User user);

}
