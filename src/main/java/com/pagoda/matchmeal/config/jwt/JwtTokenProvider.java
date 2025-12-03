package com.pagoda.matchmeal.config.jwt;

import com.pagoda.matchmeal.model.dto.UserDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenValidityInMilliseconds = 1000 * 60 * 60 * 2; // 2시간

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

        return UserDto.builder()
                .socialId(claims.getSubject())
                .id(claims.get("id", Long.class))
                .userName(claims.get("userName", String.class))
                .role(claims.get("role", String.class))
                .createdAt(claims.get("createdAt", String.class))
                .build();
    }
}
