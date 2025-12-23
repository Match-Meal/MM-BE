package com.pagoda.matchmeal.mapper;

import com.pagoda.matchmeal.model.dto.RankingDto;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RankingMapper {

    List<RankingDto> getRanking(@Param("mealType") String mealType);
}
