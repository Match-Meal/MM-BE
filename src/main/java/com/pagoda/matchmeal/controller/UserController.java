package com.pagoda.matchmeal.controller;

import com.pagoda.matchmeal.common.config.jwt.JwtTokenProvider;
import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import com.pagoda.matchmeal.common.response.CommonResponse;
import com.pagoda.matchmeal.common.util.ApiResponseUtil;
import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.model.dto.UserProfileDto;
import com.pagoda.matchmeal.model.entity.User;
import com.pagoda.matchmeal.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final OAuth2AuthorizedClientService authorizedClientService;

    /**
     * 내 정보 조회
     *
     * @param tokenUser
     * @return UserDto
     */
    @GetMapping("/me")
    public CommonResponse<UserDto> getMyInfo(@AuthenticationPrincipal UserDto tokenUser) {
        if (tokenUser == null) {
            return ApiResponseUtil.failure(ErrorResponseCode.UNAUTHORIZED);
        }

        UserDto myProfile = userService.getMyProfile(tokenUser.getId());

        return ApiResponseUtil.success(myProfile);
    }

    @GetMapping("/{userId}")
    public CommonResponse<UserDto> getUserProfile(@PathVariable Long userId) {
        UserDto userProfile = userService.getUserProfile(userId);
        return ApiResponseUtil.success(userProfile);
    }

    /**
     * 프로필 업데이트
     * Content-Type: multipart/form-data
     *
     * @param userDto
     * @param profileDto
     * @param file
     * @return
     */
    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResponse<Void> updateProfile(
            @AuthenticationPrincipal UserDto userDto,
            @RequestPart(value = "data") UserProfileDto profileDto,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        userService.updateProfile(userDto.getId(), profileDto, file);
        return ApiResponseUtil.success();
    }

    /**
     * 프로필 공개 여부 설정
     *
     * @param userDto
     * @param request
     * @return
     */
    @PatchMapping("/visibility")
    public CommonResponse<Void> updateVisibility(
            @AuthenticationPrincipal UserDto userDto,
            @RequestBody Map<String, Boolean> request
    ) {
        boolean isPublic = request.get("isPublic");
        userService.updateVisibility(userDto.getId(), isPublic);
        return ApiResponseUtil.success();
    }

    /**
     * 탈퇴 회원 복구 또는 재가입 (임시 토큰 필요)
     * Header: Authorization: Bearer {tempToken}
     */
    @PostMapping("/reactivate")
    public CommonResponse<Map<String, Object>> reactivateUser(
            @AuthenticationPrincipal UserDto tempUser, // 임시 토큰에서 파싱된 정보
            @RequestBody Map<String, String> request // { "decision": "RESTORE" or "RESET" }
    ) {
        // 1. 보안 검증: 이 토큰이 진짜 ROLE_WITHDRAWN 인지 확인
        if (!"ROLE_WITHDRAWN".equals(tempUser.getRole())) {
            return ApiResponseUtil.failure(ErrorResponseCode.UNAUTHORIZED);
        }

        String decision = request.get("decision"); // "RESTORE" or "RESET"
        String socialId = tempUser.getSocialId(); // 토큰 subject에 있음

        // 2. 서비스 호출 (기존 processLoginOrRegister 재활용)
        // socialId는 토큰에서 꺼냈으므로 안전함. 나머지는 null로 넘겨도 DB 조회로 처리됨.
        Map<String, Object> result = userService.processLoginOrRegister(
                socialId,
                null, null, null, null, // 이미 DB에 있거나 토큰에 정보가 있으므로 생략 가능
                decision
        );

        // 3. 정식 Access Token 발급 (이제 ROLE_USER)
        User user = (User) result.get("user");

        // DTO 변환 및 정식 토큰 생성 로직 (OAuth2SuccessHandler와 유사)
        UserDto userDto = userService.convertUserToDto(user); // DTO 변환 메서드 필요
        String newAccessToken = jwtTokenProvider.createAccessToken(userDto);

        // 결과 반환
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("accessToken", newAccessToken);
        responseMap.put("user", userDto);

        return ApiResponseUtil.success(responseMap);
    }

    @DeleteMapping("/withdraw")
    public CommonResponse<Void> withdrawUser(
            @AuthenticationPrincipal UserDto userDto,
            OAuth2AuthenticationToken authentication // 현재 인증 정보(소셜 정보 포함)
    ) {
        String socialAccessToken = null;

        // 1. 소셜 로그인 사용자라면 토큰 추출
        if (authentication != null) {
            // "google", "kakao" 등의 registrationId
            String registrationId = authentication.getAuthorizedClientRegistrationId();

            // 메모리(또는 DB)에 저장된 클라이언트 정보 로드
            OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                    registrationId,
                    authentication.getName()
            );

            if (client != null) {
                socialAccessToken = client.getAccessToken().getTokenValue();
            } else {
                // ★ 로그 추가 권장
                log.info("소셜 Access Token을 찾을 수 없어 플랫폼 연결 해제는 건너뜁니다. (User ID: {})", userDto.getId());
            }
        }

        // 2. 서비스로 토큰과 함께 전달
        userService.withdrawUser(userDto.getId(), socialAccessToken);

        return ApiResponseUtil.success();
    }

}
