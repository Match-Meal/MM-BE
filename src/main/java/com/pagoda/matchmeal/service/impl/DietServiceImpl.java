package com.pagoda.matchmeal.service.impl;

import com.pagoda.matchmeal.common.exception.CustomException;
import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import com.pagoda.matchmeal.mapper.DietMapper;
import com.pagoda.matchmeal.mapper.FoodMapper;
import com.pagoda.matchmeal.model.dto.request.DietRequestDto;
import com.pagoda.matchmeal.model.dto.response.DietResponseDto;
import com.pagoda.matchmeal.model.entity.Diet;
import com.pagoda.matchmeal.model.entity.DietDetail;
import com.pagoda.matchmeal.model.entity.Food;
import com.pagoda.matchmeal.service.DietService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 식단(Diet) 관련 비즈니스 로직 구현체
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DietServiceImpl implements DietService {

    private final DietMapper dietMapper;
    private final FoodMapper foodMapper;

    /**
     * [식단 기록]
     * 1. 사용자가 선택한 음식(또는 직접 입력한 음식)의 영양소를 섭취량에 맞춰 다시 계산합니다.
     * 2. 계산된 영양소를 스냅샷으로 만들어 상세 정보(Detail)를 구성합니다.
     * 3. 식단(Diet)을 먼저 저장하여 ID를 생성한 후, 상세 정보(Details)를 대량 저장합니다.
     */
    @Override
    @Transactional
    public Long recordDiet(Long userId, DietRequestDto dietRequestDto) {
        // 1. 합계 변수 초기화 (부모 테이블에 저장할 총합)
        double totalCal = 0;
        double totalCarbohydrate = 0;
        double totalProtein = 0;
        double totalFat = 0;

        List<DietDetail> details = new ArrayList<>();

        // 2. 요청된 음식 리스트 순회 및 데이터 가공
        for (DietRequestDto.DietDetailRequestDto item : dietRequestDto.getFoods()) {

            Long finalFoodId = null; // 최종적으로 저장될 음식 ID (없으면 null)
            String finalFoodName = item.getFoodName(); // 음식 이름

            // 계산된 영양소 (스냅샷)
            double snapCal, snapCarbo, snapProtein, snapFat;

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

                // ★ 체크박스 로직: "음식 DB에 저장해주세요" 라고 했나요?
                if (item.isSaveToMyFoods()) {
                    Food newCustomFood = Food.builder()
                            .userId(userId) // 내 음식
                            .foodCode("CUSTOM_" + UUID.randomUUID().toString().substring(0, 8))
                            .foodName(item.getFoodName())
                            .category("사용자등록")
                            .servingSize(item.getQuantity()) // 입력한 양을 1회 제공량으로 기준 잡음
                            .unit(item.getUnit())
                            .calories(item.getCalories())
                            .carbohydrate(item.getCarbohydrate())
                            .protein(item.getProtein())
                            .fat(item.getFat())
                            .build();

                    foodMapper.saveFood(newCustomFood); // DB 저장
                    finalFoodId = newCustomFood.getFoodId(); // 생성된 ID 확보
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
                    .build();

            details.add(detail);

            // 6. 총합 누적
            totalCal += snapCal;
            totalCarbohydrate += snapCarbo;
            totalProtein += snapProtein;
            totalFat += snapFat;
        }

        // 7. 부모(Diet) 엔티티 생성
        Diet diet = Diet.builder()
                .userId(userId)
                .eatDate(dietRequestDto.getEatDate())
                .eatTime(dietRequestDto.getEatTime())
                .mealType(dietRequestDto.getMealType())
                .memo(dietRequestDto.getMemo())
                // 계산된 총합 저장
                .totalCalories(totalCal)
                .totalCarbohydrate(totalCarbohydrate)
                .totalProtein(totalProtein)
                .totalFat(totalFat)
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
                    .build();
            finalDetails.add(completeDetail);
        }

        // 10. 자식 대량 저장
        if (!finalDetails.isEmpty()) {
            dietMapper.insertDietDetails(finalDetails);
        }

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
        if (!diet.getUserId().equals(userId)) {
            throw new CustomException(ErrorResponseCode.UNAUTHORIZED);
        }
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
    public void updateDiet(Long userId, Long dietId, DietRequestDto dietRequestDto) {
        // 1. 합계 변수 초기화
        double totalCal = 0;
        double totalCarbohydrate = 0;
        double totalProtein = 0;
        double totalFat = 0;

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
                    .build();

            newDetails.add(detail);

            // 합계 누적
            totalCal += cal;
            totalCarbohydrate += carbo;
            totalProtein += protein;
            totalFat += fat;
        }

        // 3. 부모(Diet) 정보 업데이트
        Diet dietUpdate = Diet.builder()
                .dietId(dietId)
                .eatTime(dietRequestDto.getEatTime())
                .mealType(dietRequestDto.getMealType())
                .memo(dietRequestDto.getMemo())
                // .dietImgUrl(...) // 이미지 수정 로직이 있다면 추가
                .totalCalories(totalCal)
                .totalCarbohydrate(totalCarbohydrate)
                .totalProtein(totalProtein)
                .totalFat(totalFat)
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
}
