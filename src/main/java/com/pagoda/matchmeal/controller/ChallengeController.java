package com.pagoda.matchmeal.controller;

import com.pagoda.matchmeal.common.response.CommonResponse;
import com.pagoda.matchmeal.common.util.ApiResponseUtil;
import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.model.dto.response.ChallengeResponseDto;
import com.pagoda.matchmeal.service.ChallengeService;
import lombok.RequiredArgsConstructor;
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
     * 전체 챌린지 조회
     * @param user
     * @return
     */
    @GetMapping
    public ResponseEntity<CommonResponse<List<ChallengeResponseDto>>> getChallenges(
            @AuthenticationPrincipal UserDto user) {
        return ResponseEntity.ok(
                ApiResponseUtil.success(challengeService.getAllChallenges(user.getId()))
        );
    }

    @PostMapping("/{challengeId}/join")
    public ResponseEntity<CommonResponse<Void>> joinChallenge(
            @AuthenticationPrincipal UserDto user,
            @PathVariable Long challengeId) {

        challengeService.joinChallenge(user.getId(), challengeId);
        return ResponseEntity.ok(ApiResponseUtil.success());
    }




}
