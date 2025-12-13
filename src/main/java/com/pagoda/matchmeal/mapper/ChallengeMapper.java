package com.pagoda.matchmeal.mapper;

import com.pagoda.matchmeal.model.dto.response.ActiveChallengeDto;
import com.pagoda.matchmeal.model.dto.response.ChallengeResponseDto;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ChallengeMapper {
    /**
     * 전체 챌린지 목록 조회 (로그인 유저의 참여 정보 포함)
     * @param userId 로그인한 사용자 ID
     * @return 챌린지 목록 (달성률, 스트릭 정보 포함)
     */
    List<ChallengeResponseDto> findAllChallenges(@Param("userId") Long userId);

    /**
     * 내가 현재 진행 중인 챌린지 조회 (식단 기록 시 체크용)
     * @param userId 사용자 ID
     * @return 진행 중인 챌린지 리스트 (조건 정보 포함)
     */
    List<ActiveChallengeDto> findActiveChallengesByUserId(Long userId);

    /**
     * 중복 참여 확인
     * @param userId 사용자 ID
     * @param challengeId 챌린지 ID
     * @return 존재하면 true
     */
    boolean existsByUserIdAndChallengeId(@Param("userId") Long userId, @Param("challengeId") Long challengeId);

    /**
     * 챌린지 참여하기 (INSERT)
     * @param userId 사용자 ID
     * @param challengeId 챌린지 ID
     */
    void insertUserChallenge(@Param("userId") Long userId, @Param("challengeId") Long challengeId);

    /**
     * [진척도 업데이트]
     * Service에서 계산된 Streak, Count 정보를 DB에 반영합니다.
     * @param dto 계산된 결과가 담긴 DTO
     */
    void updateProgress(ActiveChallengeDto dto);

    /**
     * 챌린지 성공 처리 (Status -> SUCCESS)
     * @param userChallengeId 유저 챌린지 PK
     */
    void updateStatusToSuccess(@Param("userChallengeId") Long userChallengeId);

    /**
     * 챌린지 실패 처리 (Status -> FAIL, 기간 만료 시)
     * @param userChallengeId 유저 챌린지 PK
     */
    void updateStatusToFail(@Param("userChallengeId") Long userChallengeId);
}
