package com.pagoda.matchmeal.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.pagoda.matchmeal.common.exception.CustomException;
import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import com.pagoda.matchmeal.mapper.AiChatbotMapper;
import com.pagoda.matchmeal.mapper.DietMapper;
import com.pagoda.matchmeal.mapper.UserMapper;
import com.pagoda.matchmeal.model.dto.ai.*;
import com.pagoda.matchmeal.model.entity.AiChatbot;
import com.pagoda.matchmeal.model.entity.Diet;
import com.pagoda.matchmeal.model.entity.DietDetail;
import com.pagoda.matchmeal.model.entity.User;
import com.pagoda.matchmeal.model.enums.AiType;
import com.pagoda.matchmeal.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiServiceImpl implements AiService {

    private final DietMapper dietMapper;
    private final UserMapper userMapper;
    private final AiChatbotMapper aiChatbotMapper;
    private final WebClient fastApiClient;

    @Override
    @Transactional
    public String getPeriodFeedback(Long userId, LocalDate startDate, LocalDate endDate) {
        User user = getUserOrThrow(userId);

        // DB 조회
        String startStr = startDate.toString();
        String endStr = endDate.toString();

        List<Diet> diets = dietMapper.selectDietsByPeriod(userId, startStr, endStr);
        List<DietDetail> details = dietMapper.selectDietDetailsByPeriod(userId, startStr, endStr);

        if (diets.isEmpty()) {
            return "해당 기간에 기록된 식단이 없습니다.";
        }

        // 통계 계산
        // 기간 총합
        double totalSodium = diets.stream().mapToDouble(Diet::getTotalSodium).sum();
        double totalSugar = diets.stream().mapToDouble(Diet::getTotalSugars).sum();
        double sumCalories = diets.stream().mapToDouble(Diet::getTotalCalories).sum();

        // 기간 계산 (1일 ~ N일)
        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        double avgCalories = (totalDays > 0) ? sumCalories / totalDays : 0;

        // 메뉴 이름 리스트 (중복 제거 후 수집)
        List<String> menuList = details.stream()
                .map(DietDetail::getFoodName)
//                .distinct() // 중복 메뉴 제거 (선택 사항)
                .collect(Collectors.toList());

        // 3. AI 요청 DTO 생성
        AiPeriodFeedbackRequestDto request = AiPeriodFeedbackRequestDto.builder()
                .userProfile(buildUserProfile(user))
                .periodInfo(AiPeriodInfoDto.builder()
                        .startDate(startStr)
                        .endDate(endStr)
                        .totalDays(totalDays)
                        .recordedMeals(diets.size())
                        .build())
                .nutritionStats(AiPeriodStatsDto.builder()
                        .avgCalories(avgCalories)
                        .totalSodium(totalSodium)
                        .totalSugar(totalSugar)
                        .build())
                .menuList(menuList)
                .build();

        // 4. FastAPI 호출
        String aiResponse = callFastApi("/ai/period-feedback", request);

        // 5. 결과 저장
        saveAiLog(userId, endDate, AiType.FEEDBACK,
                String.format("%s~%s 기간 분석", startStr, endStr), aiResponse);

        return aiResponse;
    }

    @Override
    @Transactional
    public String getMenuRecommendation(Long userId, String mealType) {
        User user = getUserOrThrow(userId);
        LocalDate today = LocalDate.now();

        // 1. 오늘 식단 조회 (이미 먹은 양 계산용)
        List<Diet> todayDiets = dietMapper.selectDietsByDate(userId, today.toString());

        // 2. 누적 섭취량 계산
        double curCal = todayDiets.stream().mapToDouble(Diet::getTotalCalories).sum();
        double curSod = todayDiets.stream().mapToDouble(Diet::getTotalSodium).sum();
        double curSug = todayDiets.stream().mapToDouble(Diet::getTotalSugars).sum();

        // 3. AI 요청 DTO 생성
        AiRecommendRequestDto request = AiRecommendRequestDto.builder()
                .userProfile(buildUserProfile(user))
                .currentIntake(AiIntakeSummaryDto.builder()
                        .calories(curCal)
                        .sodium(curSod)
                        .sugar(curSug)
                        .build())
                .mealType(mealType)
                .build();

        // 4. FastAPI 호출
        String aiResponse = callFastApi("ai/recommend", request);

        // 5. 결과 저장
        saveAiLog(userId, today, AiType.RECOMMENDATION, mealType + " 추천", aiResponse);

        return aiResponse;
    }

    // Helper Methods
    private AiUserProfileDto buildUserProfile(User user) {
        // 나이 계산
        int age = (user.getBirthDate() != null)
                ? Period.between(user.getBirthDate(), LocalDate.now()).getYears() : 0;

        // BMI 계산
        double bmi = 0.0;
        String bmiStatus = "정보없음";
        if (user.getHeightCm() != null && user.getWeightKg() != null && user.getHeightCm() > 0) {
            double h = user.getHeightCm() / 100.0;
            bmi = user.getWeightKg() / (h * h);

            if (bmi < 18.5) bmiStatus = "저체중";
            else if (bmi < 23) bmiStatus = "정상";
            else if (bmi < 25) bmiStatus = "과체중";
            else bmiStatus = "비만";
        }

        return AiUserProfileDto.builder()
                .name(user.getUserName())
                .age(age)
                .gender(user.getGender() != null ? user.getGender().name() : "UNKNOWN")
                .bmi(Math.round(bmi * 10) / 10.0)
                .bmiStatus(bmiStatus)
                .allergies(user.getAllergies())
                .diseases(user.getDiseases())
                .build();
    }

    private User getUserOrThrow(Long userId) {
        return userMapper.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorResponseCode.USER_NOT_FOUND));
    }

    private void saveAiLog(Long userId, LocalDate date, AiType type, String question, String answer) {
        AiChatbot chatbot = AiChatbot.builder()
                .userId(userId)
                .refDate(date)
                .aiType(type)
                .userQuestion(question)
                .aiResponse(answer)
                .build();
        aiChatbotMapper.insertChatLog(chatbot);
    }

    private String callFastApi(String uri, Object body) {
        try {
            JsonNode response = fastApiClient.post()
                    .uri(uri)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            // FastAPI 응답 포맷: {"result": "..."}
            return response != null && response.has("result")
                    ? response.get("result").asText()
                    : "AI 응답 형식이 올바르지 않습니다.";
        } catch (Exception e) {
            e.printStackTrace();
            return "AI 서버 연결 중 오류가 발생했습니다.";
        }
    }
}
