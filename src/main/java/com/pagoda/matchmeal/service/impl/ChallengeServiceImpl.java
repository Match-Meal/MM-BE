package com.pagoda.matchmeal.service.impl;

import com.pagoda.matchmeal.mapper.ChallengeMapper;
import com.pagoda.matchmeal.model.dto.response.ActiveChallengeDto;
import com.pagoda.matchmeal.model.entity.Diet;
import com.pagoda.matchmeal.model.entity.DietDetail;
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
     * 식단 기록 시 자동으로 반영
     * @param userId
     * @param diet
     * @param detail
     */
    @Override
    public void updateChallengeProgress(Long userId, Diet diet, List<DietDetail> detail) {
        // 유저가 현재 참여중인 모든 챌린지를 가져옴
        List<ActiveChallengeDto> activeChallenges = challengeMapper.findActiveChallengesByUserId(userId);
        if (activeChallenges.isEmpty()) return;

        LocalDate today = LocalDate.now();

        // 각 챌린지별로 조사
        for (ActiveChallengeDto activeChallenge : activeChallenges) {
            // 중복 방지 이미 점수 획득했다면 스킵
            if (activeChallenge.getLastCheckedAt() != null && activeChallenge.getLastCheckedAt().equals(today)) {
                continue;
            }

            boolean isConditionMet = false;

            // 챌린지 타입 별 조건 매칭 로직
            switch (activeChallenge.getType()) {
                // 기록
                case RECORD_FREQUENCY:
                    isConditionMet = true;
                    break;

                // 칼로리 제한
                case CALORIE_LIMIT:
                    if (diet.getTotalCalories() <= activeChallenge.getTargetValue()) {
                        isConditionMet = true;
                    }
                    break;

                // 아침 식사 챙기기 (시간 제한)
                case TIME_RANGE:
                    if (diet.getEatTime().getHour() < activeChallenge.getTargetValue()) {
                        isConditionMet = true;
                    }
                    break;
            }

            // 조건 만족시 업데이트
            if (isConditionMet) {
                challengeMapper.updateProgress(activeChallenge.getUserChallengeId());
                
                // 목표 달성 체크
                if (activeChallenge.getCurrentProgress() + 1 >= 7) {
                    challengeMapper.updateStatusToSuccess(activeChallenge.getUserChallengeId());
                }
            }
        }


    }
}
