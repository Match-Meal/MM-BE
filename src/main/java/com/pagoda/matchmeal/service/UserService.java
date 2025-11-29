package com.pagoda.matchmeal.service;

import com.pagoda.matchmeal.model.entity.User;

public interface UserService {

    // 소셜 로그인 시 회원가입 및 정보 업데이트
    User saveOrUpdate(String socialId, String email, String name, String platform);

}
