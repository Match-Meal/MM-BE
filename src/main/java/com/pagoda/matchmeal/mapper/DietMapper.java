package com.pagoda.matchmeal.mapper;

import com.pagoda.matchmeal.model.dto.response.DailyDietStatDto;
import com.pagoda.matchmeal.model.dto.response.DietResponseDto;
import com.pagoda.matchmeal.model.entity.Diet;
import com.pagoda.matchmeal.model.entity.DietDetail;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 식단(Diet) 관련 데이터베이스 접근 계층 (MyBatis Mapper)
 */
public interface DietMapper {

    /**
     * 식단(부모) 정보를 저장합니다.
     *
     * @param diet diet 저장할 식단 엔티티 (저장 후 dietId가 채워짐)
     */
    void insertDiet(Diet diet);

    /**
     * 식단 상세(자식) 음식 리스트를 대량으로 저장합니다. (Bulk Insert)
     *
     * @param dietDetails details 저장할 상세 음식 리스트
     */
    void insertDietDetails(List<DietDetail> dietDetails);

    /**
     * 특정 날짜의 식단 목록을 조회합니다. (데일리 조회)
     *
     * @param userId  사용자 ID
     * @param eatDate 조회할 날짜 (YYYY-MM-DD 문자열)
     * @return 해당 날짜의 식단 리스트 (아침, 점심, 저녁 등)
     */
    List<DietResponseDto> findAllByDate(Long userId, String eatDate);

    /**
     * 식단 ID로 상세 정보를 단건 조회
     *
     * @param dietId dietId 식단 ID
     * @return 식단 상세 정보 (음식 리스트 포함)
     */
    DietResponseDto findDietByDietId(Long dietId);

    /**
     * 식단(부모)의 기본 정보를 수정
     *
     * @param diet diet 수정할 정보가 담긴 식단 객체
     */
    void updateDiet(Diet diet);

    /**
     * 특정 식단에 포함된 모든 상세(음식) 데이터를 삭제
     * (기존 음식 목록을 싹 지우고 새로 끼워 넣는 전략)
     *
     * @param dietId 부모 식단 ID
     */
    void deleteDietDetailByDietId(Long dietId);

    /**
     * 식단을 삭제합니다.
     *
     * @param dietId 삭제할 식단 ID
     */
    void deleteDietByDietId(Long dietId);

    // 날짜별 통계 조회
    List<DailyDietStatDto> getDailyDietStats(@Param("userId") Long userId,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

//    /**
//     * 특정 날짜의 식단 목록을 조회합니다. (기간 조회)
//     *
//     * @param userId
//     * @param startDate
//     * @param endDate
//     * @return 해당 기간의 식단 리스트
//     */
//    List<DietResponseDto> findAllByPeriod(
//            @Param("userId") Long userId,
//            @Param("startDate") String startDate,
//            @Param("endDate") String endDate
//    );
}
