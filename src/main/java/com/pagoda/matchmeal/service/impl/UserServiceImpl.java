package com.pagoda.matchmeal.service.impl;

import com.pagoda.matchmeal.mapper.UserMapper;
import com.pagoda.matchmeal.model.dto.UserProfileDto;
import com.pagoda.matchmeal.model.entity.User;
import com.pagoda.matchmeal.model.enums.UserRole;
import com.pagoda.matchmeal.model.enums.UserStatus;
import com.pagoda.matchmeal.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public Map<String, Object> processLoginOrRegister(String socialId, String email, String name, String platform) {
        Map<String, Object> result = new HashMap<>();

        User user = userMapper.findBySocialId(socialId).orElse(null);
        boolean isNew = false;

        if (user == null) {
            // 신규 회원
            isNew = true;
            user = User.builder()
                    .socialId(socialId)
                    .email(email)
                    .userName(name) // 최초 가입시 소셜 이름으로 기본값 설정
                    .platform(platform)
                    .role(UserRole.ROLE_USER)
                    .status(UserStatus.ACTIVE)
                    .build();
            userMapper.save(user);
        }
        result.put("user", user);
        result.put("isNew", isNew);

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public User findBySocialId(String socialId) {
        return userMapper.findBySocialId(socialId).orElse(null);
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, UserProfileDto profileDto) {
        // 기존 유저 조회
//        User user = User.builder()
//                .id(userId)
//                .gender(profileDto.getGender())
//                .birthDate(profileDto.getBirthDate())
//                .heightCm(profileDto.getHeightCm())
//                .weightKg(profileDto.getWeightKg())
//                .build();
//
//        userMapper.updateUserName(user);
    }


}
