package com.pagoda.matchmeal.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialUnlinkService {

    private final RestTemplate restTemplate;

    // application.yml에 kakao.admin-key 설정이 있어야 합니다.
    @Value("${spring.security.oauth2.client.registration.kakao.admin-key:}")
    private String kakaoAdminKey;

    /**
     * @param platform  소셜 플랫폼 (google, kakao)
     * @param socialId  식별자 (sub, id) - AccessToken 아님!
     */
    public void unlink(String platform, String socialId) {
        if ("google".equalsIgnoreCase(platform)) {
            // 구글은 Access Token 없이 서버에서 연결 해제 불가능
            log.info("구글 계정은 서버 측 연결 해제를 지원하지 않습니다. (DB 탈퇴만 진행)");
        }
        else if ("kakao".equalsIgnoreCase(platform)) {
            unlinkKakao(socialId);
        }
    }

    // 2. 카카오 연결 해제 (Admin Key 사용)
    private void unlinkKakao(String socialId) {
        // 유저 토큰 대신 '어드민 키'를 사용하여 강제 해제
        String url = "https://kapi.kakao.com/v1/user/unlink";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // ★ 핵심: Authorization에 'KakaoAK' + 어드민 키 사용
        if (kakaoAdminKey == null || kakaoAdminKey.isBlank()) {
            log.warn("카카오 Admin Key가 설정되지 않아 연결 해제를 건너뜁니다.");
            return;
        }
        headers.set("Authorization", "KakaoAK " + kakaoAdminKey);

        // Body에 target_id_type, target_id 파라미터 전달
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("target_id_type", "user_id");
        body.add("target_id", socialId);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForObject(url, entity, String.class);
            log.info("카카오 연결 해제 성공 (Admin Key) - socialId: {}", socialId);
        } catch (Exception e) {
            log.error("카카오 연결 해제 실패: {}", e.getMessage());
            // 여기서 예외를 던지지 않아야 DB 탈퇴(Soft Delete)까지 무사히 진행됨
        }
    }
}