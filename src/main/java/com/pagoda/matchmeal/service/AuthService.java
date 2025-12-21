package com.pagoda.matchmeal.service;

import java.util.Map;

public interface AuthService {
    // 로그인 처리 (저장/업데이트 후 isNew 여부 반환)
    Map<String, Object> processLoginOrRegister(String socialId, String email, String name, String platform, String picture, String restartType);

    Map<String, String> reissueToken(String refreshToken);

    void logout(Long userId);
}
