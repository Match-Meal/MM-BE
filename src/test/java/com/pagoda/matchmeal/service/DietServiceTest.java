package com.pagoda.matchmeal.service;


import com.pagoda.matchmeal.mapper.DietMapper;
import com.pagoda.matchmeal.mapper.FoodMapper;
import com.pagoda.matchmeal.model.dto.request.DietRequestDto;
import com.pagoda.matchmeal.model.entity.Diet;
import com.pagoda.matchmeal.model.entity.DietDetail;
import com.pagoda.matchmeal.model.entity.Food;
import com.pagoda.matchmeal.model.enums.MealType;
import com.pagoda.matchmeal.service.impl.DietServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Mockito 사용 설정
class DietServiceTest {

    @InjectMocks
    private DietServiceImpl dietService;

    @Mock
    private DietMapper dietMapper;

    @Mock
    private FoodMapper foodMapper;

    @Test
    @DisplayName("[Case 1] 기존 음식 선택 시 - 비율 계산 및 스냅샷 저장 검증")
    void recordDiet_ExistingFood() {
        // given
        Long userId = 1L;
        Long foodId = 100L;

        // DB에 있는 음식 Mock (기준: 100g당 100kcal)
        Food mockFood = Food.builder()
                .foodId(foodId)
                .foodName("공기밥")
                .servingSize(100.0)
                .calories(100.0)
                .carbohydrate(20.0).protein(2.0).fat(1.0)
                .build();

        given(foodMapper.findById(foodId)).willReturn(mockFood);

        // 요청: 200g을 먹음 (비율 2.0배가 되어야 함)
        DietRequestDto.DietDetailRequestDto detailRequest = new DietRequestDto.DietDetailRequestDto();
        detailRequest.setFoodId(foodId);
        detailRequest.setQuantity(200.0); // 2배
        detailRequest.setUnit("g");

        DietRequestDto request = createBaseRequest(Collections.singletonList(detailRequest));

        // insertDiet 호출 시 dietId를 555L로 세팅해주는 척(Mock) 하기
        doAnswer(invocation -> {
            Diet diet = invocation.getArgument(0);
            ReflectionTestUtils.setField(diet, "dietId", 555L);
            return null;
        }).when(dietMapper).insertDiet(any(Diet.class));

        // when
        Long resultDietId = dietService.recordDiet(userId, request);

        // then
        assertThat(resultDietId).isEqualTo(555L);

        // ★ 핵심 검증: insertDietDetails에 넘어간 값이 올바르게 계산되었는가?
        ArgumentCaptor<List<DietDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(dietMapper).insertDietDetails(captor.capture());

        DietDetail savedDetail = captor.getValue().get(0);

        // 부모 ID가 잘 들어갔나?
        assertThat(savedDetail.getDietId()).isEqualTo(555L);
        // 음식 ID가 잘 들어갔나?
        assertThat(savedDetail.getFoodId()).isEqualTo(foodId);
        // 칼로리가 2배(200kcal)로 계산되었나? (100g당 100kcal * 200g)
        assertThat(savedDetail.getCalories()).isEqualTo(200.0);
    }

    @Test
    @DisplayName("[Case 2] 직접 입력 + 저장(O) - 새 음식 등록 후 ID 연결 검증")
    void recordDiet_CustomFood_Save() {
        // given
        Long userId = 1L;

        // 요청: 직접 입력 + saveToMyFoods = true
        DietRequestDto.DietDetailRequestDto detailRequest = new DietRequestDto.DietDetailRequestDto();
        detailRequest.setFoodName("엄마표 제육");
        detailRequest.setQuantity(150.0);
        detailRequest.setUnit("g");
        detailRequest.setCalories(300.0); // 입력한 영양소
        detailRequest.setSaveToMyFoods(true); // ★ 저장 체크!

        DietRequestDto request = createBaseRequest(Collections.singletonList(detailRequest));

        // foodMapper.save()가 호출되면 foodId를 777L로 세팅해주는 척 하기
        doAnswer(invocation -> {
            Food food = invocation.getArgument(0);
            ReflectionTestUtils.setField(food, "foodId", 777L);
            return null;
        }).when(foodMapper).saveFood(any(Food.class));

        // when
        dietService.recordDiet(userId, request);

        // then
        // 1. Food 저장이 호출되었는가?
        verify(foodMapper, times(1)).saveFood(any(Food.class));

        // 2. DietDetail에 방금 만든 ID(777L)가 들어갔는가?
        ArgumentCaptor<List<DietDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(dietMapper).insertDietDetails(captor.capture());

        DietDetail savedDetail = captor.getValue().get(0);
        assertThat(savedDetail.getFoodId()).isEqualTo(777L); // ID 연결 확인
        assertThat(savedDetail.getFoodName()).isEqualTo("엄마표 제육");
    }

    @Test
    @DisplayName("[Case 3] 직접 입력 + 저장(X) - foodId가 null인지 검증")
    void recordDiet_CustomFood_NoSave() {
        // given
        Long userId = 1L;

        // 요청: 직접 입력 + saveToMyFoods = false
        DietRequestDto.DietDetailRequestDto detailRequest = new DietRequestDto.DietDetailRequestDto();
        detailRequest.setFoodName("길거리 떡볶이");
        detailRequest.setQuantity(100.0);
        detailRequest.setSaveToMyFoods(false); // ★ 저장 안 함!

        DietRequestDto request = createBaseRequest(Collections.singletonList(detailRequest));

        // when
        dietService.recordDiet(userId, request);

        // then
        // 1. Food 저장이 호출되면 안 됨!
        verify(foodMapper, never()).saveFood(any(Food.class));

        // 2. DietDetail의 foodId는 null이어야 함
        ArgumentCaptor<List<DietDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(dietMapper).insertDietDetails(captor.capture());

        DietDetail savedDetail = captor.getValue().get(0);
        assertThat(savedDetail.getFoodId()).isNull(); // Null 확인
        assertThat(savedDetail.getFoodName()).isEqualTo("길거리 떡볶이");
    }

    // 테스트용 DTO 생성 헬퍼 메소드
    private DietRequestDto createBaseRequest(List<DietRequestDto.DietDetailRequestDto> foods) {
        DietRequestDto request = new DietRequestDto();
        request.setEatDate(LocalDate.now());
        request.setEatTime(LocalTime.now());
        request.setMealType(MealType.LUNCH);
        request.setFoods(foods);
        return request;
    }
}