package com.pagoda.matchmeal.mapper;

import com.pagoda.matchmeal.model.entity.Food;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FoodBatchMapper {
    /**
     * 음식 데이터를 DB에 저장하거나 업데이트합니다.
     * - XML의 <insert id="insertFood"> 쿼리를 실행합니다.
     * - ON DUPLICATE KEY UPDATE 구문을 통해 중복 시 갱신 처리합니다.
     *
     * @param food 저장할 음식 엔티티
     */
    void insertFood(Food food);
}
