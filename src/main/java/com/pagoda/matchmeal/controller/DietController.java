package com.pagoda.matchmeal.controller;

import com.pagoda.matchmeal.common.response.CommonResponse;
import com.pagoda.matchmeal.common.util.ApiResponseUtil;
import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.model.dto.request.DietRequestDto;
import com.pagoda.matchmeal.model.dto.request.DietStatsRequestDto;
import com.pagoda.matchmeal.model.dto.response.DietResponseDto;
import com.pagoda.matchmeal.model.dto.response.DietStatsResponseDto;
import com.pagoda.matchmeal.model.dto.response.FoodAnalysisResponseDto;
import com.pagoda.matchmeal.service.AiFoodVisionService;
import com.pagoda.matchmeal.service.DietService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

/**
 * 식단 관리 컨트롤러
 * - 식단 CRUD, AI 이미지 분석, 통계 조회 API 제공
 */
@RestController
@RequiredArgsConstructor
public class DietController {

    private final DietService dietService;
    private final AiFoodVisionService aiFoodVisionService;

    /**
     * 식단 이미지 AI 분석 요청
     */
    @PostMapping(value = "/diet/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResponse<FoodAnalysisResponseDto> analyzeDietImage(
            @RequestPart("file") MultipartFile file
    ) {
        // AiFoodService.analyzeAndFindFood 호출
        return ApiResponseUtil.success(aiFoodVisionService.analyzeAndFindFood(file));
    }

    /**
     * 식단 기록 저장
     */
    @PostMapping("/diet")
    public CommonResponse<Long> addDiet(
            @AuthenticationPrincipal UserDto userDto,
            @RequestPart(value = "data") DietRequestDto dietRequestDto,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return ApiResponseUtil.created(dietService.recordDiet(userDto.getId(), dietRequestDto, file));
    }

    /**
     * 일별 식단 조회
     */
    @GetMapping("/diet")
    public CommonResponse<List<DietResponseDto>> getDailyDiet(
            @AuthenticationPrincipal UserDto userDto,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "userId", required = false) Long targetUserId) {

        if (date == null) {
            date = LocalDate.now();
        }

        // 타겟 ID가 있으면 그 사람 것을, 없으면 내 것을 조회
        Long searchUserId = (targetUserId != null) ? targetUserId : userDto.getId();

        return ApiResponseUtil.success(dietService.getDailyDiet(searchUserId, date));
    }

    /**
     * 식단 상세 조회
     */
    @GetMapping("/diet/{dietId}")
    public CommonResponse<DietResponseDto> getDietById(@AuthenticationPrincipal UserDto userDto, @PathVariable Long dietId) {
        return ApiResponseUtil.success(dietService.getDietDetail(userDto.getId(), dietId));
    }

    /**
     * 식단 수정
     */
    @PutMapping("/diet/{dietId}")
    public CommonResponse<Void> updateDiet(@AuthenticationPrincipal UserDto userDto,
                                           @PathVariable Long dietId,
                                           @RequestPart(value = "data") DietRequestDto requestDto,
                                           @RequestPart(value = "file") MultipartFile file) {
        dietService.updateDiet(userDto.getId(), dietId, requestDto, file);
        return ApiResponseUtil.success();
    }

    /**
     * 식단 삭제
     */
    @DeleteMapping("/diet/{dietId}")
    public CommonResponse<Void> deleteDiet(@AuthenticationPrincipal UserDto userDto, @PathVariable Long dietId) {
        dietService.deleteDiet(userDto.getId(), dietId);
        return ApiResponseUtil.success();
    }

    /**
     * 식단 통계 조회
     */
    @GetMapping("/diet/stats")
    public CommonResponse<DietStatsResponseDto> getDietStats(
            @AuthenticationPrincipal UserDto userDto,
            @ModelAttribute DietStatsRequestDto dietStatsRequestDto,
            @RequestParam(value = "userId", required = false) Long targetUserId) {

        Long searchUserId = (targetUserId != null) ? targetUserId : userDto.getId();

        return ApiResponseUtil.success(dietService.getDietStats(searchUserId, dietStatsRequestDto));
    }

    /**
     * 기간별 식단 리스트 조회 (챌린지용 등)
     */
    @GetMapping("/diet/period")
    public CommonResponse<List<DietResponseDto>> getDietListByPeriod(
            @AuthenticationPrincipal UserDto userDto,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "userId", required = false) Long targetUserId) {

        // targetUserId가 있으면 그 유저를, 없으면 로그인한 유저 본인을 조회
        Long searchUserId = (targetUserId != null) ? targetUserId : userDto.getId();

        return ApiResponseUtil.success(dietService.getDietListByPeriod(searchUserId, startDate, endDate));
    }
}