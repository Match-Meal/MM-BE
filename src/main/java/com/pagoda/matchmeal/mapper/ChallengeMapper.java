package com.pagoda.matchmeal.mapper;

import com.pagoda.matchmeal.model.dto.ChallengeSearchCondition;
import com.pagoda.matchmeal.model.dto.response.ActiveChallengeDto;
import com.pagoda.matchmeal.model.dto.response.ChallengeInvitationResponseDto;
import com.pagoda.matchmeal.model.dto.response.ChallengeParticipantDto;
import com.pagoda.matchmeal.model.dto.response.ChallengeResponseDto;
import com.pagoda.matchmeal.model.entity.Challenge;
import com.pagoda.matchmeal.model.entity.ChallengeInvitation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
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
     * @param userId
     * @param cond 검색 조건 (타입, 날짜, 키워드) - XML에서 #{cond.xxx}로 접근
     */
    List<ChallengeResponseDto> searchPublicChallenges(@Param("userId") Long userId,
                                                      @Param("cond") ChallengeSearchCondition cond);

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

    boolean existsInvitation(@Param("challengeId") Long challengeId, @Param("inviteeId") Long inviteeId);

    /**
     * 챌린지 상세 조회
     * @param userId
     * @param challengeId
     */
    Optional<ChallengeResponseDto> findChallengeDetailById(@Param("userId") Long userId, @Param("challengeId") Long challengeId);

    /**
     * 챌린지 정보 수정
     * @param challenge
     */
    void updateChallenge(Challenge challenge);

    /**
     * 특정 챌린지의 참여자 목록 조회
     * @param challengeId
     * @return 참여자 정보를 담은 리스트 반환
     */
    List<ChallengeParticipantDto> findParticipantsByChallengeId(Long challengeId);

    /**
     * 챌린지 최대 인원
     * @param challengeId
     */
    void increaseHeadCount(Long challengeId);

    /**
     * 해당 챌린지의 모든 초대장 삭제
     * @param challengeId
     */
    void deleteInvitationsByChallengeId(Long challengeId);

    /**
     * 해당 챌린지의 모든 참여 기록 삭제
     * @param challengeId
     */
    void deleteUserChallengesByChallengeId(Long challengeId);

    /**
     * 챌린지 본체 삭제
     * @param challengeId
     */
    void deleteChallengeById(Long challengeId);

    /**
     * 유저 챌린지 기록 삭제 (나가기)
     * @param userId
     * @param challengeId
     */
    void deleteUserChallenge(@Param("userId") Long userId, @Param("challengeId") Long challengeId);

    /**
     * 챌린지 인원수 감소
     * @param challengeId
     */
    void decreaseHeadCount(Long challengeId);
    
    /**
     * 초대장 조회 (ID)
     * @param invitationId
     * @return 챌린지 초대 정보
     */
    ChallengeInvitation findInvitationById(Long invitationId);

    /**
     * 나에게 온 대기 중인 초대장 목록 조회
     */
    List<ChallengeInvitationResponseDto> findPendingInvitationsByUserId(Long userId);

    /**
     * 초대장 상태 업데이트
     * @param invitationId
     * @param status (ACCEPTED / REJECTED)
     */
    void updateInvitationStatus(@Param("invitationId") Long invitationId, @Param("status") String status);

    void updateStatusToProgress(Long userChallengeId);
}
