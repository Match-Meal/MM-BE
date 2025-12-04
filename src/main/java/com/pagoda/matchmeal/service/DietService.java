package com.pagoda.matchmeal.service;

import com.pagoda.matchmeal.model.dto.request.DietRequestDto;
import com.pagoda.matchmeal.model.dto.response.DietResponseDto;

import java.time.LocalDate;
import java.util.List;

public interface DietService {

    Long recordDiet(Long userId, DietRequestDto dietRequestDto);

    List<DietResponseDto> getDailyDiet(Long userId, LocalDate date);

    DietResponseDto getDietDetail(Long userId, Long dietId);

    void updateDiet(Long userId, Long dietId, DietRequestDto dietRequestDto);

    void deleteDiet(Long userId, Long dietId);
}
