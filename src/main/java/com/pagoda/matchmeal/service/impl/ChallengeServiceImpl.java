package com.pagoda.matchmeal.service.impl;

import com.pagoda.matchmeal.common.exception.CustomException;
import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import com.pagoda.matchmeal.mapper.ChallengeMapper;
import com.pagoda.matchmeal.mapper.DietMapper;
import com.pagoda.matchmeal.mapper.UserMapper;
import com.pagoda.matchmeal.model.dto.ChallengeSearchCondition;
import com.pagoda.matchmeal.model.dto.request.ChallengeCreateRequestDto;
import com.pagoda.matchmeal.model.dto.response.*;
import com.pagoda.matchmeal.model.entity.Challenge;
import com.pagoda.matchmeal.model.entity.ChallengeInvitation;
import com.pagoda.matchmeal.model.entity.Diet;
import com.pagoda.matchmeal.model.entity.DietDetail;
import com.pagoda.matchmeal.model.enums.ChallengeType;
import com.pagoda.matchmeal.model.enums.NotificationType;
import com.pagoda.matchmeal.service.ChallengeService;
import com.pagoda.matchmeal.service.FollowService;
import com.pagoda.matchmeal.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 챌린지 비즈니스 로직 구현체
 * - 챌린지 생성, 검색, 참여, 초대
 * - 식단 기록에 따른 챌린지 성공 여부 판단 및 스트릭 업데이트
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChallengeServiceImpl implements ChallengeService {

    private final ChallengeMapper challengeMapper;
    private final FollowService followService;
    private final DietMapper dietMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    /**
     * 챌린지 생성
     *
     * @param userId
     * @param dto
     * @return challengeId
     */
    @Override
    @Transactional
    public Long createChallenge(Long userId, ChallengeCreateRequestDto dto) {
        String inviteCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

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

        challengeMapper.insertChallenge(challenge);
        challengeMapper.insertUserChallenge(userId, challenge.getChallengeId());

        return challenge.getChallengeId();
    }

    /**
     * 챌린지 검색 (공개된 챌린지만 검색)
     *
     * @param condition
     * @return List<ChallengeResponseDto>
     */
    @Override
    public List<ChallengeResponseDto> searchChallenges(Long userId, ChallengeSearchCondition condition) {
        return challengeMapper.searchPublicChallenges(userId, condition);
    }

    /**
     * 공개 챌린지 일반 참여
     *
     * @param userId      참여할 유저 PK
     * @param challengeId 챌린지 PK
     */
    @Override
    @Transactional
    public void joinPublicChallenge(Long userId, Long challengeId) {
        Challenge challenge = challengeMapper.findById(challengeId);
        if (!challenge.isPublic()) {
            throw new CustomException(ErrorResponseCode.UNAUTHORIZED); // 비공개는 코드로만 입장 가능
        }
        joinChallengeLogic(userId, challenge);
    }

    // 공통 참여 로직 (중복 체크, 인원 제한 체크)
    private void joinChallengeLogic(Long userId, Challenge challenge) {
        if (challengeMapper.existsByUserIdAndChallengeId(userId, challenge.getChallengeId())) {
            throw new CustomException(ErrorResponseCode.ALREADY_JOINED_CHALLENGE);
        }
        int currentCount = challengeMapper.countParticipants(challenge.getChallengeId());
        if (currentCount >= challenge.getMaxParticipants()) {
            throw new CustomException(ErrorResponseCode.CHALLENGE_FULL);
        }
        challengeMapper.insertUserChallenge(userId, challenge.getChallengeId());
        challengeMapper.increaseHeadCount(challenge.getChallengeId());
    }

    /**
     * 비공개 챌린지 참여
     *
     * @param userId
     * @param code
     */
    @Override
    @Transactional
    public void joinByCode(Long userId, String code) {
        Challenge challenge = challengeMapper.findByInvitationCode(code);
        if (challenge == null) {
            throw new CustomException(ErrorResponseCode.CHALLENGE_NOT_FOUND);
        }
        joinChallengeLogic(userId, challenge);
    }

    /**
     * 팔로윙 목록에 있는 유저인지 확인 후 초대장 발송
     *
     * @param inviterId
     * @param challengeId
     * @param targetUserId
     */
    @Override
    @Transactional
    public void inviteUser(Long inviterId, Long challengeId, Long targetUserId) {
        if (!challengeMapper.existsByUserIdAndChallengeId(inviterId, challengeId)) {
            throw new CustomException(ErrorResponseCode.UNAUTHORIZED);
        }
        boolean isFollowing = followService.isFollowing(inviterId, targetUserId);
        if (!isFollowing) {
            throw new CustomException(ErrorResponseCode.NOT_FOLLOWING);
        }
        if (challengeMapper.existsByUserIdAndChallengeId(targetUserId, challengeId)) {
            throw new CustomException(ErrorResponseCode.ALREADY_JOINED_USER);
        }
        if (challengeMapper.existsInvitation(challengeId, targetUserId)) {
            throw new CustomException(ErrorResponseCode.ALREADY_INVITED);
        }
        challengeMapper.insertInvitation(challengeId, inviterId, targetUserId);

        String inviterName = userMapper.findById(inviterId).orElseThrow(() -> new CustomException(ErrorResponseCode.USER_NOT_FOUND)).getUserName();

        String challengeTitle = challengeMapper.findById(challengeId).getTitle();

        notificationService.sendToUser(
                targetUserId,
                inviterId,
                NotificationType.CHALLENGE_INVITE,
                inviterName + "님이 [" + challengeTitle + "] 챌린지에 초대했습니다.",
                challengeId.intValue(),
                "/challenges/" + challengeId
        );
    }

    /**
     * 식단 기록 시 챌린지 반영
     *
     * @param userId
     * @param diet
     * @param details
     */
    @Override
    @Transactional
    public void updateChallengeProgress(Long userId, Diet diet, List<DietDetail> details) {
        List<ActiveChallengeDto> activeChallenges = challengeMapper.findActiveChallengesByUserId(userId);
        if (activeChallenges.isEmpty()) return;

        LocalDate today = diet.getEatDate();
        List<DietResponseDto> todayDiets = dietMapper.findAllByDate(userId, today.toString());
        double dailyTotalCalories = todayDiets.stream().mapToDouble(DietResponseDto::getTotalCalories).sum();

        for (ActiveChallengeDto uc : activeChallenges) {
            if (today.isBefore(uc.getStartDate()) || today.isAfter(uc.getEndDate())) continue;

            boolean isConditionMet = false;

            switch (uc.getType()) {
                case RECORD_FREQUENCY: // 기록형: 기록만 하면 성공
                    isConditionMet = true;
                    break;
                case CALORIE_LIMIT: // 칼로리형: 누적 칼로리가 목표치 이하
                    if (dailyTotalCalories <= uc.getTargetValue()) isConditionMet = true;
                    break;
                case TIME_RANGE: // 타임어택: 식사 시간이 목표 시간 이전
                    if (diet.getEatTime().getHour() < uc.getTargetValue()) isConditionMet = true;
                    break;
            }

            boolean alreadySucceededToday = uc.getLastSuccessDate() != null && uc.getLastSuccessDate().equals(today);

            if (isConditionMet) {
                if (!alreadySucceededToday) {
                    updateUserChallengeStatus(uc, today, true); // 성공 처리
                }
            } else {
                // 이전에 성공했으나 이번 기록으로 인해 실패하게 된 경우 (예: 폭식으로 칼로리 초과) -> 성공 취소
                if (alreadySucceededToday && uc.getType() == ChallengeType.CALORIE_LIMIT) {
                    updateUserChallengeStatus(uc, today, false);
                }
            }
        }
    }

    /**
     * 식단 삭제/수정 시 해당 날짜의 진척도를 재계산
     */
    @Override
    @Transactional
    public void recalculateChallengeProgress(Long userId, LocalDate date) {
        // 1. 해당 유저의 진행 중인 챌린지 조회
        List<ActiveChallengeDto> activeChallenges = challengeMapper.findActiveChallengesByUserId(userId);
        if (activeChallenges.isEmpty()) return;

        // 2. 해당 날짜의 식단 전체 조회하여 칼로리 재계산
        List<DietResponseDto> dailyDiets = dietMapper.findAllByDate(userId, date.toString());
        double dailyTotalCalories = dailyDiets.stream().mapToDouble(DietResponseDto::getTotalCalories).sum();

        // 3. 기록형/타임어택 여부 체크를 위해 가장 마지막 식사 시간 조회 (필요 시)
        // (삭제 시에는 남아있는 식단 중 조건에 맞는게 있는지 확인해야 함)
        // 로직 재활용을 위해 processChallengeUpdate 로직과 유사하게 수행

        for (ActiveChallengeDto uc : activeChallenges) {
            if (date.isBefore(uc.getStartDate()) || date.isAfter(uc.getEndDate())) continue;

            boolean isConditionMet = false;

            // 이미 성공 처리된 상태인지 확인
            boolean alreadySucceededToday = uc.getLastSuccessDate() != null && uc.getLastSuccessDate().equals(date);

            switch (uc.getType()) {
                case RECORD_FREQUENCY: // 기록형: 식단이 하나라도 있으면 성공
                    if (!dailyDiets.isEmpty()) isConditionMet = true;
                    break;
                case CALORIE_LIMIT: // 칼로리형: 누적 칼로리가 목표치 이하 & 식단이 하나라도 있어야 함(0칼로리 성공 악용 방지?)
                    // 보통 식단이 없으면 성공으로 치지 않음 -> 식단이 적어도 1개 존재해야 함
                    if (!dailyDiets.isEmpty() && dailyTotalCalories <= uc.getTargetValue()) isConditionMet = true;
                    break;
                case TIME_RANGE: // 타임어택: 목표 시간 이전에 먹은 식단이 하나라도 있으면 성공
                    for (DietResponseDto d : dailyDiets) {
                        if (d.getEatTime().getHour() < uc.getTargetValue()) {
                            isConditionMet = true;
                            break;
                        }
                    }
                    break;
            }

            if (isConditionMet) {
                if (!alreadySucceededToday) {
                    updateUserChallengeStatus(uc, date, true);
                }
            } else {
                // 조건을 만족하지 못하는데, 오늘 이미 성공으로 기록되어 있다면 -> 취소(Rollback) 필요
                if (alreadySucceededToday) {
                    updateUserChallengeStatus(uc, date, false);
                }
            }
        }
    }

    /**
     * 내 전체 챌린지 조회
     * - 어제 기록이 없으면 연속 성공(Streak)이 끊긴 것으로 간주하여 0으로 보정합니다.
     */
    @Override
    @Transactional
    public List<ChallengeResponseDto> getAllChallenges(Long userId) {
        List<ChallengeResponseDto> challenges = challengeMapper.findAllChallenges(userId);
        LocalDate yesterday = LocalDate.now().minusDays(1);

        for (ChallengeResponseDto dto : challenges) {
            if (!dto.isJoined() || dto.getCurrentCount() == 0) continue;
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
     *
     * @param isSuccess true: 성공 처리, false: 성공 취소
     */
    private void updateUserChallengeStatus(ActiveChallengeDto uc, LocalDate today, boolean isSuccess) {
        if (isSuccess) {
            int newCurrentCount = uc.getCurrentCount() + 1;
            int newCurrentStreak = 1;
            if (uc.getLastSuccessDate() != null && uc.getLastSuccessDate().equals(today.minusDays(1))) {
                newCurrentStreak = uc.getCurrentStreak() + 1;
            }
            int newMaxStreak = Math.max(uc.getMaxStreak(), newCurrentStreak);

            uc.setCurrentCount(newCurrentCount);
            uc.setCurrentStreak(newCurrentStreak);
            uc.setMaxStreak(newMaxStreak);
            uc.setLastSuccessDate(today);

            challengeMapper.updateProgress(uc);
            if (newCurrentCount >= uc.getGoalCount()) {
                challengeMapper.updateStatusToSuccess(uc.getUserChallengeId());
            }
        } else {
            // Rollback
            int newCurrentCount = Math.max(0, uc.getCurrentCount() - 1);
            int newCurrentStreak = Math.max(0, uc.getCurrentStreak() - 1);

            // Fix: If rolling back "today", lastSuccessDate check (alreadySucceededToday) relies on this NOT being today.
            // If streak is preserved (was > 1, now > 0), previous success was yesterday.
            // If streak broken (now 0), we set to null to safely clear "today" status.
            LocalDate rollbackDate = (newCurrentStreak > 0) ? today.minusDays(1) : null;

            uc.setCurrentCount(newCurrentCount);
            uc.setCurrentStreak(newCurrentStreak);
            uc.setLastSuccessDate(rollbackDate);

            challengeMapper.updateProgress(uc);
            if (newCurrentCount < uc.getGoalCount()) {
                challengeMapper.updateStatusToProgress(uc.getUserChallengeId());
            }
        }
    }

    /**
     * 챌린지 상세 조회
     * - 기본 정보, 내 진행 상황, 참여자 목록을 함께 반환합니다.
     */
    @Override
    public ChallengeResponseDto getChallengeDetail(Long userId, Long challengeId) {
        ChallengeResponseDto response = challengeMapper.findChallengeDetailById(userId, challengeId)
                .orElseThrow(() -> new CustomException(ErrorResponseCode.CHALLENGE_NOT_FOUND));

        List<ChallengeParticipantDto> participants = challengeMapper.findParticipantsByChallengeId(challengeId);
        response.setParticipants(participants);
        response.setCurrentHeadCount(participants.size());
        return response;
    }

    /**
     * 챌린지 수정 (방장만 가능)
     */
    @Override
    @Transactional
    public void updateChallenge(Long userId, Long challengeId, ChallengeCreateRequestDto dto) {
        Challenge challenge = challengeMapper.findById(challengeId);
        if (challenge == null) throw new CustomException(ErrorResponseCode.CHALLENGE_NOT_FOUND);
        if (challenge.getOwnerId() == null || !challenge.getOwnerId().equals(userId)) {
            throw new CustomException(ErrorResponseCode.UNAUTHORIZED);
        }

        // 필드 업데이트 (Setter 사용)
        challenge.setTitle(dto.getTitle());
        // ... 생략 (나머지 필드)
        challenge.setPublic(dto.isPublic());

        challengeMapper.updateChallenge(challenge);
    }

    /**
     * 챌린지 삭제 (방장만 가능)
     */
    @Override
    @Transactional
    public void deleteChallenge(Long userId, Long challengeId) {
        Challenge challenge = challengeMapper.findById(challengeId);
        if (challenge == null) throw new CustomException(ErrorResponseCode.CHALLENGE_NOT_FOUND);
        if (challenge.getOwnerId() == null || !challenge.getOwnerId().equals(userId)) {
            throw new CustomException(ErrorResponseCode.UNAUTHORIZED);
        }
        // 연관 데이터 삭제 순서 중요
        challengeMapper.deleteInvitationsByChallengeId(challengeId);
        challengeMapper.deleteUserChallengesByChallengeId(challengeId);
        challengeMapper.deleteChallengeById(challengeId);
    }

    /**
     * 챌린지 떠나기
     *
     * @param userId
     * @param challengeId
     */
    @Override
    @Transactional
    public void leaveChallenge(Long userId, Long challengeId) {
        Challenge challenge = challengeMapper.findById(challengeId);
        if (challenge == null) {
            throw new CustomException(ErrorResponseCode.CHALLENGE_NOT_FOUND);
        }

        // ownerId가 null이 아니고, 내가 ownerId라면 예외 발생
        // (ownerId가 null인 경우는 방장이 없는 방이므로, 참여자는 누구나 나갈 수 있음 -> 통과)
        if (challenge.getOwnerId() != null && challenge.getOwnerId().equals(userId)) {
            throw new CustomException(ErrorResponseCode.OWNER_CANNOT_LEAVE);
        }
        if (!challengeMapper.existsByUserIdAndChallengeId(userId, challengeId)) {
            throw new CustomException(ErrorResponseCode.NOT_JOINED_USER);
        }
        challengeMapper.deleteUserChallenge(userId, challengeId);
        challengeMapper.decreaseHeadCount(challengeId);
    }

    /**
     * 초대 응답 (승인/거절)
     */
    @Override
    @Transactional
    public void respondInvitation(Long userId, Long invitationId, boolean isAccepted) {
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
            Challenge challenge = challengeMapper.findById(invitation.getChallengeId());
            if (challenge == null) {
                challengeMapper.updateInvitationStatus(invitationId, "EXPIRED");
                throw new CustomException(ErrorResponseCode.CHALLENGE_NOT_FOUND);
            }
            try {
                joinChallengeLogic(userId, challenge);
                challengeMapper.updateInvitationStatus(invitationId, "ACCEPTED");
            } catch (CustomException e) {
                throw e;
            }
        } else {
            challengeMapper.updateInvitationStatus(invitationId, "REJECTED");
        }
    }

    /**
     * 나에게 온 초대장 목록 조회
     */
    @Override
    public List<ChallengeInvitationResponseDto> getMyInvitations(Long userId) {
        return challengeMapper.findPendingInvitationsByUserId(userId);
    }
}