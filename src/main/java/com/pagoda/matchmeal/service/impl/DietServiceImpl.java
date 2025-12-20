package com.pagoda.matchmeal.service.impl;

import com.pagoda.matchmeal.common.exception.CustomException;
import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import com.pagoda.matchmeal.mapper.DietMapper;
import com.pagoda.matchmeal.mapper.FoodMapper;
import com.pagoda.matchmeal.model.dto.request.DietRequestDto;
import com.pagoda.matchmeal.model.dto.request.DietStatsRequestDto;
import com.pagoda.matchmeal.model.dto.request.FoodRequestDto;
import com.pagoda.matchmeal.model.dto.response.*;
import com.pagoda.matchmeal.model.entity.Diet;
import com.pagoda.matchmeal.model.entity.DietDetail;
import com.pagoda.matchmeal.model.entity.Food;
import com.pagoda.matchmeal.service.ChallengeService;
import com.pagoda.matchmeal.service.DietService;
import com.pagoda.matchmeal.service.FoodService;
import com.pagoda.matchmeal.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 식단(Diet) 관련 비즈니스 로직 구현체
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DietServiceImpl implements DietService {

    private final DietMapper dietMapper;
    private final FoodMapper foodMapper;
    private final S3Service s3Service;
    private final FoodService foodService;
    private final ChallengeService challengeService;

    /**
     * [식단 기록]
     * 1. 사용자가 선택한 음식(또는 직접 입력한 음식)의 영양소를 섭취량에 맞춰 다시 계산합니다.
     * 2. 계산된 영양소를 스냅샷으로 만들어 상세 정보(Detail)를 구성합니다.
     * 3. 식단(Diet)을 먼저 저장하여 ID를 생성한 후, 상세 정보(Details)를 대량 저장합니다.
     */
    @Override
    @Transactional
    public Long recordDiet(Long userId, DietRequestDto dietRequestDto, MultipartFile file) {
        // 1. 이미지 업로드 처리
        String imgUrl = null;
        if (file != null && !file.isEmpty()) {
            imgUrl = s3Service.uploadFile(file, "diet");
        }

        // 1. 합계 변수 초기화 (부모 테이블에 저장할 총합)
        double totalCal = 0;
        double totalCarbohydrate = 0;
        double totalProtein = 0;
        double totalFat = 0;
        double totalSugars = 0;
        double totalSodium = 0;

        List<DietDetail> details = new ArrayList<>();

        // 2. 요청된 음식 리스트 순회 및 데이터 가공
        for (DietRequestDto.DietDetailRequestDto item : dietRequestDto.getFoods()) {

            Long finalFoodId = null; // 최종적으로 저장될 음식 ID (없으면 null)
            String finalFoodName = item.getFoodName(); // 음식 이름

            // 계산된 영양소 (스냅샷)
            double snapCal, snapCarbo, snapProtein, snapFat, snapSugars, snapSodium;

            // ==========================================================
            // CASE 1: 기존 음식 DB에서 선택한 경우 (ID가 있음)
            // ==========================================================
            if (item.getFoodId() != null) {
                Food food = foodMapper.findById(item.getFoodId());
                if (food == null) throw new IllegalArgumentException("음식 없음");

                finalFoodId = food.getFoodId();
                finalFoodName = food.getFoodName();

                // 비율 계산 (DB기준 vs 먹은양)
                double ratio = (food.getServingSize() > 0) ? item.getQuantity() / food.getServingSize() : 1.0;

                snapCal = food.getCalories() * ratio;
                snapCarbo = food.getCarbohydrate() * ratio;
                snapProtein = food.getProtein() * ratio;
                snapFat = food.getFat() * ratio;
                snapSugars = food.getSugars() * ratio;
                snapSodium = food.getSodium() * ratio;
            }
            // ==========================================================
            // CASE 2: 직접 입력한 경우 (ID 없음)
            // ==========================================================
            else {
                // 직접 입력은 "입력한 영양소 = 섭취한 영양소"라고 가정 (비율 1:1)
                // (사용자가 "닭가슴살 200g에 단백질 40g이야"라고 입력하면 그게 곧 스냅샷)
                snapCal = item.getCalories();
                snapCarbo = item.getCarbohydrate();
                snapProtein = item.getProtein();
                snapFat = item.getFat();
                snapSugars = item.getSugars();
                snapSodium = item.getSodium();

                // ★ 체크박스 로직: "음식 DB에 저장해주세요" 라고 했나요?
                if (item.isSaveToMyFoods()) {
                    FoodRequestDto newFoodRequest = FoodRequestDto.builder()
                            .foodName(item.getFoodName())
                            .category("사용자등록")
                            .servingSize(item.getQuantity()) // 입력한 양을 1회 제공량으로 기준 잡음
                            .unit(item.getUnit())
                            .calories(item.getCalories())
                            .carbohydrate(item.getCarbohydrate())
                            .protein(item.getProtein())
                            .fat(item.getFat())
                            .sugars(item.getSugars())
                            .sodium(item.getSodium())
                            .build();

                    finalFoodId = foodService.addFood(userId, newFoodRequest);
                }
                // 체크 안 했으면 finalFoodId는 그냥 null 상태로 남음 (일회성 기록)
            }

            // ==========================================================
            // 공통: 상세(Detail) 객체 생성 및 리스트 추가
            // ==========================================================
            DietDetail detail = DietDetail.builder()
                    // dietId는 나중에 부모 저장 후 주입
                    .foodId(finalFoodId) // ★ ID가 있을 수도, 없을 수도(null) 있음
                    .foodName(finalFoodName)
                    .quantity(item.getQuantity())
                    .unit(item.getUnit())
                    .calories(snapCal)
                    .carbohydrate(snapCarbo)
                    .protein(snapProtein)
                    .fat(snapFat)
                    .sugars(snapSugars)
                    .sodium(snapSodium)
                    .build();

            details.add(detail);

            // 6. 총합 누적
            totalCal += snapCal;
            totalCarbohydrate += snapCarbo;
            totalProtein += snapProtein;
            totalFat += snapFat;
            totalSugars += snapSugars;
            totalSodium += snapSodium;
        }

        // 7. 부모(Diet) 엔티티 생성
        Diet diet = Diet.builder()
                .userId(userId)
                .eatDate(dietRequestDto.getEatDate())
                .eatTime(dietRequestDto.getEatTime())
                .mealType(dietRequestDto.getMealType())
                .memo(dietRequestDto.getMemo())
                .dietImgUrl(imgUrl)
                // 계산된 총합 저장
                .totalCalories(totalCal)
                .totalCarbohydrate(totalCarbohydrate)
                .totalProtein(totalProtein)
                .totalFat(totalFat)
                .totalSugars(totalSugars)
                .totalSodium(totalSodium)
                .build();

        // 8. 부모 저장
        dietMapper.insertDiet(diet);

        // 9. 자식들에게 부모 ID 주입
        List<DietDetail> finalDetails = new ArrayList<>();
        for (DietDetail d : details) {
            DietDetail completeDetail = DietDetail.builder()
                    .dietId(diet.getDietId()) // ★ 부모 ID 주입
                    .foodId(d.getFoodId())
                    .foodName(d.getFoodName())
                    .quantity(d.getQuantity())
                    .unit(d.getUnit())
                    .calories(d.getCalories())
                    .carbohydrate(d.getCarbohydrate())
                    .protein(d.getProtein())
                    .fat(d.getFat())
                    .sugars(d.getSugars())
                    .sodium(d.getSodium())
                    .build();
            finalDetails.add(completeDetail);
        }

        // 10. 자식 대량 저장
        if (!finalDetails.isEmpty()) {
            dietMapper.insertDietDetails(finalDetails);
        }

        // 챌린지 반영
        challengeService.updateChallengeProgress(userId, diet, finalDetails);

        // 11. 생성된 ID 반환
        return diet.getDietId();
    }

    /**
     * [일별 조회] 특정 날짜의 식단 리스트 반환
     * - 달력이나 메인 화면에서 '오늘 뭐 먹었지?' 볼 때 사용
     * - 페이징 없이 리스트 전체 반환
     */
    @Override
    public List<DietResponseDto> getDailyDiet(Long userId, LocalDate date) {
        return dietMapper.findAllByDate(userId, date.toString());
    }

    /**
     * [상세 조회] 식단 ID로 단건 조회
     */
    @Override
    public DietResponseDto getDietDetail(Long userId, Long dietId) {
        DietResponseDto diet = dietMapper.findDietByDietId(dietId);
        if (diet == null) {
            throw new CustomException(ErrorResponseCode.DIET_NOT_FOUND);
        }
        // 챌린지 참여자간 공유 목적으로 임시 비활성화
//        if (!diet.getUserId().equals(userId)) {
//            throw new CustomException(ErrorResponseCode.UNAUTHORIZED);
//        }
        return diet;
    }

    /**
     * [식단 수정]
     * - 전략: 기존 상세 데이터를 수정하는 것은 복잡하므로,
     * 1. 부모(Diet) 정보 업데이트
     * 2. 기존 자식(Details) 모두 삭제
     * 3. 새로운 자식(Details) 재등록 (Delete-Insert 방식)
     */
    @Override
    @Transactional
    public void updateDiet(Long userId, Long dietId, DietRequestDto dietRequestDto, MultipartFile file) {
        // 1. 기존 식단 조회
        DietResponseDto existingDiet = dietMapper.findDietByDietId(dietId); // (null 체크 및 권한 체크 필수)

        String newImgUrl = existingDiet.getDietImgUrl();

        // 2. 새 이미지가 들어왔다면?
        if (file != null && !file.isEmpty()) {
            // 2-1. 기존 이미지가 있었다면 S3에서 삭제 (쓰레기 파일 방지)
            if (existingDiet.getDietImgUrl() != null) {
                s3Service.deleteFile(existingDiet.getDietImgUrl());
            }
            // 2-2. 새 이미지 업로드
            newImgUrl = s3Service.uploadFile(file, "diet");
        }

        // 1. 합계 변수 초기화
        double totalCal = 0;
        double totalCarbohydrate = 0;
        double totalProtein = 0;
        double totalFat = 0;
        double totalSugars = 0;
        double totalSodium = 0;

        List<DietDetail> newDetails = new ArrayList<>();

        // 2. 새로운 음식 리스트 재계산 (Create 로직과 동일)
        for (DietRequestDto.DietDetailRequestDto item : dietRequestDto.getFoods()) {

            // 음식 정보 가져오기 (기존 조회 or 신규 생성 로직 재사용 권장)
            // 여기선 심플하게 조회만 구현
            Food food = foodMapper.findById(item.getFoodId());
            if (food == null) throw new IllegalArgumentException("음식 정보 없음 ID=" + item.getFoodId());

            // 비율 및 스냅샷 계산
            double ratio = (food.getServingSize() > 0) ? item.getQuantity() / food.getServingSize() : 1.0;

            double cal = food.getCalories() * ratio;
            double carbo = food.getCarbohydrate() * ratio;
            double protein = food.getProtein() * ratio;
            double fat = food.getFat() * ratio;
            double sugars = food.getSugars() * ratio;
            double sodium = food.getSodium() * ratio;

            DietDetail detail = DietDetail.builder()
                    .dietId(dietId) // 기존 식단 ID 유지
                    .foodId(food.getFoodId())
                    .foodName(food.getFoodName())
                    .quantity(item.getQuantity())
                    .unit(item.getUnit())
                    .calories(cal)
                    .carbohydrate(carbo)
                    .protein(protein)
                    .fat(fat)
                    .sugars(sugars)
                    .sodium(sodium)
                    .build();

            newDetails.add(detail);

            // 합계 누적
            totalCal += cal;
            totalCarbohydrate += carbo;
            totalProtein += protein;
            totalFat += fat;
            totalSugars += sugars;
            totalSodium += sodium;
        }

        // 3. 부모(Diet) 정보 업데이트
        Diet dietUpdate = Diet.builder()
                .dietId(dietId)
                .eatTime(dietRequestDto.getEatTime())
                .mealType(dietRequestDto.getMealType())
                .memo(dietRequestDto.getMemo())
                .dietImgUrl(newImgUrl) // 이미지 수정 로직이 있다면 추가
                .totalCalories(totalCal)
                .totalCarbohydrate(totalCarbohydrate)
                .totalProtein(totalProtein)
                .totalFat(totalFat)
                .totalSugars(totalSugars)
                .totalSodium(totalSodium)
                .build();

        dietMapper.updateDiet(dietUpdate);

        // 4. 기존 상세(Details) 모두 삭제
        dietMapper.deleteDietDetailByDietId(dietId);

        // 5. 새로운 상세(Details) 대량 등록
        if (!newDetails.isEmpty()) {
            dietMapper.insertDietDetails(newDetails);
        }
    }

    /**
     * [식단 삭제]
     * - DB의 ON DELETE CASCADE 설정 덕분에 부모만 지우면 자식도 자동 삭제됨
     */
    @Override
    @Transactional
    public void deleteDiet(Long userId, Long dietId) {
        DietResponseDto diet = dietMapper.findDietByDietId(dietId);
        if (diet == null || diet.getDeletedAt() != null) {
            throw new CustomException(ErrorResponseCode.DIET_NOT_FOUND);
        }
        if (!diet.getUserId().equals(userId)) {
            throw new CustomException(ErrorResponseCode.UNAUTHORIZED);
        }
        dietMapper.deleteDietByDietId(dietId);
    }

    @Override
    public DietStatsResponseDto getDietStats(Long userId, DietStatsRequestDto dietStatsRequestDto) {
        // 1. 기간 계산 (시작일~종료일 확정)
        LocalDate endDate;
        if (dietStatsRequestDto.getEndDate() == null) {
            endDate = LocalDate.now();
        } else {
            endDate = dietStatsRequestDto.getEndDate();
        }
        LocalDate startDate = calculateStartDate(dietStatsRequestDto.getPeriodType(), dietStatsRequestDto.getStartDate(), endDate);

        // 2. DB 조회 (입력한 날짜만 나옴)
        List<DailyDietStatDto> rawStats = dietMapper.getDailyDietStats(userId, startDate, endDate);

        // 3. 빈 날짜 채우기 (Gap Filling) - 그래프 끊김 방지
        List<DailyDietStatDto> fullDailyStats = fillMissingDates(startDate, endDate, rawStats);

        // 4. 기간 평균 및 분석 데이터 계산 (여기가 핵심!)
        return analyzeDietStats(fullDailyStats, startDate, endDate);
    }

    // 1. 시작일 계산기
    private LocalDate calculateStartDate(String type, LocalDate customStart, LocalDate end) {
        if ("MONTHLY".equals(type))
            return end.minusMonths(1);
        if ("CUSTOM".equals(type) && customStart != null)
            return customStart;
        return end.minusDays(6); // 기본: 일주일 (오늘 포함 7일)
    }

    // 2. 빈 날짜 채우기 (0점 처리)
    private List<DailyDietStatDto> fillMissingDates(LocalDate start, LocalDate end, List<DailyDietStatDto> rawData) {
        Map<LocalDate, DailyDietStatDto> map = rawData.stream()
                .collect(Collectors.toMap(DailyDietStatDto::getDate, Function.identity()));

        List<DailyDietStatDto> result = new ArrayList<>();

        // 시작일부터 종료일까지 하루씩 증가하며 Map에 없으면 0으로 채움
        start.datesUntil(end.plusDays(1)).forEach(date -> {
            result.add(map.getOrDefault(date, DailyDietStatDto.builder()
                    .date(date)
                    .totalCalories(0).carbsG(0).proteinG(0).fatG(0).sugarG(0).sodiumMg(0)
                    .build()));
        });
        return result;
    }

    // 3. ★ 분석 로직 (비율, 경고 등)
    private DietStatsResponseDto analyzeDietStats(List<DailyDietStatDto> stats, LocalDate start, LocalDate end) {
        // A. 평균 계산 (값이 없으면 0.0)
        double totalDays = stats.isEmpty() ? 1 : stats.size();
        double avgCal = stats.stream().mapToInt(DailyDietStatDto::getTotalCalories).average().orElse(0);
        double avgCarb = stats.stream().mapToInt(DailyDietStatDto::getCarbsG).average().orElse(0);
        double avgProtein = stats.stream().mapToInt(DailyDietStatDto::getProteinG).average().orElse(0);
        double avgFat = stats.stream().mapToInt(DailyDietStatDto::getFatG).average().orElse(0);
        double avgSugar = stats.stream().mapToInt(DailyDietStatDto::getSugarG).average().orElse(0);
        double avgSodium = stats.stream().mapToInt(DailyDietStatDto::getSodiumMg).average().orElse(0);

        // B. 탄단지 비율 분석 (kcal 기준: 탄4, 단4, 지9)
        double totalMacroCal = (avgCarb * 4) + (avgProtein * 4) + (avgFat * 9);
        totalMacroCal = (totalMacroCal == 0) ? 1 : totalMacroCal; // 0 나누기 방지

        // 1. 실제 비율 계산 (소수점 반올림하여 저장)
        double actualCarbRatio = Math.round((avgCarb * 4 / totalMacroCal) * 100);
        double actualProteinRatio = Math.round((avgProtein * 4 / totalMacroCal) * 100);
        double actualFatRatio = Math.round((avgFat * 9 / totalMacroCal) * 100);

        // 2. 목표 비율 정의 (권장 5:2:3 -> 50%, 20%, 30%)
        // (추후 user_goals 테이블에서 가져오는 값으로 대체 가능)
        double goalCarbRatio = 50.0;
        double goalProteinRatio = 20.0;
        double goalFatRatio = 30.0;

        // 3. ★ [수정 포인트] 피드백 생성 메서드 호출
        String feedbackMsg = generateCpfFeedback(
                new double[]{actualCarbRatio, actualProteinRatio, actualFatRatio},
                new double[]{goalCarbRatio, goalProteinRatio, goalFatRatio}
        );

        // 4. 탄단지 DTO 생성
        MacronutrientAnalysisDto cpfAnalysis = MacronutrientAnalysisDto.builder()
                .carbRatio(actualCarbRatio)
                .proteinRatio(actualProteinRatio)
                .fatRatio(actualFatRatio)
                .recommendedCarbRatio(goalCarbRatio)
                .recommendedProteinRatio(goalProteinRatio)
                .recommendedFatRatio(goalFatRatio)
                .feedback(feedbackMsg) // 생성된 메시지 주입
                .build();

        // C. 당류 분석 (권고: 총 섭취 열량의 10% 미만)
        // 당류 1g = 4kcal. (평균칼로리 * 0.1) / 4 = 권장 g수
        int recommendedSugarLimit = (int) ((avgCal * 0.1) / 4);
        // 0이면 최소한의 값(예: 25g) 또는 0 처리
        recommendedSugarLimit = (recommendedSugarLimit == 0) ? 25 : recommendedSugarLimit;

        NutrientStatusDto sugarAnalysis = NutrientStatusDto.builder()
                .nutrientName("당류")
                .currentIntake((int) avgSugar)
                .recommendedLimit(recommendedSugarLimit)
                .intakePercentage((int) (avgSugar * 100 / recommendedSugarLimit))
                // 권장량 초과 시 BAD, 아니면 GOOD
                .status(avgSugar > recommendedSugarLimit ? NutrientStatusDto.NutrientLevel.BAD : NutrientStatusDto.NutrientLevel.GOOD)
                .build();

        // D. 나트륨 분석 (권고: 2000mg 미만)
        int recommendedSodiumLimit = 2000;

        NutrientStatusDto sodiumAnalysis = NutrientStatusDto.builder()
                .nutrientName("나트륨")
                .currentIntake((int) avgSodium)
                .recommendedLimit(recommendedSodiumLimit)
                .intakePercentage((int) (avgSodium * 100 / recommendedSodiumLimit))
                .status(avgSodium > recommendedSodiumLimit ? NutrientStatusDto.NutrientLevel.BAD : NutrientStatusDto.NutrientLevel.GOOD)
                .build();

        // E. 최종 응답 DTO 조립
        return DietStatsResponseDto.builder()
                .periodTotalDays((int) totalDays)
                .averageCalories((int) avgCal)
                .cpfRatioAnalysis(cpfAnalysis)
                .sugarAnalysis(sugarAnalysis)
                .sodiumAnalysis(sodiumAnalysis)
                .dailyStats(stats) // 그래프용 일별 데이터
                .build();
    }

    // 비율 피드백 생성 메서드 (예시)
    private String generateCpfFeedback(double[] actualRatio, double[] goalRatio) {
        double carbDiff = actualRatio[0] - goalRatio[0];    // 탄수화물 차이 (+면 과잉, -면 부족)
        double proteinDiff = actualRatio[1] - goalRatio[1]; // 단백질 차이
        double fatDiff = actualRatio[2] - goalRatio[2];     // 지방 차이

        // 허용 오차 범위 (±10%)
        final double THRESHOLD = 10.0;

        // 1. 데이터 없음 체크
        if (actualRatio[0] == 0 && actualRatio[1] == 0 && actualRatio[2] == 0) {
            return "식단 기록이 없어 분석할 수 없어요. 😢";
        }

        // 2. [우선순위 1] 단백질 부족 체크 (가장 중요)
        if (proteinDiff < -THRESHOLD) {
            return "단백질이 많이 부족해요! 🍗 닭가슴살이나 두부, 계란을 더 챙겨드세요.";
        }

        // 3. [우선순위 2] 탄수화물 과잉 체크
        if (carbDiff > THRESHOLD) {
            return "탄수화물 비중이 너무 높아요. 🍚 밥이나 면 양을 조금만 줄여볼까요?";
        }

        // 4. [우선순위 3] 지방 과잉 체크
        if (fatDiff > THRESHOLD) {
            return "지방 섭취가 많네요. 튀김이나 기름진 고기 대신 살코기 위주로 드셔보세요.";
        }

        // 5. [우선순위 4] 탄수화물 부족 (다이어트 중 기력 저하 우려)
        if (carbDiff < -THRESHOLD) {
            return "에너지가 부족할 수 있어요. 🍠 고구마나 통곡물로 탄수화물을 보충해주세요.";
        }

        // 6. [기본] 밸런스 양호 (모든 오차가 10% 이내)
        return "탄단지 비율이 황금 밸런스입니다! 아주 훌륭해요! 🌿";
    }
}
