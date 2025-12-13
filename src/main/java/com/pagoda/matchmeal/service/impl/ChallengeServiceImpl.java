package com.pagoda.matchmeal.service.impl;

import com.pagoda.matchmeal.mapper.ChallengeMapper;
import com.pagoda.matchmeal.model.dto.response.ActiveChallengeDto;
import com.pagoda.matchmeal.model.dto.response.ChallengeResponseDto;
import com.pagoda.matchmeal.model.entity.Diet;
import com.pagoda.matchmeal.model.entity.DietDetail;
import com.pagoda.matchmeal.model.enums.MealType;
import com.pagoda.matchmeal.service.ChallengeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ChallengeServiceImpl implements ChallengeService {

    private final ChallengeMapper challengeMapper;

    /**
     * 챌린지 참여
     * @param userId
     * @param challengeId
     */
    @Override
    public void joinChallenge(Long userId, Long challengeId) {
        // 이미 참여했는지 중복 체크 로직 필요
        challengeMapper.insertUserChallenge(userId, challengeId);
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
            //
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
