package com.pagoda.matchmeal.service;

import com.pagoda.matchmeal.common.exception.CustomException;
import com.pagoda.matchmeal.mapper.FoodMapper;
import com.pagoda.matchmeal.model.dto.request.FoodRequestDto;
import com.pagoda.matchmeal.model.dto.response.FoodDetailResponseDto;
import com.pagoda.matchmeal.model.entity.Food;
import com.pagoda.matchmeal.service.impl.FoodServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FoodServiceTest {

    @InjectMocks
    private FoodServiceImpl foodService;

    @Mock
    private FoodMapper foodMapper;

    @Test
    @DisplayName("음식 추가 성공 테스트")
    void addFood_Success() {
        // given
        Long userId = 1L;
        FoodRequestDto req = new FoodRequestDto();
        // Reflection으로 DTO 값 주입 (Setter가 없으므로)
        ReflectionTestUtils.setField(req, "foodName", "닭가슴살");
        ReflectionTestUtils.setField(req, "category", "육류");

        // saveFood 호출 시 아무 동작 안함 (void)
        // MyBatis insert 후 keyProperty로 ID가 세팅되지만, Mock에서는 직접 세팅 어려우므로
        // Service 로직이 Mapper를 호출하는지 검증 위주로 진행

        // when
        Long resultId = foodService.addFood(userId, req);

        // then
        // Service코드에서 food.getFoodId()를 반환하는데, 
        // Mock객체가 ID를 세팅해주지 않으면 null이 반환될 수 있음. 
        // verify를 통해 saveFood가 실행되었는지 확인하는 것이 핵심
        verify(foodMapper).saveFood(any(Food.class));
    }

    @Test
    @DisplayName("음식 상세 조회 - 성공 (내 음식 조회)")
    void getFoodDetail_Success() {
        // given
        Long foodId = 100L;
        Long userId = 1L;

        FoodDetailResponseDto mockDto = FoodDetailResponseDto.builder()
                .foodId(foodId)
                .userId(userId) // 작성자 ID와 요청자 ID 일치
                .foodName("테스트 음식")
                .build();

        given(foodMapper.findFoodDetail(foodId)).willReturn(mockDto);

        // when
        FoodDetailResponseDto result = foodService.getFoodDetail(foodId, userId);

        // then
        assertThat(result.getFoodName()).isEqualTo("테스트 음식");
        assertThat(result.isMine()).isTrue(); // isMine 세팅 확인
    }

    @Test
    @DisplayName("음식 상세 조회 - 실패 (권한 없음: 남의 비공개 음식)")
    void getFoodDetail_Fail_Unauthorized() {
        // given
        Long foodId = 100L;
        Long userId = 1L;      // 요청자
        Long ownerId = 2L;     // 작성자 (다름)

        FoodDetailResponseDto mockDto = FoodDetailResponseDto.builder()
                .foodId(foodId)
                .userId(ownerId)
                .build();

        given(foodMapper.findFoodDetail(foodId)).willReturn(mockDto);

        // when & then
        assertThatThrownBy(() -> foodService.getFoodDetail(foodId, userId))
                .isInstanceOf(CustomException.class); // 예외 발생 검증
    }

    @Test
    @DisplayName("음식 수정 - 실패 (권한 없음)")
    void updateFood_Fail_Unauthorized() {
        // given
        Long foodId = 100L;
        Long userId = 1L;
        Long ownerId = 2L;
        FoodRequestDto req = new FoodRequestDto();

        FoodDetailResponseDto existingFood = FoodDetailResponseDto.builder()
                .foodId(foodId)
                .userId(ownerId) // 작성자가 다름
                .build();

        given(foodMapper.findFoodDetail(foodId)).willReturn(existingFood);

        // when & then
        assertThatThrownBy(() -> foodService.updateFood(userId, foodId, req))
                .isInstanceOf(CustomException.class);
    }
}