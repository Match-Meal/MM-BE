package com.pagoda.matchmeal.service;

import com.pagoda.matchmeal.model.dto.request.DietRequestDto;
import com.pagoda.matchmeal.model.dto.request.DietStatsRequestDto;
import com.pagoda.matchmeal.model.dto.response.DietResponseDto;
import com.pagoda.matchmeal.model.dto.response.DietStatsResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface DietService {

    Long recordDiet(Long userId, DietRequestDto dietRequestDto, MultipartFile file);

    List<DietResponseDto> getDailyDiet(Long userId, LocalDate date);

    DietResponseDto getDietDetail(Long userId, Long dietId);

    void updateDiet(Long userId, Long dietId, DietRequestDto dietRequestDto, MultipartFile file);

    void deleteDiet(Long userId, Long dietId);

    DietStatsResponseDto getDietStats(Long userId, DietStatsRequestDto dietStatsRequestDto);

    List<DietResponseDto> getDietListByPeriod(Long userId, LocalDate startDate, LocalDate endDate);
}
