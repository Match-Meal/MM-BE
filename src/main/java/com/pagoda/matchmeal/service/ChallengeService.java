package com.pagoda.matchmeal.service;

import com.pagoda.matchmeal.model.dto.ChallengeSearchCondition;
import com.pagoda.matchmeal.model.dto.request.ChallengeCreateRequestDto;
import com.pagoda.matchmeal.model.dto.response.ChallengeResponseDto;
import com.pagoda.matchmeal.model.entity.Diet;
import com.pagoda.matchmeal.model.entity.DietDetail;

import java.util.List;

public interface ChallengeService {
    /**
     * 챌린지 생성
     * @return 생성된 챌린지 ID
     */
    Long createChallenge(Long userId, ChallengeCreateRequestDto dto);

    /**
     * 공개 챌린지 검색 (타입, 기간, 키워드 등)
     */
    List<ChallengeResponseDto> searchChallenges(Long userId, ChallengeSearchCondition condition);

    /**
     * [참여 1] 공개 챌린지 목록에서 선택하여 참여
     * - 공개 여부(isPublic) 체크 후 참여
     */
    void joinPublicChallenge(Long userId, Long challengeId);

    /**
     * [참여 2] 초대/참여 코드로 챌린지 입장
     * - 비공개 방도 입장 가능
     */
    void joinByCode(Long userId, String code);

    /**
     * 친구 초대하기
     */
    void inviteUser(Long inviterId, Long challengeId, Long targetUserId);

    /**
     * 식단 기록 시 챌린지 진척도 자동 반영
     */
    void updateChallengeProgress(Long userId, Diet diet, List<DietDetail> detail);

    /**
     * 내 챌린지 전체 조회 (진척도, 스트릭 포함)
     */
    List<ChallengeResponseDto> getAllChallenges(Long userId);

    /**
     * 상세 조회
     */
    ChallengeResponseDto getChallengeDetail(Long userId, Long challengeId);

    /**
     * 챌린지 수정
     */
    void updateChallenge(Long userId, Long challengeId, ChallengeCreateRequestDto dto);

    /**
     * 챌린지 삭제
     */
    void deleteChallenge(Long userId, Long challengeId);
}
