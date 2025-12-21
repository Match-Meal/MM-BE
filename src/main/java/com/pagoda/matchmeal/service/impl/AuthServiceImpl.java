package com.pagoda.matchmeal.service.impl;

import com.pagoda.matchmeal.common.config.jwt.JwtTokenProvider;
import com.pagoda.matchmeal.common.exception.CustomException;
import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import com.pagoda.matchmeal.mapper.UserMapper;
import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.model.entity.User;
import com.pagoda.matchmeal.model.enums.UserRole;
import com.pagoda.matchmeal.model.enums.UserStatus;
import com.pagoda.matchmeal.service.AuthService;
import com.pagoda.matchmeal.service.RedisService;
import com.pagoda.matchmeal.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 인증(Authentication) 비즈니스 로직 구현체
 * - 로그인/회원가입 분기 처리
 * - JWT 토큰 재발급 (Access/Refresh Token)
 * - 로그아웃 (Redis 토큰 삭제)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;
    private final UserMapper userMapper;
    private final RedisService redisService;

    /**
     * 로그인 또는 회원가입 처리 (UserService에서 이동됨)
     *
     * @param socialId    소셜 서비스(ID Provider)의 고유 식별자
     * @param email       사용자 이메일
     * @param name        사용자 이름 (닉네임)
     * @param platform    소셜 플랫폼 정보 (예: kakao, naver, google)
     * @param picture     프로필 이미지 URL
     * @param restartType 탈퇴 회원 처리 방식 ("RESTORE": 계정 복구, "RESET": 신규 가입으로 초기화, null: 일반 로그인)
     * @return 로그인/가입 처리된 User 객체와 신규 가입 여부(isNew)를 담은 Map
     */
    @Override
    @Transactional
    public Map<String, Object> processLoginOrRegister(String socialId, String email, String name, String platform, String picture, String restartType) {
        Map<String, Object> result = new HashMap<>();
        User user = userMapper.findBySocialId(socialId).orElse(null);
        boolean isNew = false;

        // 탈퇴 회원 처리 로직
        if (user != null && user.getDeletedAt() != null) {
            LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
            if (user.getDeletedAt().isBefore(threeMonthsAgo)) {
                // 3개월 지났으면 완전 삭제 후 신규 가입 유도
                userMapper.hardDeleteUserById(user.getUserId());
                user = null;
            } else {
                if (restartType == null) throw new CustomException(ErrorResponseCode.USER_WITHDRAWN_WAITING);

                if ("RESTORE".equals(restartType)) {
                    // 복구: 삭제일 제거 및 상태 활성화
                    userMapper.restoreUser(user.getUserId());
                    user.setDeletedAt(null);
                    user.setStatus(UserStatus.ACTIVE);
                } else if ("RESET".equals(restartType)) {
                    // 초기화: 기존 데이터 삭제 후 아래에서 신규 가입 진행
                    userMapper.hardDeleteUserById(user.getUserId());
                    user = null;
                }
            }
        }

        // 신규 가입
        if (user == null) {
            isNew = true;
            user = User.builder()
                    .socialId(socialId)
                    .email(email)
                    .userName(name)
                    .platform(platform)
                    .profileImage(picture)
                    .role(UserRole.ROLE_USER)
                    .status(UserStatus.ACTIVE)
                    .isPublic(true)
                    .build();
            userMapper.save(user);
        }

        result.put("user", user);
        result.put("isNew", isNew);
        return result;
    }

    /**
     * 토큰 재발급 (RTR 방식: Refresh Token도 함께 갱신)
     *
     * @param refreshToken 클라이언트가 보낸 Refresh Token
     * @return 새로 발급된 accessToken과 refreshToken이 담긴 Map
     */
    @Override
    @Transactional
    public Map<String, String> reissueToken(String refreshToken) {
        // Refresh Token 유효성 검사
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new CustomException(ErrorResponseCode.UNAUTHORIZED);
        }

        // 토큰에서 userId 추출
        String userIdStr = jwtTokenProvider.getSubject(refreshToken);
        Long userId = Long.valueOf(userIdStr);

        // redis에서 저장된 토큰 조회
        String redisKey = "RT:" + userId;
        String saveRefreshToken = redisService.getValues(redisKey);

        // redis 토큰 검증
        if (saveRefreshToken == null) {
            // redis에 없다는 것은 로그아웃 or 만료되어 사라진 상태
            throw new CustomException(ErrorResponseCode.UNAUTHORIZED);
        }

        if (!saveRefreshToken.equals(refreshToken)) {
            // redis에 있는 것과 다르면 토큰 탈취 의심 -> 보안상 정보 삭제
            redisService.deleteValues(redisKey);
            throw new CustomException(ErrorResponseCode.UNAUTHORIZED);
        }

        // 유저 db 조회 및 상태 확인
        User user = userMapper.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorResponseCode.USER_NOT_FOUND));

        // 탈퇴 대기 상태인 유저가 토큰을 재발급을 시도하면 차단
        if (user.getDeletedAt() != null) {
            redisService.deleteValues(redisKey);
            throw new CustomException(ErrorResponseCode.USER_WITHDRAWN_WAITING);
        }

        // 새 토큰 생성 (Access + Refresh)
        UserDto userDto = userService.convertUserToDto(user);
        String newAccessToken = jwtTokenProvider.createAccessToken(userDto);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);

        // redis 업데이트 (기존 키 덮어쓰기)
        long refreshTokenExpirationMillis = jwtTokenProvider.getRefreshTokenValidityInMilliseconds();
        redisService.setValues(redisKey, newRefreshToken, Duration.ofMillis(refreshTokenExpirationMillis));

        log.info("토큰 재발급 성공. UserID: {}", userId);

        Map<String, String> result = new HashMap<>();
        result.put("accessToken", newAccessToken);
        result.put("refreshToken", newRefreshToken);

        return result;
    }

    /**
     * 로그아웃 (Redis 삭제)
     *
     * @param userId 로그아웃할 사용자 PK
     */
    @Override
    public void logout(Long userId) {
        redisService.deleteValues("RT:" + userId);
        log.info("로그아웃 처리 완료. UserID: {}", userId);
    }
}