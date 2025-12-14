package com.pagoda.matchmeal.mapper;

import com.pagoda.matchmeal.model.dto.ChallengeSearchCondition;
import com.pagoda.matchmeal.model.dto.response.ActiveChallengeDto;
import com.pagoda.matchmeal.model.dto.response.ChallengeResponseDto;
import com.pagoda.matchmeal.model.entity.Challenge;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ChallengeMapper {
    /* ============================================================
       [Section 1] 챌린지 생성 및 조회 (검색/상세)
       ============================================================ */

    /**
     * 챌린지 생성
     * useGeneratedKeys="true" 설정을 통해 생성된 challengeId가 객체에 담깁니다.
     */
    void insertChallenge(Challenge challenge);

    /**
     * 챌린지 단건 조회 (ID로 찾기)
     * - 참여 전 공개 여부나 인원 수 체크 등을 위해 필요
     */
    Challenge findById(Long challengeId);

    /**
     * 초대 코드로 챌린지 조회
     * - 비공개 챌린지 입장 시 사용
     */
    Challenge findByInvitationCode(String code);

    /**
     * 공개 챌린지 검색 (동적 쿼리)
     * @param cond 검색 조건 (타입, 날짜, 키워드) - XML에서 #{cond.xxx}로 접근
     */
    List<ChallengeResponseDto> searchPublicChallenges(@Param("cond") ChallengeSearchCondition cond);

    /**
     * 현재 참여 인원 수 조회
     * - 최대 인원(maxParticipants) 초과 체크용
     */
    int countParticipants(Long challengeId);


    /* ============================================================
       [Section 2] 유저 참여 및 관리 (초대/입장)
       ============================================================ */

    /**
     * 중복 참여 여부 확인
     * @return true: 이미 참여 중, false: 미참여
     */
    boolean existsByUserIdAndChallengeId(@Param("userId") Long userId, @Param("challengeId") Long challengeId);

    /**
     * 챌린지 참여하기 (UserChallenge 테이블 Insert)
     * - 방장 생성 시, 유저 입장 시 공통 사용
     */
    void insertUserChallenge(@Param("userId") Long userId, @Param("challengeId") Long challengeId);

    /**
     * 친구 초대하기 (Invitation 테이블 Insert)
     */
    void insertInvitation(@Param("challengeId") Long challengeId,
                          @Param("inviterId") Long inviterId,
                          @Param("inviteeId") Long inviteeId);


    /* ============================================================
       [Section 3] 진행 현황 및 진척도 (기존 로직)
       ============================================================ */

    /**
     * 내 전체 챌린지 목록 조회
     * - 마이페이지 등에서 내 현황(스트릭, 달성률) 확인할 때 사용
     */
    List<ChallengeResponseDto> findAllChallenges(@Param("userId") Long userId);

    /**
     * 현재 진행 중인 챌린지 조회 (식단 기록 시 체크용)
     * - ActiveChallengeDto 매핑
     */
    List<ActiveChallengeDto> findActiveChallengesByUserId(Long userId);

    /**
     * 진척도(카운트, 스트릭) 업데이트
     */
    void updateProgress(ActiveChallengeDto dto);

    /**
     * 챌린지 성공 처리
     */
    void updateStatusToSuccess(@Param("userChallengeId") Long userChallengeId);

    /**
     * 챌린지 실패 처리 (기간 만료 등)
     */
    void updateStatusToFail(@Param("userChallengeId") Long userChallengeId);
}
