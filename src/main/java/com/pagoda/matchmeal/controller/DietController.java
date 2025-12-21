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

@RestController
@RequiredArgsConstructor
public class DietController {

    private final DietService dietService;
    private final AiFoodVisionService aiFoodVisionService;

    @PostMapping(value = "/diet/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResponse<FoodAnalysisResponseDto> analyzeDietImage(
            @RequestPart("file") MultipartFile file
    ) {
        // AiFoodService.analyzeAndFindFood 호출
        return ApiResponseUtil.success(aiFoodVisionService.analyzeAndFindFood(file));
    }

    @PostMapping("/diet")
    public CommonResponse<Long> addDiet(
            @AuthenticationPrincipal UserDto userDto,
            @RequestPart(value = "data") DietRequestDto dietRequestDto,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return ApiResponseUtil.created(dietService.recordDiet(userDto.getId(), dietRequestDto, file));
    }

    @GetMapping("/diet")
    public CommonResponse<List<DietResponseDto>> getDailyDiet(
            @AuthenticationPrincipal UserDto userDto,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "userId", required = false) Long targetUserId) { // [추가]

        if (date == null) {
            date = LocalDate.now();
        }

        // 타겟 ID가 있으면 그 사람 것을, 없으면 내 것을 조회
        Long searchUserId = (targetUserId != null) ? targetUserId : userDto.getId();

        return ApiResponseUtil.success(dietService.getDailyDiet(searchUserId, date));
    }

    @GetMapping("/diet/{dietId}")
    public CommonResponse<DietResponseDto> getDietById(@AuthenticationPrincipal UserDto userDto, @PathVariable Long dietId) {
        return ApiResponseUtil.success(dietService.getDietDetail(userDto.getId(), dietId));
    }

    @PutMapping("/diet/{dietId}")
    public CommonResponse<Void> updateDiet(@AuthenticationPrincipal UserDto userDto,
                                           @PathVariable Long dietId,
                                           @RequestPart(value = "data") DietRequestDto requestDto,
                                           @RequestPart(value = "file") MultipartFile file) {
        dietService.updateDiet(userDto.getId(), dietId, requestDto, file);
        return ApiResponseUtil.success();
    }

    @DeleteMapping("/diet/{dietId}")
    public CommonResponse<Void> deleteDiet(@AuthenticationPrincipal UserDto userDto, @PathVariable Long dietId) {
        dietService.deleteDiet(userDto.getId(), dietId);
        return ApiResponseUtil.success();
    }

    @GetMapping("/diet/stats")
    public CommonResponse<DietStatsResponseDto> getDietStats(
            @AuthenticationPrincipal UserDto userDto,
            @ModelAttribute DietStatsRequestDto dietStatsRequestDto,
            @RequestParam(value = "userId", required = false) Long targetUserId) { // [추가]

        Long searchUserId = (targetUserId != null) ? targetUserId : userDto.getId();

        return ApiResponseUtil.success(dietService.getDietStats(searchUserId, dietStatsRequestDto));
    }

    /**
     * [기간 조회] 챌린지 기간 등 특정 기간의 식단 기록 조회
     * - 본인 또는 타인의 식단 기록을 기간 단위로 조회
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
