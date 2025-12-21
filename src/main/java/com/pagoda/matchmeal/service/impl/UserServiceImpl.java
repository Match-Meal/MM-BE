package com.pagoda.matchmeal.service.impl;

import com.pagoda.matchmeal.common.exception.CustomException;
import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import com.pagoda.matchmeal.mapper.FollowMapper;
import com.pagoda.matchmeal.mapper.UserMapper;
import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.model.dto.UserProfileDto;
import com.pagoda.matchmeal.model.entity.User;
import com.pagoda.matchmeal.service.RedisService;
import com.pagoda.matchmeal.service.S3Service;
import com.pagoda.matchmeal.service.SocialUnlinkService;
import com.pagoda.matchmeal.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 회원 비즈니스 로직 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final S3Service s3Service;
    private final FollowMapper followMapper;
    private final SocialUnlinkService socialUnlinkService;
    private final RedisService redisService;

    /**
     * 내 프로필 상세 조회
     *
     * @param userId 로그인한 사용자 PK
     * @return 사용자 상세 정보 DTO
     */
    @Override
    public UserDto getMyProfile(Long userId) {
        User user = userMapper.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        return convertToDto(user);
    }

    /**
     * 프로필 정보 수정
     *
     * @param userId     수정할 사용자 PK
     * @param profileDto 수정할 텍스트 정보(닉네임, 키, 몸무게 등)가 담긴 DTO
     * @param imageFile  새로 교체할 프로필 이미지 파일 (null일 경우 이미지 변경 없음)
     */
    @Override
    @Transactional
    public void updateProfile(Long userId, UserProfileDto profileDto, MultipartFile imageFile) {
        User existUser = userMapper.findById(userId).orElseThrow(() -> new CustomException(ErrorResponseCode.USER_NOT_FOUND));
        String profileImageUrl = existUser.getProfileImage();

        if (imageFile != null && !imageFile.isEmpty()) {
            if (StringUtils.hasText(profileImageUrl) && !profileImageUrl.startsWith("amazonaws.com")) {
                s3Service.deleteFile(profileImageUrl);
            }
            profileImageUrl = s3Service.uploadFile(imageFile, "profile");
        }

        String allergyStr = convertToString(profileDto.getAllergies());
        String diseaseStr = convertToString(profileDto.getDiseases());

        User user = User.builder()
                .userId(userId)
                .userName(profileDto.getUserName())
                .gender(profileDto.getGender())
                .birthDate(profileDto.getBirthDate())
                .heightCm(profileDto.getHeightCm())
                .weightKg(profileDto.getWeightKg())
                .statusMessage(profileDto.getStatusMessage())
                .profileImage(profileImageUrl)
                .allergies(allergyStr)
                .diseases(diseaseStr)
                .build();

        userMapper.updateProfile(user);
    }

    /**
     * 프로필 공개/비공개 전환
     *
     * @param userId   사용자 PK
     * @param isPublic true: 공개, false: 비공개
     */
    @Override
    @Transactional
    public void updateVisibility(Long userId, boolean isPublic) {
        User user = User.builder()
                .userId(userId)
                .isPublic(isPublic)
                .build();
        userMapper.updateVisibility(user);
    }

    /**
     * 상대방 프로필 조회
     *
     * @param targetUserId 조회할 상대방 사용자 PK
     * @return 공개 여부에 따라 필터링된 사용자 정보 DTO
     */
    @Override
    @Transactional
    public UserDto getUserProfile(Long targetUserId) {
        User targetUser = userMapper.findById(targetUserId)
                .orElseThrow(() -> new CustomException(ErrorResponseCode.USER_NOT_FOUND));

        if (Boolean.FALSE.equals(targetUser.getIsPublic())) {
            Long followerCount = followMapper.countFollowers(targetUserId);
            Long followingCount = followMapper.countFollowings(targetUserId);

            return UserDto.builder()
                    .userName(targetUser.getUserName())
                    .profileImage(targetUser.getProfileImage())
                    .statusMessage("비공개 프로필입니다.")
                    .followerCount(followerCount)
                    .followingCount(followingCount)
                    .build();
        }
        return convertToDto(targetUser);
    }

    /**
     * 회원 탈퇴 처리
     *
     * @param userId 탈퇴할 사용자 PK
     */
    @Override
    @Transactional
    public void withdrawUser(Long userId) {
        User user = userMapper.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorResponseCode.USER_NOT_FOUND));

        if (user.getDeletedAt() != null) {
            throw new CustomException(ErrorResponseCode.ALREADY_USER_DELETE);
        }

        if (StringUtils.hasText(user.getPlatform()) && StringUtils.hasText(user.getSocialId())) {
            try {
                socialUnlinkService.unlink(user.getPlatform(), user.getSocialId());
            } catch (Exception e) {
                log.warn("소셜 연동 해제 실패 (진행은 계속함). userId: {}, error: {}", userId, e.getMessage());
            }
        }

        redisService.deleteValues("RT:" + userId);
        userMapper.softDeleteUser(userId);
        log.info("회원 탈퇴 완료 (Soft Delete). UserID: {}", userId);
    }

    @Override
    public UserDto convertUserToDto(User user) {
        return convertToDto(user);
    }

    // --- Helper Methods (Private) ---

    private List<String> convertToList(String str) {
        if (!StringUtils.hasText(str)) return Collections.emptyList();
        return Arrays.stream(str.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    private String convertToString(List<String> list) {
        if (list == null || list.isEmpty()) return "";
        return String.join(",", list);
    }

    private UserDto convertToDto(User user) {
        Long followerCount = followMapper.countFollowers(user.getUserId());
        Long followingCount = followMapper.countFollowings(user.getUserId());

        return UserDto.builder()
                .id(user.getUserId())
                .userName(user.getUserName())
                .socialId(user.getSocialId())
                .email(user.getEmail())
                .profileImage(user.getProfileImage())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .statusMessage(user.getStatusMessage())
                .gender(user.getGender())
                .birthDate(user.getBirthDate())
                .heightCm(user.getHeightCm())
                .weightKg(user.getWeightKg())
                .allergies(convertToList(user.getAllergies()))
                .diseases(convertToList(user.getDiseases()))
                .isPublic(user.getIsPublic())
                .followerCount(followerCount)
                .followingCount(followingCount)
                .build();
    }
}