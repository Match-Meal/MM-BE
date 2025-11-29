package com.pagoda.matchmeal.config.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final Key key;
    private final long accessTokenValidityInMilliseconds = 1000 * 60 * 60 * 2; // 2시간

    public JwtTokenProvider(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    // access token 생성
    public String createAccessToken(String socialId, String role) {
        Claims claims = (Claims) Jwts.claims().subject(socialId);
        claims.put("role", role);

        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTokenValidityInMilliseconds);

        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }




}
