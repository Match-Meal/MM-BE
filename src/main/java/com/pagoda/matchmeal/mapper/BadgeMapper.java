package com.pagoda.matchmeal.mapper;

import com.pagoda.matchmeal.model.entity.Badge;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BadgeMapper {
    List<Badge> findAllByOrderByTierAsc();
    
    // For internal checking logic
    List<Badge> findAll();
}
