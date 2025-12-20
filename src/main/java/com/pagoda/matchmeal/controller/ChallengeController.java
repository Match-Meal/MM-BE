package com.pagoda.matchmeal.controller;

import com.pagoda.matchmeal.common.response.CommonResponse;
import com.pagoda.matchmeal.common.util.ApiResponseUtil;
import com.pagoda.matchmeal.model.dto.ChallengeSearchCondition;
import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.model.dto.request.ChallengeCreateRequestDto;
import com.pagoda.matchmeal.model.dto.request.ChallengeInviteRequestDto;
import com.pagoda.matchmeal.model.dto.response.ChallengeResponseDto;
import com.pagoda.matchmeal.service.ChallengeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
     * [변경] ResponseEntity 제거 -> CommonResponse 직접 반환
     * [추가] @ResponseStatus(HttpStatus.CREATED) -> HTTP Header를 201로 설정
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<Long> createChallenge(
            @AuthenticationPrincipal UserDto user,
            @RequestBody ChallengeCreateRequestDto dto) {

        Long challengeId = challengeService.createChallenge(user.getId(), dto);

        // ApiResponseUtil.created()를 바로 반환
        return ApiResponseUtil.created(challengeId);
    }

    /**
     * 공개 챌린지 검색
     * (기본 상태코드가 200 OK이므로 별도 어노테이션 불필요)
     */
    @GetMapping("/search")
    public CommonResponse<List<ChallengeResponseDto>> searchChallenges(
            @ModelAttribute ChallengeSearchCondition condition,
            @AuthenticationPrincipal UserDto user) {

        Long userId = (user != null) ? user.getId() : null;

        return ApiResponseUtil.success(challengeService.searchChallenges(userId, condition));
    }

    /**
     * 전체 챌린지 조회
     */
    @GetMapping
    public CommonResponse<List<ChallengeResponseDto>> getAllChallenges(
            @AuthenticationPrincipal UserDto user) {

        return ApiResponseUtil.success(challengeService.getAllChallenges(user.getId()));
    }

    /**
     * 공개 챌린지 참여
     */
    @PostMapping("/{challengeId}/join")
    public CommonResponse<Void> joinPublicChallenge(
            @AuthenticationPrincipal UserDto user,
            @PathVariable Long challengeId) {

        challengeService.joinPublicChallenge(user.getId(), challengeId);

        return ApiResponseUtil.success();
    }

    /**
     * 비공개 챌린지 참여
     */
    @PostMapping("/join/code")
    public CommonResponse<Void> joinByCode(
            @AuthenticationPrincipal UserDto user,
            @RequestParam String code) {

        challengeService.joinByCode(user.getId(), code);

        return ApiResponseUtil.success();
    }

    /**
     * 팔로잉 유저를 챌린지에 초대
     */
    @PostMapping("/{challengeId}/invite")
    public CommonResponse<Void> inviteUser(
            @AuthenticationPrincipal UserDto user,
            @PathVariable Long challengeId,
            @RequestBody ChallengeInviteRequestDto request) {

        challengeService.inviteUser(user.getId(), challengeId, request.getTargetUserId());

        return ApiResponseUtil.success();
    }

    /**
     * 챌린지 상세 조회
     */
    @GetMapping("/{challengeId}")
    public CommonResponse<ChallengeResponseDto> getChallengeDetail(
            @AuthenticationPrincipal UserDto user,
            @PathVariable Long challengeId) {

        Long userId = (user != null) ? user.getId() : null;

        return ApiResponseUtil.success(challengeService.getChallengeDetail(userId, challengeId));
    }

    /**
     * 챌린지 수정
     */
    @PutMapping("/{challengeId}")
    public CommonResponse<Void> updateChallenge(
            @AuthenticationPrincipal UserDto user,
            @PathVariable Long challengeId,
            @RequestBody ChallengeCreateRequestDto dto) {

        challengeService.updateChallenge(user.getId(), challengeId, dto);

        return ApiResponseUtil.success();
    }

    /**
     * 챌린지 삭제
     */
    @DeleteMapping("/{challengeId}")
    public CommonResponse<Void> deleteChallenge(
            @AuthenticationPrincipal UserDto user,
            @PathVariable Long challengeId) {

        challengeService.deleteChallenge(user.getId(), challengeId);

        return ApiResponseUtil.success();
    }
}