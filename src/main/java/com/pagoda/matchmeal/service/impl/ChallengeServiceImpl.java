package com.pagoda.matchmeal.service.impl;

import com.pagoda.matchmeal.common.exception.CustomException;
import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import com.pagoda.matchmeal.mapper.ChallengeMapper;
import com.pagoda.matchmeal.model.dto.ChallengeSearchCondition;
import com.pagoda.matchmeal.model.dto.request.ChallengeCreateRequestDto;
import com.pagoda.matchmeal.model.dto.response.ActiveChallengeDto;
import com.pagoda.matchmeal.model.dto.response.ChallengeResponseDto;
import com.pagoda.matchmeal.model.entity.Challenge;
import com.pagoda.matchmeal.model.entity.Diet;
import com.pagoda.matchmeal.model.entity.DietDetail;
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
    public List<ChallengeResponseDto> searchChallenges(ChallengeSearchCondition condition) {
        return challengeMapper.searchPublicChallenges(condition);
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
        // 유저가 참여 중인 챌린지 조회(DTO에 챌린지 마스터 정보 포함)
        List<ActiveChallengeDto> activeChallenges = challengeMapper.findActiveChallengesByUserId(userId);
        if (activeChallenges.isEmpty()) return;

        LocalDate today = LocalDate.now();

        for (ActiveChallengeDto uc: activeChallenges) {
            // 챌린지 기간 아니면 스킵
            if (today.isBefore(uc.getStartDate()) || today.isAfter(uc.getEndDate())) {
                continue;
            }

            // 오늘 이미 성공했다면 스킵
            if (uc.getLastSuccessDate() != null && uc.getLastSuccessDate().equals(today)) {
                continue;
            }

            boolean isSuccess = false;

            // 챌린지 타입별 로직
            switch (uc.getType()) {
                // 습관형
                case RECORD_FREQUENCY:
                    isSuccess = true;
                    break;

                // 칼로리형
                case CALORIE_LIMIT:
                    if (diet.getTotalCalories() < uc.getTargetValue()) {
                        isSuccess = true;
                    }
                    break;

                // 시간형
                case TIME_RANGE:
                    if (diet.getMealType() == MealType.BREAKFAST &&
                        diet.getEatTime().getHour() < uc.getTargetValue()) {
                        isSuccess = true;
                    }
                    break;
            }

            if (isSuccess) {
                updateUserChallengeStatus(uc, today);
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
     * 스트릭 계산 및 상태 업데이트
     * @param uc
     * @param today
     */
    private void updateUserChallengeStatus(ActiveChallengeDto uc, LocalDate today) {
        int newCurrentCount = uc.getCurrentCount() + 1;
        int newCurrentStreak = 1;

        // 연속 달성 로직
        if (uc.getLastSuccessDate() != null) {
            // 마지막 성공일 어제? -> 연속 성공
            if (uc.getLastSuccessDate().equals(today.minusDays(1))) {
                newCurrentStreak = uc.getCurrentStreak() + 1;
            }
            
            // 어제가 아니면 Streak은 1로 초기화
        }

        // 최대 스트릭 갱신
        int newMaxStreak = Math.max(uc.getMaxStreak(), newCurrentStreak);

        // DB 업데이트를 위한 파라미터 세팅
        uc.setCurrentCount(newCurrentCount);
        uc.setCurrentStreak(newCurrentStreak);
        uc.setMaxStreak(newMaxStreak);
        uc.setLastSuccessDate(today);

        // DB 업데이트 실행
        challengeMapper.updateProgress(uc);

        // 목표 달성 체크
        if (newCurrentCount >= uc.getGoalCount()) {
            challengeMapper.updateStatusToSuccess(uc.getUserChallengeId());

        }
    }

}
