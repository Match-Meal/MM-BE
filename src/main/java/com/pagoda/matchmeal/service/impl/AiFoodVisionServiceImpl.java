package com.pagoda.matchmeal.service.impl;

import com.pagoda.matchmeal.mapper.FoodMapper;
import com.pagoda.matchmeal.model.dto.AiResponseDto;
import com.pagoda.matchmeal.model.dto.MatchedFoodDto;
import com.pagoda.matchmeal.model.dto.response.FoodAnalysisResponseDto;
import com.pagoda.matchmeal.model.dto.response.FoodDetailResponseDto;
import com.pagoda.matchmeal.service.AiFoodVisionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class AiFoodVisionServiceImpl implements AiFoodVisionService {

    private final FoodMapper foodMapper;
    private final WebClient webClient;

    @Value("${ai.url}")
    private String FASTAPI_URL;

    @Override
    public FoodAnalysisResponseDto analyzeAndFindFood(MultipartFile image) {
        // 1. [유효성 검사] 이미지가 비어있는지 확인
        if (image.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 없습니다.");
        }

        AiResponseDto aiResult;

        // 2. [FastAPI 호출] 이미지 전송 및 분석 결과 수신
        try {
            aiResult = webClient.post()
                    .uri(FASTAPI_URL)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData("file", new ByteArrayResource(image.getBytes()) {
                        @Override
                        public String getFilename() {
                            return image.getOriginalFilename(); // 파일명 유지를 위해 오버라이드 필수
                        }
                    }))
                    .retrieve()
                    .bodyToMono(AiResponseDto.class)
                    .block(); // 동기 처리 (결과 기다림)

        } catch (IOException e) {
            log.error("이미지 처리 중 오류 발생", e);
            throw new RuntimeException("이미지 파일을 읽을 수 없습니다.");
        } catch (Exception e) {
            log.error("AI 서버 통신 오류", e);
            throw new RuntimeException("AI 서버 분석에 실패했습니다.");
        }

        // AI 응답이 null일 경우 방어 로직
        if (aiResult == null || aiResult.getBestCandidate() == null) {
            throw new RuntimeException("AI 분석 결과를 받지 못했습니다.");
        }

        String predictedName = aiResult.getBestCandidate(); // 예: "김치찌개"

        // 3. [DB 검색] AI가 알려준 이름으로 DB에서 음식 찾기
        // (Repository 메소드는 findByNameContaining(String name) 가정)
        List<FoodDetailResponseDto> foodEntities = foodMapper.findFoodDetailByFoodName(predictedName);

        // 4. [Entity -> DTO 변환] MatchedFoodDto 리스트 생성
        List<MatchedFoodDto> matchedFoodDtos = foodEntities.stream()
                .map(entity -> MatchedFoodDto.builder()
                        .foodId(entity.getFoodId())
                        .servingSize(entity.getServingSize())
                        .unit(entity.getUnit())
                        .foodName(entity.getFoodName())
                        .calories(entity.getCalories())
                        .carbohydrate(entity.getCarbohydrate())
                        .protein(entity.getProtein())
                        .fat(entity.getFat())
                        .sugars(entity.getSugars())
                        .sodium(entity.getSodium())
                        .build())
                .collect(Collectors.toList());

        // 5. [최종 응답 생성] FoodAnalysisResponseDto 빌드 및 반환
        return FoodAnalysisResponseDto.builder()
                .predictedName(predictedName)
                .candidates(aiResult.getCandidates() != null ? aiResult.getCandidates() : Collections.emptyList())
                .matchedFoods(matchedFoodDtos)
                .build();
    }
}
