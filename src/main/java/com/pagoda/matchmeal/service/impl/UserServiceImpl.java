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

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final S3Service s3Service;
    private final FollowMapper followMapper;
    private final SocialUnlinkService socialUnlinkService;
    private final RedisService redisService;

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
    public void updateProfile(Long userId, UserProfileDto profileDto, MultipartFile imageFile) {

        User existUser = userMapper.findById(userId).orElseThrow(() -> new CustomException(ErrorResponseCode.USER_NOT_FOUND));

        String profileImageUrl = existUser.getProfileImage();

        // 새 이미지가 업로드된 경우
        if (imageFile != null && !imageFile.isEmpty()) {
            // (선택) 기존 이미지가 있다면 S3에서 삭제 (구글 기본 이미지가 아닐 경우만)
            if (StringUtils.hasText(profileImageUrl) && !profileImageUrl.startsWith("amazonaws.com")) {
                s3Service.deleteFile(profileImageUrl); // 삭제 메서드 구현 필요
            }

            // 새 파일 업로드
            profileImageUrl = s3Service.uploadFile(imageFile, "profile");
        }

        String allergyStr = convertToString(profileDto.getAllergies());
        String diseaseStr = convertToString(profileDto.getDiseases());

        // 기존 유저 조회
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

    @Override
    @Transactional
    public void updateVisibility(Long userId, boolean isPublic) {
        User user = User.builder()
                .userId(userId)
                .isPublic(isPublic)
                .build();

        userMapper.updateVisibility(user);
    }


    @Override
    @Transactional
    public UserDto getUserProfile(Long targetUserId) {
        User targetUser = userMapper.findById(targetUserId)
                .orElseThrow(() -> new CustomException(ErrorResponseCode.USER_NOT_FOUND));

        if (Boolean.FALSE.equals(targetUser.getIsPublic())) {
            // 비공개여도 이름, 사진, 팔로우 숫자는 보여줘야함
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

    @Override
    @Transactional
    public void withdrawUser(Long userId) {
        // 1. 유저 조회
        User user = userMapper.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorResponseCode.USER_NOT_FOUND));

        // 2. 이미 탈퇴한 유저인지 체크
        if (user.getDeletedAt() != null) {
            throw new CustomException(ErrorResponseCode.ALREADY_USER_DELETE);
        }

        // 3. 소셜 연결 끊기 (옵션)
        // JWT 환경에서는 AccessToken을 서버가 가지고 있지 않으므로,
        // DB에 있는 socialId와 platform 정보를 넘겨서 처리합니다.
        // (SocialUnlinkService 구현체에서 Admin Key를 쓰거나 로직을 수정해야 할 수 있음)
        if (StringUtils.hasText(user.getPlatform()) && StringUtils.hasText(user.getSocialId())) {
            try {
                // [변경] accessToken 대신 socialId를 넘기는 방식으로 변경하거나,
                // 만약 accessToken이 필수라면 이 부분은 스킵하고 로컬 탈퇴만 진행해야 합니다.
                socialUnlinkService.unlink(user.getPlatform(), user.getSocialId());
            } catch (Exception e) {
                // 외부 API 연동 실패가 우리 DB 탈퇴를 막으면 안 됨 -> 로그만 찍고 진행
                log.warn("소셜 연동 해제 실패 (진행은 계속함). userId: {}, error: {}", userId, e.getMessage());
            }
        }

        // 4. Redis Refresh Token 삭제 (강제 로그아웃)
        redisService.deleteValues("RT:" + userId);

        // 5. DB Soft Delete (핵심)
        userMapper.softDeleteUser(userId);

        log.info("회원 탈퇴 완료 (Soft Delete). UserID: {}", userId);
    }

    @Override
    public UserDto convertUserToDto(User user) {
        // 기존의 private 메서드를 호출하거나, 로직을 여기로 옮기면 됩니다.
        return convertToDto(user);
    }

    // ---------------- Helper Methods -----------------

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
        // 팔로우/팔로잉 숫자 조회
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
                // 문자열(DB) -> 리스트(DTO) 변환 헬퍼 사용
                .allergies(convertToList(user.getAllergies()))
                .diseases(convertToList(user.getDiseases()))
                .isPublic(user.getIsPublic())
                .followerCount(followerCount)
                .followingCount(followingCount)
                .build();
    }
}
