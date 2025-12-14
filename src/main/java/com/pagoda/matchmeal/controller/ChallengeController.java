package com.pagoda.matchmeal.controller;

import com.pagoda.matchmeal.common.response.CommonResponse;
import com.pagoda.matchmeal.common.util.ApiResponseUtil;
import com.pagoda.matchmeal.model.dto.ChallengeSearchCondition;
import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.model.dto.request.ChallengeCreateRequestDto;
import com.pagoda.matchmeal.model.dto.response.ChallengeResponseDto;
import com.pagoda.matchmeal.service.ChallengeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/challenge")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;

    /**
     * 챌린지 생성
     * @param user
     * @param dto
     * @return 성공 시 HTTP 201 Created와 생성된 challengeId 반환
     */
    @PostMapping
    public ResponseEntity<CommonResponse<Long>> createChallenge(
            @AuthenticationPrincipal UserDto user,
            @RequestBody ChallengeCreateRequestDto dto) {

        Long challengeId = challengeService.createChallenge(user.getId(), dto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseUtil.success(challengeId));
    }

    /**
     * 공개 챌린지 검색
     * @param condition (type, date 등을 담은 객체)
     * @return 필터링된 공개 챌린지 List 반환
     */
    @GetMapping("/search")
    public ResponseEntity<CommonResponse<List<ChallengeResponseDto>>> searchChallenges(
            @ModelAttribute ChallengeSearchCondition condition) {

        return ResponseEntity.ok(
                ApiResponseUtil.success(challengeService.searchChallenges(condition))
        );
    }

    /**
     * 전체 챌린지 조회
     * @param user
     * @return 로그인한 유저의 참여 목록, 진척도, 스트릭 정보 반환
     */
    @GetMapping
    public ResponseEntity<CommonResponse<List<ChallengeResponseDto>>> getAllChallenges(
            @AuthenticationPrincipal UserDto user) {
        return ResponseEntity.ok(
                ApiResponseUtil.success(challengeService.getAllChallenges(user.getId()))
        );
    }

    /**
     * 공개 챌린지 참여
     * @param user
     * @param challengeId
     */
    @PostMapping("/{challengeId}/join")
    public ResponseEntity<CommonResponse<Void>> joinPublicChallenge(
            @AuthenticationPrincipal UserDto user,
            @PathVariable Long challengeId) {

        challengeService.joinPublicChallenge(user.getId(), challengeId);
        return ResponseEntity.ok(ApiResponseUtil.success());
    }

    /**
     * 비공개 챌린지 참여
     * @param user
     * @param code
     */
    @PostMapping("/join/code")
    public ResponseEntity<CommonResponse<Void>> joinByCode(
            @AuthenticationPrincipal UserDto user,
            @RequestParam String code) {

        challengeService.joinByCode(user.getId(), code);
        return ResponseEntity.ok(ApiResponseUtil.success());
    }

    /**
     * 팔로잉 유저를 챌린지에 초대
     * @param user
     * @param challengeId
     * @param targetUserId
     */
    @PostMapping("/{challengeId}/invite")
    public ResponseEntity<CommonResponse<Void>> inviteUser(
            @AuthenticationPrincipal UserDto user,
            @PathVariable Long challengeId,
            @RequestBody Long targetUserId) {

        challengeService.inviteUser(user.getId(), challengeId, targetUserId);
        return ResponseEntity.ok(ApiResponseUtil.success());
    }
}
