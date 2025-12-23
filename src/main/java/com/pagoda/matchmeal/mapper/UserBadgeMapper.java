package com.pagoda.matchmeal.mapper;

import com.pagoda.matchmeal.model.entity.UserBadge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface UserBadgeMapper {
    List<UserBadge> findAllByUserId(Long userId);
    
    Optional<UserBadge> findByUserIdAndBadgeId(@Param("userId") Long userId, @Param("badgeId") Long badgeId);
    
    void save(UserBadge userBadge);
    
    void update(UserBadge userBadge);

    // Helper to check existence
    boolean existsByUserIdAndBadgeId(@Param("userId") Long userId, @Param("badgeId") Long badgeId);
}
