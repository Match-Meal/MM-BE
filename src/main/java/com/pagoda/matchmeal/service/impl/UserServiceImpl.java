package com.pagoda.matchmeal.service.impl;

import com.pagoda.matchmeal.mapper.UserMapper;
import com.pagoda.matchmeal.model.entity.User;
import com.pagoda.matchmeal.model.enums.UserRole;
import com.pagoda.matchmeal.model.enums.UserStatus;
import com.pagoda.matchmeal.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    @Transactional
    public User saveOrUpdate(String socialId, String email, String name, String platform) {

        // DB에서 socialId로 조회
        return userMapper.findBySocialId(socialId)
                .map(entity -> {
                    // 이미 존재하는 회원인 경우 -> 이름 업데이트
                    entity.setUserName(name);
                    userMapper.update(entity);
                    return entity;
                })
                .orElseGet(() -> {
                    // 없는 회원인 경우 -> 신규 회원가입
                    User newUser = User.builder()
                            .socialId(socialId)
                            .email(email)
                            .userName(name)
                            .platform(platform)
                            .role(UserRole.ROLE_USER)
                            .status(UserStatus.ACTIVE)
                            .build();
                    userMapper.save(newUser);
                    return newUser;
                });
    }
}
