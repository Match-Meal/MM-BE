package com.pagoda.matchmeal.service;

import com.pagoda.matchmeal.model.dto.response.ActiveChallengeDto;
import com.pagoda.matchmeal.model.dto.response.ChallengeResponseDto;
import com.pagoda.matchmeal.model.entity.Diet;
import com.pagoda.matchmeal.model.entity.DietDetail;

import java.time.LocalDate;
import java.util.List;

public interface ChallengeService {
    void joinChallenge(Long userId, Long challengeId);

    void updateChallengeProgress(Long userId, Diet diet, List<DietDetail> detail);

    List<ChallengeResponseDto> getAllChallenges(Long userId);
}
