package com.pagoda.matchmeal.mapper;

import com.pagoda.matchmeal.model.dto.response.ActiveChallengeDto;
import com.pagoda.matchmeal.model.dto.response.ChallengeResponseDto;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ChallengeMapper {
    // 전체 챌린지 목록 조회 (로그인 유저의 참여 정보 포함)
    List<ChallengeResponseDto> findAllChallenges(@Param("userId") Long userId);

    // 유저가 진행중인 챌린지 목록 조회 (업데이트용)
    List<ActiveChallengeDto> findActiveChallengesByUserId(Long userId);

    // 챌린지 참여
    void insertUserChallenge(@Param("userId") Long userId, @Param("challengeId") Long challengeId);

    // 진척도 업데이트
    void updateProgress(@Param("userChallengeId") Long userChallengeId);

    // 성공 상태 변경
    void updateStatusToSuccess(@Param("userChallengeId") Long userChallengeId);


}
