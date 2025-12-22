package com.pagoda.matchmeal.controller;

import com.pagoda.matchmeal.common.response.CommonResponse;
import com.pagoda.matchmeal.common.util.ApiResponseUtil;
import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.model.dto.response.KakaoReadyResponse;
import com.pagoda.matchmeal.service.KakaoPayService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class KakaoPayController {
    private final KakaoPayService kakaoPayService;

    // 1. 결제 준비 요청 (Ready)
    @PostMapping("/ready")
    public KakaoReadyResponse ready(@AuthenticationPrincipal UserDto userDto) {
        return kakaoPayService.ready(userDto.getId());
    }

    @GetMapping("/success")
    public CommonResponse<String> success(@RequestParam("pg_token") String pgToken,
                                          @RequestParam("userId") Long userId) {
        // pgToken을 서비스로 넘겨서 카카오 승인 API를 호출하고 SID를 발급받음
        kakaoPayService.approveFirstPayment(pgToken, userId);
        return ApiResponseUtil.success("구독이 완료되었습니다!");
    }

    @GetMapping("/my-subscription")
    public CommonResponse<com.pagoda.matchmeal.model.dto.response.SubscriptionResponseDto> getMySubscription(@AuthenticationPrincipal UserDto userDto) {
        return ApiResponseUtil.success(kakaoPayService.getMySubscription(userDto.getId()));
    }

    // 3. 구독 해지 요청
    @PostMapping("/cancel")
    public CommonResponse<String> cancel(@AuthenticationPrincipal UserDto userDto) {
        kakaoPayService.cancelSubscription(userDto.getId());
        return ApiResponseUtil.success("정기 구독이 성공적으로 해지되었습니다.");
    }

    // 4. 구독 재활성화 요청
    @PostMapping("/reactivate")
    public CommonResponse<String> reactivate(@AuthenticationPrincipal UserDto userDto) {
        kakaoPayService.reactivateSubscription(userDto.getId());
        return ApiResponseUtil.success("구독이 다시 활성화되었습니다. 예정된 날짜에 결제됩니다.");
    }
}