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
     */
    @Override
    @Transactional
    public Map<String, Object> processLoginOrRegister(String socialId, String email, String name, String platform, String picture, String restartType) {
        Map<String, Object> result = new HashMap<>();
        User user = userMapper.findBySocialId(socialId).orElse(null);
        boolean isNew = false;

        // 탈퇴 회원 처리 로직 (기존과 동일)
        if (user != null && user.getDeletedAt() != null) {
            LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
            if (user.getDeletedAt().isBefore(threeMonthsAgo)) {
                userMapper.hardDeleteUserById(user.getUserId());
                user = null;
            } else {
                if (restartType == null) throw new CustomException(ErrorResponseCode.USER_WITHDRAWN_WAITING);

                if ("RESTORE".equals(restartType)) {
                    userMapper.restoreUser(user.getUserId());
                    user.setDeletedAt(null);
                    user.setStatus(UserStatus.ACTIVE);
                } else if ("RESET".equals(restartType)) {
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

        // ★ 여기서 토큰까지 만들어서 리턴해주는 게 더 깔끔할 수도 있지만,
        // 기존 구조(핸들러에서 처리)를 유지한다면 user 객체만 넘깁니다.
        result.put("user", user);
        result.put("isNew", isNew);
        return result;
    }

    /**
     * 토큰 재발급 (RTR 방식: Refresh Token도 함께 갱신)
     */
    @Override
    @Transactional
    public Map<String, String> reissueToken(String refreshToken) {
        // Refresh Token 유효성 검사(만료 여부, 서명 위조 여부)
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw  new CustomException(ErrorResponseCode.UNAUTHORIZED);
        }

        // 토큰에서 userId 추출
        // createRefreshToken에서 subject에 userId를 넣었기 때문에 그대로 꺼냄
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
            // redis에 있는 것과 다르면 토큰 탈취 의심 -> 보안상 해당 유저의 redis 정보를 날려버림
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
     */
    @Override
    public void logout(Long userId) {
        redisService.deleteValues("RT:" + userId);
        log.info("로그아웃 처리 완료. UserID: {}", userId);
    }
}
