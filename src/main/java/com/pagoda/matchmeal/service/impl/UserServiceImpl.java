package com.pagoda.matchmeal.service.impl;

import com.pagoda.matchmeal.common.exception.CustomException;
import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import com.pagoda.matchmeal.mapper.FollowMapper;
import com.pagoda.matchmeal.mapper.UserMapper;
import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.model.dto.UserProfileDto;
import com.pagoda.matchmeal.model.entity.User;
import com.pagoda.matchmeal.model.enums.UserRole;
import com.pagoda.matchmeal.model.enums.UserStatus;
import com.pagoda.matchmeal.service.S3Service;
import com.pagoda.matchmeal.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final S3Service s3Service;
    private final FollowMapper followMapper;

    @Override
    @Transactional
    public Map<String, Object> processLoginOrRegister(String socialId, String email, String name, String platform, String picture, String restartType) {
        Map<String, Object> result = new HashMap<>();

        User user = userMapper.findBySocialId(socialId).orElse(null);
        boolean isNew = false;

        // 유저가 존재하고, 탈퇴한 상태인 경우 처리 로직
        if (user != null && user.getDeletedAt() != null) {
            // 유예기간이 지났는지 먼저 체크(지났으면 무조건 삭제 후 신규 가입
            LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
            if (user.getDeletedAt().isBefore(threeMonthsAgo)) {
                userMapper.hardDeleteUserById(user.getUserId());
                user = null; // 신규 가입 로직으로 넘기기 위해 null 처리
            } else {
                // 유예기간 이내인 경우 -> 사용자 선택 확인

                if (restartType == null) {
                    // 선택 없이 처음
                    throw new CustomException(ErrorResponseCode.USER_WITHDRAWN_WAITING);
                }

                if ("RESTORE".equals(restartType)) {
                    // 복구하기
                    userMapper.restoreUser(user.getUserId());
                    user.setDeletedAt(null);
                    user.setStatus(UserStatus.ACTIVE);
                } else if ("RESET".equals(restartType)) {
                    // 새로 만들기(기존 데이터 삭제)
                    // 연관된 테이블이 있다면 Casecade 설정
                    userMapper.hardDeleteUserById(user.getUserId());
                    user = null; // 아래 신규 가입 로직을 타게 만듬
                }
            }
        }

        // 신규 가입 (아예 데이터가 없거나 RESET을 통해 user가 null인 경우
        if (user == null) {
            // 신규 회원
            isNew = true;
            user = User.builder()
                    .socialId(socialId)
                    .email(email)
                    .userName(name) // 최초 가입시 소셜 이름으로 기본값 설정
                    .platform(platform)
                    .profileImage(picture)
                    .role(UserRole.ROLE_USER)
                    .status(UserStatus.ACTIVE)
                    .isPublic(true)
                    .build();
            userMapper.save(user);
        }
        
        // 결과 반환(JWT 토큰 생성 등을 위한 정보
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
        User user = userMapper.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorResponseCode.USER_NOT_FOUND));

        if (user.getDeletedAt() != null) {
            throw new CustomException(ErrorResponseCode.ALREADY_USER_DELETE);
        }

        userMapper.softDeleteUser(userId);
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
