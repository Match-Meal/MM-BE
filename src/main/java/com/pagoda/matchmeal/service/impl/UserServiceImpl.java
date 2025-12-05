package com.pagoda.matchmeal.service.impl;

import com.pagoda.matchmeal.mapper.UserMapper;
import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.model.dto.UserProfileDto;
import com.pagoda.matchmeal.model.entity.User;
import com.pagoda.matchmeal.model.enums.UserRole;
import com.pagoda.matchmeal.model.enums.UserStatus;
import com.pagoda.matchmeal.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public Map<String, Object> processLoginOrRegister(String socialId, String email, String name, String platform) {
        Map<String, Object> result = new HashMap<>();

        User user = userMapper.findBySocialId(socialId).orElse(null);
        boolean isNew = false;

        if (user == null) {
            // 신규 회원
            isNew = true;
            user = User.builder()
                    .socialId(socialId)
                    .email(email)
                    .userName(name) // 최초 가입시 소셜 이름으로 기본값 설정
                    .platform(platform)
                    .role(UserRole.ROLE_USER)
                    .status(UserStatus.ACTIVE)
                    .build();
            userMapper.save(user);
        }
        result.put("user", user);
        result.put("isNew", isNew);

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getMyProfile(Long userId) {
        // DB 조회
        User user = userMapper.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // Entity -> DTO 변환 (Service 내부에서 처리)
        return convertToDto(user);
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, UserProfileDto profileDto) {
        String allergyStr = convertToString(profileDto.getAllergies());
        String diseaseStr = convertToString(profileDto.getDiseases());

        // 기존 유저 조회
        User user = User.builder()
                .id(userId)
                .userName(profileDto.getUserName())
                .gender(profileDto.getGender())
                .birthDate(profileDto.getBirthDate())
                .heightCm(profileDto.getHeightCm())
                .weightKg(profileDto.getWeightKg())
                .statusMessage(profileDto.getStatusMessage())
                .allergies(allergyStr)
                .diseases(diseaseStr)
                .build();

        userMapper.updateProfile(user);
    }

    public UserDto getUserProfile(Long targetUserId) {
        User targetUser = userMapper.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (!targetUser.getIsPublic()) {
            return UserDto.builder()
                    .userName(targetUser.getUserName())
                    .statusMessage("비공개 프로필입니다.")
                    .build();
        }

        return convertToDto(targetUser);
    }

    private List<String> convertToList(String str) {
        if (!StringUtils.hasText(str)) return Collections.emptyList();
        return Arrays.stream(str.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    // List["땅콩", "우유"] -> DB의 "땅콩,우유"
    private String convertToString(List<String> list) {
        if (list == null || list.isEmpty()) return "";
        return String.join(",", list);
    }

    private UserDto convertToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .userName(user.getUserName())
                .socialId(user.getSocialId())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .statusMessage(user.getStatusMessage())
                .gender(user.getGender())
                .birthDate(user.getBirthDate())
                .heightCm(user.getHeightCm())
                .weightKg(user.getWeightKg())
                // 문자열(DB) -> 리스트(DTO) 변환 헬퍼 사용
                .allergies(convertToList(user.getAllergies()))
                .diseases(convertToList(user.getDiseases()))
                .build();
    }


}
