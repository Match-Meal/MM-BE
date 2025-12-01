package com.pagoda.matchmeal.integration;

import com.pagoda.matchmeal.mapper.FoodMapper;
import com.pagoda.matchmeal.model.dto.FoodSearchCond;
import com.pagoda.matchmeal.model.dto.response.FoodListResponseDto;
import com.pagoda.matchmeal.model.entity.Food;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // 테스트 끝나고 DB 롤백
class FoodMapperTest {

    @Autowired
    private FoodMapper foodMapper;

    @Test
    @DisplayName("음식 저장 및 상세 조회 테스트")
    void saveAndFindTest() {
        // given
        Food food = Food.builder()
                .userId(1L)
                .foodCode("CODE_001") // ✅ 필수값
                .foodName("테스트 닭가슴살")
                .calories(100.0)
                .build();

        // when
        foodMapper.saveFood(food);

        // then
        assertThat(food.getFoodId()).isNotNull();

        var result = foodMapper.findFoodDetail(food.getFoodId());
        assertThat(result.getFoodName()).isEqualTo("테스트 닭가슴살");
        assertThat(result.getCalories()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("검색 조건 테스트 (Dynamic SQL)")
    void searchTest() {
        // given
        // ✅ 모든 builder에 .foodCode(...)를 추가했습니다.

        // 1. 내 음식
        foodMapper.saveFood(Food.builder()
                .userId(1L)
                .foodCode("MY_APPLE") // 필수!
                .foodName("내 사과")
                .category("과일")
                .build());

        // 2. 남의 음식
        foodMapper.saveFood(Food.builder()
                .userId(2L)
                .foodCode("OTHER_BANANA") // 필수!
                .foodName("남의 바나나")
                .category("과일")
                .build());

        // 3. 공용 음식
        foodMapper.saveFood(Food.builder()
                .userId(null)
                .foodCode("PUBLIC_MILK") // 필수!
                .foodName("공용 우유")
                .category("유제품")
                .build());

        // when 1: '과일' 카테고리 + 내 음식만 보기
        FoodSearchCond cond1 = FoodSearchCond.builder()
                .userId(1L)
                .category("과일")
                .userOnly(true)
                .limit(10)
                .offset(0)
                .build();

        List<FoodListResponseDto> result1 = foodMapper.findFoodList(cond1);

        // then 1
        assertThat(result1).hasSize(1);
        assertThat(result1.get(0).getFoodName()).isEqualTo("내 사과");

        // when 2: '우유' 검색 + 전체 보기 (공용 포함)
        FoodSearchCond cond2 = FoodSearchCond.builder()
                .userId(1L)
                .keyword("우유")
                .userOnly(false)
                .limit(10)
                .offset(0)
                .build();

        List<FoodListResponseDto> result2 = foodMapper.findFoodList(cond2);

        // then 2
        assertThat(result2).hasSize(1);
        assertThat(result2.get(0).getFoodName()).isEqualTo("공용 우유");
    }

    @Test
    @DisplayName("부분 업데이트 테스트 (Dynamic Set)")
    void updateFoodTest() {
        // given
        Food food = Food.builder()
                .userId(1L)
                .foodCode("UPDATE_TEST_CODE") // ✅ 필수값
                .foodName("원래 이름")
                .calories(100.0)
                .build();
        foodMapper.saveFood(food);

        // when
        Food updateParams = Food.builder()
                .foodId(food.getFoodId())
                .foodName(null)
                .calories(200.0)
                .build(); // 업데이트할 때는 foodCode가 없어도 됨 (Mapper XML에서 if test로 거르거나 WHERE 조건에 안 쓰니까)

        foodMapper.updateFood(updateParams);

        // then
        var result = foodMapper.findFoodDetail(food.getFoodId());
        assertThat(result.getFoodName()).isEqualTo("원래 이름");
        assertThat(result.getCalories()).isEqualTo(200.0);
    }
}