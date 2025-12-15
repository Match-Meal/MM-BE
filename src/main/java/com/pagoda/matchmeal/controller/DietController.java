package com.pagoda.matchmeal.controller;

import com.pagoda.matchmeal.common.response.CommonResponse;
import com.pagoda.matchmeal.common.util.ApiResponseUtil;
import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.model.dto.request.DietRequestDto;
import com.pagoda.matchmeal.model.dto.response.DietResponseDto;
import com.pagoda.matchmeal.service.DietService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class DietController {

    private final DietService dietService;

    @PostMapping("/diet")
    public CommonResponse<Long> addDiet(
            @AuthenticationPrincipal UserDto userDto,
            @RequestPart(value = "data") DietRequestDto dietRequestDto,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return ApiResponseUtil.created(dietService.recordDiet(userDto.getId(), dietRequestDto, file));
    }

    @GetMapping("/diet")
    public CommonResponse<List<DietResponseDto>> getDailyDiet(@AuthenticationPrincipal UserDto userDto,
                                                              @RequestParam(value = "date", required = false)
                                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        return ApiResponseUtil.success(dietService.getDailyDiet(userDto.getId(), date));
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
}
