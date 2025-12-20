package com.pagoda.matchmeal.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialUnlinkService {

    private final RestTemplate restTemplate;

    public void unlink(String platform, String accessToken) {
        if ("google".equalsIgnoreCase(platform)) {
            unlinkGoogle(accessToken);
        }
        else if ("kakao".equalsIgnoreCase(platform)) {
            unlinkKakao(accessToken);
        }
    }

    // 1. 구글 연결 해제 (Revoke)
    private void unlinkGoogle(String accessToken) {
        String url = "https://oauth2.googleapis.com/revoke?token=" + accessToken;
        try {
            restTemplate.postForObject(url, null, String.class);
            log.info("구글 연결 해제 성공");
        } catch (Exception e) {
            log.error("구글 연결 해제 실패", e);
        }
    }

    // 2. 카카오 연결 해제 (Unlink)
    private void unlinkKakao(String accessToken) {
        String url = "https://kapi.kakao.com/v1/user/unlink";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        // "Bearer " 뒤에 공백 필수
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<String> entity = new HttpEntity<>(null, headers); // Body는 null이어도 됨

        try {
            restTemplate.postForObject(url, entity, String.class);
            log.info("카카오 연결 해제 성공");
        } catch (Exception e) {
            log.error("카카오 연결 해제 실패: {}", e.getMessage());
        }
    }
}
