package com.pagoda.matchmeal.model.dto.response;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class KakaoReadyResponse {
    private String tid;                  // 결제 고유 번호
    private String next_redirect_app_url;  // 모바일 앱용 리다이렉트 URL
    private String next_redirect_mobile_url; // 모바일 웹용 리다이렉트 URL
    private String next_redirect_pc_url;     // PC 웹용 리다이렉트 URL
    private String android_app_scheme;
    private String ios_app_scheme;
    private LocalDateTime created_at;      // 결제 준비 요청 시간
}