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

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenValidityInMilliseconds = 1000 * 60 * 60 * 2; // 2시간

    @Getter
    private final long refreshTokenValidityInMilliseconds = 1000 * 60 * 60 * 24 * 7;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // access token 생성
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

    // refresh token 생성
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

    // 토큰 유효성 검증
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
     * 수정: DB 조회 없이 토큰의 Payload를 읽어 UserDto 객체를 복원
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

    // 임시 토큰 생성(5분)
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

    // 6. (편의 메서드) 토큰에서 Subject(UserId or SocialId) 추출
    public String getSubject(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
