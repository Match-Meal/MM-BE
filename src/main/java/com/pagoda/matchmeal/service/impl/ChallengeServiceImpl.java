package com.pagoda.matchmeal.service.impl;

import com.pagoda.matchmeal.common.exception.CustomException;
import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import com.pagoda.matchmeal.mapper.ChallengeMapper;
import com.pagoda.matchmeal.mapper.DietMapper;
import com.pagoda.matchmeal.model.dto.ChallengeSearchCondition;
import com.pagoda.matchmeal.model.dto.request.ChallengeCreateRequestDto;
import com.pagoda.matchmeal.model.dto.response.*;
import com.pagoda.matchmeal.model.entity.Challenge;
import com.pagoda.matchmeal.model.entity.ChallengeInvitation;
import com.pagoda.matchmeal.model.entity.Diet;
import com.pagoda.matchmeal.model.entity.DietDetail;
import com.pagoda.matchmeal.model.enums.ChallengeType;
import com.pagoda.matchmeal.model.enums.MealType;
import com.pagoda.matchmeal.service.ChallengeService;
import com.pagoda.matchmeal.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ChallengeServiceImpl implements ChallengeService {

    private final ChallengeMapper challengeMapper;
    private final FollowService followService;
    private final DietMapper dietMapper;

    /**
     * 챌린지 생성
     * @param userId
     * @param dto
     * @return challengeId
     */
    @Override
    public Long createChallenge(Long userId, ChallengeCreateRequestDto dto) {
        // 초대 코드 생성 (난수 8자리)
        String inviteCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 엔티티 빌드
        Challenge challenge = Challenge.builder()
                .ownerId(userId)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .type(dto.getType())
                .targetValue(dto.getTargetValue())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .goalCount(dto.getGoalCount())
                .maxParticipants(dto.getMaxParticipants())
                .isPublic(dto.isPublic())
                .invitationCode(inviteCode)
                .currentHeadCount(1)
                .build();

        // 챌린지 저장
        challengeMapper.insertChallenge(challenge);

        // 방장도 참여자로 등록
        challengeMapper.insertUserChallenge(userId, challenge.getChallengeId());

        return challenge.getChallengeId();
    }

    /**
     * 챌린지 검색 (공개된 챌린지만 검색)
     * @param condition
     * @return List<ChallengeResponseDto>
     */
    @Override
    @Transactional(readOnly = true) // 읽기 전용
    public List<ChallengeResponseDto> searchChallenges(Long userId, ChallengeSearchCondition condition) {
        return challengeMapper.searchPublicChallenges(userId, condition);
    }

    /**
     * 공개 목록에서 선택해서 참여 (일반 참여)
     * @param userId
     * @param challengeId
     */
    @Override
    public void joinPublicChallenge(Long userId, Long challengeId) {
        Challenge challenge = challengeMapper.findById(challengeId);
        if (!challenge.isPublic()) {
            throw new CustomException(ErrorResponseCode.UNAUTHORIZED); // 비공개는 코드로만 입장 가능
        }
        joinChallengeLogic(userId, challenge);
    }

    // 공통 참여 로직
    private void joinChallengeLogic(Long userId, Challenge challenge) {
        // 중복 체크
        if (challengeMapper.existsByUserIdAndChallengeId(userId, challenge.getChallengeId())) {
            throw new CustomException(ErrorResponseCode.ALREADY_JOINED_CHALLENGE);
        }

        // 인원 제한 체크
        int currentCount = challengeMapper.countParticipants(challenge.getChallengeId());
        if (currentCount >= challenge.getMaxParticipants()) {
            throw new CustomException(ErrorResponseCode.CHALLENGE_FULL);
        }

        // 참여
        challengeMapper.insertUserChallenge(userId, challenge.getChallengeId());

        // 챌린지 테이블의 참여 인원수 증가
        challengeMapper.increaseHeadCount(challenge.getChallengeId());
    }

    /**
     * 비공개 챌린지 참여
     * @param userId
     * @param code
     */
    @Override
    public void joinByCode(Long userId, String code) {
        Challenge challenge = challengeMapper.findByInvitationCode(code);
        if (challenge == null) {
            throw new CustomException(ErrorResponseCode.CHALLENGE_NOT_FOUND);
        }

        // 참여 로직 호출
        joinChallengeLogic(userId, challenge);
    }

    /**
     * 팔로윙 목록에 있는 유저인지 확인 후 초대장 발송
     * @param inviterId
     * @param challengeId
     * @param targetUserId
     */
    @Override
    public void inviteUser(Long inviterId, Long challengeId, Long targetUserId) {
        // 권한 체크(방장만 가능한지, 참여자 모두 가능한지 정책 결정)
        if (!challengeMapper.existsByUserIdAndChallengeId(inviterId, challengeId)) {
            throw new CustomException(ErrorResponseCode.UNAUTHORIZED);
        }

        // 팔로우 관계 확인
        boolean isFollowing = followService.isFollowing(inviterId, targetUserId);
        if (!isFollowing) {
            throw new CustomException(ErrorResponseCode.NOT_FOLLOWING);
        }

        // 3. 이미 초대된 상태인지 혹은 이미 챌린지 멤버인지 중복 체크
        if (challengeMapper.existsByUserIdAndChallengeId(targetUserId, challengeId)) {
            throw new CustomException(ErrorResponseCode.ALREADY_JOINED_USER);
        }

        // 이미 대기 중인 초대장이 있는지?
        if (challengeMapper.existsInvitation(challengeId, targetUserId)) {
            throw new CustomException(ErrorResponseCode.ALREADY_INVITED);
        }

        // 초대장 DB 저장
        challengeMapper.insertInvitation(challengeId, inviterId, targetUserId);
    }

    /**
     * 식단 기록 시 챌린지 반영
     * @param userId
     * @param diet
     * @param details
     */
    @Override
    public void updateChallengeProgress(Long userId, Diet diet, List<DietDetail> details) {
        List<ActiveChallengeDto> activeChallenges = challengeMapper.findActiveChallengesByUserId(userId);
        if (activeChallenges.isEmpty()) return;

        LocalDate today = diet.getEatDate();

        // [1] 하루 누적 데이터 계산 (칼로리 챌린지용)
        // 방금 기록한 식단이 포함된 오늘의 모든 식단을 가져옵니다.
        List<DietResponseDto> todayDiets = dietMapper.findAllByDate(userId, today.toString());
        double dailyTotalCalories = todayDiets.stream()
                .mapToDouble(DietResponseDto::getTotalCalories)
                .sum();

        for (ActiveChallengeDto uc : activeChallenges) {
            // 기간 체크
            if (today.isBefore(uc.getStartDate()) || today.isAfter(uc.getEndDate())) {
                continue;
            }

            boolean isConditionMet = false;

            // [2] 챌린지 타입별 조건 체크
            switch (uc.getType()) {
                case RECORD_FREQUENCY:
                    // 기록형: 기록만 하면 무조건 성공
                    isConditionMet = true;
                    break;

                case CALORIE_LIMIT:
                    // 칼로리형: '하루 누적 칼로리'가 목표치 이하여야 함
                    if (dailyTotalCalories <= uc.getTargetValue()) {
                        isConditionMet = true;
                    }
                    break;

                case TIME_RANGE:
                    // 타임어택: 방금 먹은 식사의 시간이 목표 시간 이전이어야 함
                    // (주의: 하루에 여러 끼니 중 하나라도 시간 어기면 실패로 볼지, 성공한 끼니가 있으면 성공으로 볼지 정책 결정 필요)
                    // 현재 정책: 이번 식사가 시간을 지켰으면 성공으로 간주
                    if (diet.getEatTime().getHour() < uc.getTargetValue()) {
                        isConditionMet = true;
                    }
                    break;
            }

            // [3] 상태 업데이트 로직 (성공 처리 OR 성공 취소)
            boolean alreadySucceededToday = uc.getLastSuccessDate() != null && uc.getLastSuccessDate().equals(today);

            if (isConditionMet) {
                // 조건 만족 & 아직 오늘 성공 처리 안 됨 -> 성공 처리 (카운트 증가)
                if (!alreadySucceededToday) {
                    updateUserChallengeStatus(uc, today, true);
                }
                // 조건 만족 & 이미 성공 처리 됨 -> 유지 (아무것도 안 함)
            } else {
                // 조건 불만족 & 이미 오늘 성공 처리 됨 -> **성공 취소 (Rollback)**
                // 예: 아침에 적게 먹어서 성공했는데, 저녁에 폭식해서 누적 칼로리 초과한 경우
                if (alreadySucceededToday && uc.getType() == ChallengeType.CALORIE_LIMIT) {
                    updateUserChallengeStatus(uc, today, false);
                }
            }
        }
    }

    /**
     * 전체 챌린지 조회
     * @param userId
     * @return
     */
    @Override
    public List<ChallengeResponseDto> getAllChallenges(Long userId) {
        // DB
        List<ChallengeResponseDto> challenges = challengeMapper.findAllChallenges(userId);

        // 스트릭 유효성 보정
        // 유저가 어제 기록을 안했다면, DB 업데이트 전이라도 화면에는 연속 0일로 보여줌
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate today = LocalDate.now();

        for (ChallengeResponseDto dto: challenges) {
            // 참여중이지 않거나, 성공 기록이 아예 없으면 패스
            if(!dto.isJoined() || dto.getCurrentCount() == 0) continue;

            // 마지막 성공 날짜가 null이 아닌 경우 체크
            if (dto.getLastSuccessDate() != null) {
                boolean isStreakBroken = dto.getLastSuccessDate().isBefore(yesterday);

                if (isStreakBroken) {
                    dto.setCurrentStreak(0);
                }
            }

        }

        return challenges;
    }

    /**
     * 카운트/스트릭 증가 또는 감소(취소) 처리
     * @param isSuccess true: 성공 처리, false: 성공 취소
     */
    private void updateUserChallengeStatus(ActiveChallengeDto uc, LocalDate today, boolean isSuccess) {
        if (isSuccess) {
            // [성공 처리]
            int newCurrentCount = uc.getCurrentCount() + 1;
            int newCurrentStreak = 1;

            // 스트릭 계산
            if (uc.getLastSuccessDate() != null && uc.getLastSuccessDate().equals(today.minusDays(1))) {
                newCurrentStreak = uc.getCurrentStreak() + 1;
            }

            int newMaxStreak = Math.max(uc.getMaxStreak(), newCurrentStreak);

            uc.setCurrentCount(newCurrentCount);
            uc.setCurrentStreak(newCurrentStreak);
            uc.setMaxStreak(newMaxStreak);
            uc.setLastSuccessDate(today); // 오늘 날짜로 갱신

            // DB 업데이트
            challengeMapper.updateProgress(uc);

            // 목표 달성 체크
            if (newCurrentCount >= uc.getGoalCount()) {
                challengeMapper.updateStatusToSuccess(uc.getUserChallengeId());
            }

        } else {
            // [성공 취소 - Rollback]
            int newCurrentCount = Math.max(0, uc.getCurrentCount() - 1);
            int newCurrentStreak = Math.max(0, uc.getCurrentStreak() - 1);

            // 날짜 롤백 처리 (간소화: 스트릭이 0이 되면 날짜도 초기화, 아니면 유지)
            LocalDate rollbackDate = (newCurrentStreak > 0) ? uc.getLastSuccessDate() : null;
            // *참고: 더 정확히 하려면 '어제 날짜'를 계산해야 하지만, DTO에 없으므로 유지하거나 null 처리

            uc.setCurrentCount(newCurrentCount);
            uc.setCurrentStreak(newCurrentStreak);
            uc.setLastSuccessDate(rollbackDate);

            // 1. 수치 업데이트
            challengeMapper.updateProgress(uc);

            // 2. 상태 롤백 (SUCCESS -> PROGRESS)
            // 목표 횟수를 달성해서 'SUCCESS' 상태였는데,
            // 이번 취소로 인해 횟수가 부족해졌다면 다시 'PROGRESS'로 되돌림
            if (newCurrentCount < uc.getGoalCount()) {
                challengeMapper.updateStatusToProgress(uc.getUserChallengeId());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ChallengeResponseDto getChallengeDetail(Long userId, Long challengeId) {
        // 챌린지 기본 정보 + 내 진행 상황 조회
        ChallengeResponseDto response = challengeMapper.findChallengeDetailById(userId, challengeId)
                .orElseThrow(() -> new CustomException(ErrorResponseCode.CHALLENGE_NOT_FOUND));

        // 해당 챌린지의 참여자 목록 조회 및 세팅
        List<ChallengeParticipantDto> participants = challengeMapper.findParticipantsByChallengeId(challengeId);
        response.setParticipants(participants);

        // 리스트 화면과 싱크를 맞추기 위해 headCount도 다시 세팅
        response.setCurrentHeadCount(participants.size());
        return response;
    }

    @Override
    public void updateChallenge(Long userId, Long challengeId, ChallengeCreateRequestDto dto) {
        Challenge challenge = challengeMapper.findById(challengeId);
        if (challenge == null) {
            throw new CustomException(ErrorResponseCode.CHALLENGE_NOT_FOUND);
        }

        // 권한 체크
        if (!challenge.getOwnerId().equals(userId)) {
            throw new CustomException(ErrorResponseCode.UNAUTHORIZED);
        }

        // 데이터 업데이터
        challenge.setTitle(dto.getTitle());
        challenge.setDescription(dto.getDescription());
        challenge.setType(dto.getType());
        challenge.setTargetValue(dto.getTargetValue());
        challenge.setStartDate(dto.getStartDate());
        challenge.setEndDate(dto.getEndDate());
        challenge.setGoalCount(dto.getGoalCount());
        challenge.setMaxParticipants(dto.getMaxParticipants());
        challenge.setPublic(dto.isPublic());

        challengeMapper.updateChallenge(challenge);
    }

    @Override
    public void deleteChallenge(Long userId, Long challengeId) {
        Challenge challenge = challengeMapper.findById(challengeId);
        if (challenge == null) {
            throw new CustomException(ErrorResponseCode.CHALLENGE_NOT_FOUND);
        }

        // 권한 체크
        if (!challenge.getOwnerId().equals(userId)) {
            throw new CustomException(ErrorResponseCode.UNAUTHORIZED);
        }

        // 연관 데이터 삭제 (초대장 -> 참여중인 유저 -> 챌린지 순으로 삭제)
        challengeMapper.deleteInvitationsByChallengeId(challengeId);
        challengeMapper.deleteUserChallengesByChallengeId(challengeId);
        challengeMapper.deleteChallengeById(challengeId);
    }

    /**
     * 챌린지 떠나기
     * @param userId
     * @param challengeId
     */
    @Override
    public void leaveChallenge(Long userId, Long challengeId) {
        Challenge challenge = challengeMapper.findById(challengeId);
        if (challenge == null) {
            throw new CustomException(ErrorResponseCode.CHALLENGE_NOT_FOUND);
        }

        // 방장은 나가는것이 아닌 챌린지 삭제를 해야함
        if (challenge.getOwnerId().equals(userId)) {
            throw new CustomException(ErrorResponseCode.OWNER_CANNOT_LEAVE);
        }

        // 참여 여부 확인
        if (!challengeMapper.existsByUserIdAndChallengeId(userId, challengeId)) {
            throw new CustomException(ErrorResponseCode.NOT_JOINED_USER);
        }

        // 참여 기록 삭제
        challengeMapper.deleteUserChallenge(userId, challengeId);

        // 인원 수 감소
        challengeMapper.decreaseHeadCount(challengeId);
    }

    /**
     * 초대 응답 (승인/거절)
     * @param userId
     * @param invitationId
     * @param isAccepted
     */
    @Override
    public void respondInvitation(Long userId, Long invitationId, boolean isAccepted) {
        // 초대장 조회
        ChallengeInvitation invitation = challengeMapper.findInvitationById(invitationId);
        if (invitation == null) {
            throw new CustomException(ErrorResponseCode.INVITATION_NOT_FOUND);
        }

        // 본인 확인
        if (!invitation.getInviteeId().equals(userId)) {
            throw new CustomException(ErrorResponseCode.UNAUTHORIZED);
        }

        // 이미 처리된 초대장인지 확인
        if (!"PENDING".equals(invitation.getStatus())) {
            throw new CustomException(ErrorResponseCode.ALREADY_PROCESSED_INVITATION);
        }

        if (isAccepted) {
            // 승인
            Challenge challenge = challengeMapper.findById(invitation.getChallengeId());
            if (challenge == null) {
                // 삭제된 경우
                challengeMapper.updateInvitationStatus(invitationId,"EXPIRED");
                throw new CustomException(ErrorResponseCode.CHALLENGE_NOT_FOUND);
            }

            // 참여로직 호출
            try {
                joinChallengeLogic(userId, challenge);
                // 상태 업데이트
                challengeMapper.updateInvitationStatus(invitationId, "ACCEPTED");
            } catch (CustomException e) {
                // 실패 처리
                throw e;
            }
        } else {
            // 거절
            challengeMapper.updateInvitationStatus(invitationId, "REJECTED");
        }
    }

    /**
     * 나에게 온 초대장 목록 조회
     */
    @Override
    @Transactional(readOnly = true)
    public List<ChallengeInvitationResponseDto> getMyInvitations(Long userId) {
        return challengeMapper.findPendingInvitationsByUserId(userId);
    }
}
