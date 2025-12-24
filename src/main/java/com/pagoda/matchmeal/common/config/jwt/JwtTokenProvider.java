package com.pagoda.matchmeal.common.config.jwt;

import com.pagoda.matchmeal.model.dto.UserDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT(Json Web Token) 생성, 검증 및 정보 추출을 담당하는 클래스
 * - Access/Refresh Token 생성
 * - 토큰 유효성 검사 및 Claims 추출
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenValidityInMilliseconds = 1000 * 60 * 30; // 30분

    @Getter
    private final long refreshTokenValidityInMilliseconds = 1000 * 60 * 60 * 24 * 7;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Access Token 생성
     * 유저의 상세 정보(Claims)를 포함하여 생성 (유효기간: 2시간)
     *
     * @param userDto 토큰에 담을 유저 정보
     * @return 생성된 JWT String
     */
    public String createAccessToken(UserDto userDto) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTokenValidityInMilliseconds);

        return Jwts.builder()
                .subject(userDto.getSocialId())
                .claim("id", userDto.getId())
                .claim("userName", userDto.getUserName())
                .claim("role", userDto.getRole())
                .claim("createdAt", userDto.getCreatedAt())
                .issuedAt(now)
                .expiration(validity)
                .signWith(key)
                .compact();
    }

    /**
     * Refresh Token 생성
     * 갱신을 위한 최소한의 정보(UserId)만 포함 (유효기간: 7일)
     *
     * @param userId 유저 PK
     * @return 생성된 JWT String
     */
    public String createRefreshToken(Long userId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshTokenValidityInMilliseconds);

        return Jwts.builder()
                .subject(String.valueOf(userId)) // Subject에 userId(PK)만 저장
                .issuedAt(now)
                .expiration(validity)
                .signWith(key)
                .compact();
    }

    /**
     * 토큰 유효성 검증
     * 서명(Signature) 검증 및 만료 여부 확인
     *
     * @param token 검증할 JWT
     * @return 유효하면 true, 그렇지 않으면 false
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 토큰에서 사용자 정보(UserDto) 추출
     * DB 조회 없이 토큰 Payload를 파싱하여 UserDto 객체로 복원
     *
     * @param token 파싱할 JWT
     * @return 복원된 UserDto 객체
     */
    public UserDto getUserDto(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // Role 확인
        String role = claims.get("role", String.class);

        // 임시 토큰(ROLE_WITHDRAWN)인 경우 필수값만 매핑하여 반환 (NPE 방지)
        if ("ROLE_WITHDRAWN".equals(role)) {
            return UserDto.builder()
                    .socialId(claims.getSubject())
                    .email(claims.get("email", String.class))
                    // .platform(claims.get("platform", String.class)) // DTO에 platform 필드가 있다면 추가
                    .role(role)
                    .build();
        }

        return UserDto.builder()
                .socialId(claims.getSubject())
                .id(claims.get("id", Long.class))
                .userName(claims.get("userName", String.class))
                .role(claims.get("role", String.class))
                .createdAt(claims.get("createdAt", String.class))
                .build();
    }

    /**
     * 임시 토큰 생성
     * 짧은 유효기간(5분)과 제한된 Role(ROLE_WITHDRAWN) 부여
     *
     * @param socialId 소셜 ID
     * @param email    이메일
     * @param platform 플랫폼 정보
     * @return 생성된 임시 JWT String
     */
    public String createTemporaryToken(String socialId, String email, String platform) {
        Date now = new Date();
        // 5분 설정
        Date validity = new Date(now.getTime() + (1000 * 60 * 5));

        return Jwts.builder()
                .subject(socialId)
                .claim("email", email)
                .claim("platform", platform)
                .claim("role", "ROLE_WITHDRAWN") // 탈퇴 대기자로 한정
                .issuedAt(now)
                .expiration(validity)
                .signWith(key)
                .compact();
    }

    /**
     * 토큰에서 Subject(UserId or SocialId) 추출 편의 메서드
     *
     * @param token JWT
     * @return 토큰의 Subject 문자열
     */
    public String getSubject(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
