package com.pagoda.matchmeal.service;

import com.pagoda.matchmeal.mapper.BadgeMapper;
import com.pagoda.matchmeal.mapper.UserBadgeMapper;
import com.pagoda.matchmeal.model.dto.response.BadgeResponseDto;
import com.pagoda.matchmeal.model.entity.Badge;
import com.pagoda.matchmeal.model.entity.UserBadge;
import com.pagoda.matchmeal.model.enums.BadgeCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BadgeService {

    private final BadgeMapper badgeMapper;
    private final UserBadgeMapper userBadgeMapper;

    /**
     * 내 뱃지 컬렉션 조회
     */
    @Transactional(readOnly = true)
    public Map<BadgeCategory, List<BadgeResponseDto>> getMyBadges(Long userId) {
        // 1. 모든 뱃지 메타데이터 조회
        List<Badge> allBadges = badgeMapper.findAllByOrderByTierAsc();

        // 2. 유저의 현재 진행 상황 조회
        List<UserBadge> userBadges = userBadgeMapper.findAllByUserId(userId);
        Map<Long, UserBadge> userBadgeMap = userBadges.stream()
                .collect(Collectors.toMap(ub -> ub.getBadge().getBadgeId(), ub -> ub));

        // 3. DTO 변환 및 카테고리별 그룹화
        Map<BadgeCategory, List<BadgeResponseDto>> result = new HashMap<>();

        for (Badge badge : allBadges) {
            UserBadge ub = userBadgeMap.get(badge.getBadgeId());
            boolean isAcquired = ub != null && ub.isAcquired();
            int currentVal = ub != null ? ub.getCurrentValue() : 0;
            
            // 이미지는 획득 여부에 따라 컬러/흑백 선택
            String displayImage = isAcquired ? badge.getImageUrl() : badge.getGrayImageUrl();

            BadgeResponseDto dto = BadgeResponseDto.builder()
                    .badgeId(badge.getBadgeId())
                    .name(badge.getName())
                    .description(badge.getDescription())
                    .imageUrl(displayImage)
                    .isAcquired(isAcquired)
                    .currentValue(currentVal)
                    .targetValue(badge.getTargetValue())
                    .tier(badge.getTier())
                    .build();

            result.computeIfAbsent(badge.getCategory(), k -> new ArrayList<>()).add(dto);
        }

        return result;
    }
    
    /**
     * [이벤트 처리용] 뱃지 조건 체크 및 지급
     */
    @Transactional
    public void checkBadgeCondition(Long userId, BadgeCategory category, String subCategory, int newValue) {
        // 해당 카테고리/서브카테고리의 모든 뱃지 조회 (실제론 DB 쿼리 최적화 필요)
        List<Badge> allBadges = badgeMapper.findAll();
        List<Badge> targetBadges = allBadges.stream()
                .filter(b -> b.getCategory() == category && b.getSubCategory().equals(subCategory))
                .toList();

        for (Badge badge : targetBadges) {
            Optional<UserBadge> existing = userBadgeMapper.findByUserIdAndBadgeId(userId, badge.getBadgeId());
            
            UserBadge userBadge;
            boolean isNew = false;

            if (existing.isPresent()) {
                userBadge = existing.get();
            } else {
                userBadge = UserBadge.builder()
                        .userId(userId)
                        .badge(badge)
                        .currentValue(0)
                        .isAcquired(false)
                        .build();
                isNew = true;
            }
            
            // 값 업데이트
            if (newValue > userBadge.getCurrentValue()) {
                userBadge.setCurrentValue(newValue);
            }

            // 목표 달성 체크 (이미 획득한 경우 제외)
            if (!userBadge.isAcquired() && userBadge.getCurrentValue() >= badge.getTargetValue()) {
                userBadge.setAcquired(true);
                userBadge.setAcquiredAt(LocalDateTime.now());
                log.info("🎉 Badge Acquired! User: {}, Badge: {}", userId, badge.getName());
                // TODO: 알림 전송 로직 추가: notificationService.send(...)
            }
            
            if (isNew) {
                userBadgeMapper.save(userBadge);
            } else {
                userBadgeMapper.update(userBadge);
            }
        }
    }
}
