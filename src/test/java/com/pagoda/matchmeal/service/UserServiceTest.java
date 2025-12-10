package com.pagoda.matchmeal.service;

import com.pagoda.matchmeal.common.exception.CustomException;
import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import com.pagoda.matchmeal.mapper.UserMapper;
import com.pagoda.matchmeal.model.dto.UserDto;
import com.pagoda.matchmeal.model.dto.UserProfileDto;
import com.pagoda.matchmeal.model.entity.User;
import com.pagoda.matchmeal.model.enums.UserRole;
import com.pagoda.matchmeal.service.impl.UserServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("신규 회원이면 save가 호출되어야 함")
    void processLoginNewUserTest() {
        // given
        String socialId = "12345";
        String email = "test@gmail.com";
        String pictureUrl = "https://google.com/picture.jpg";

        given(userMapper.findBySocialId(socialId)).willReturn(Optional.empty());

        // when
        Map<String, Object> result = userService.processLoginOrRegister(socialId, email, "테스트유저", "google", pictureUrl);

        // then
        verify(userMapper, times(1)).save(argThat(user ->
                user.getProfileImage().equals(pictureUrl) // 이미지가 잘 들어갔는지 확인
        ));

        assertThat(result.get("isNew")).isEqualTo(true);
        assertThat(((User) result.get("user")).getSocialId()).isEqualTo(socialId);
    }

    @Test
    @DisplayName("내 정보 조회 시 문자열로 저장된 알레르기가 리스트로 변환되어야 함")
    void getMyProfileConversionTest() {
        // given
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .allergies("땅콩,우유") // DB 값
                .role(UserRole.ROLE_USER)
                .isPublic(true)
                .build();

        given(userMapper.findById(userId)).willReturn(Optional.of(user));

        // when
        UserDto result = userService.getMyProfile(userId);

        // then
        assertThat(result.getAllergies()).hasSize(2);
        assertThat(result.getAllergies()).contains("땅콩", "우유");
        assertThat(result.getRole()).isEqualTo(UserRole.ROLE_USER.name());
        assertThat(result.getIsPublic()).isEqualTo(true);
    }

    @Test
    @DisplayName("타인 프로필 조회 - 공개 계정일 경우 모든 정보 반환")
    void getUserProfile_Public() {
        // given
        Long targetId = 2L;
        User targetUser = User.builder()
                .id(targetId)
                .userName("공개유저")
                .isPublic(true)
                .heightCm(180.0)
                .role(UserRole.ROLE_USER)
                .build();

        given(userMapper.findById(targetId)).willReturn(Optional.of(targetUser));

        // when
        UserDto result = userService.getUserProfile(targetId);

        // then
        assertThat(result.getUserName()).isEqualTo("공개유저");
        assertThat(result.getHeightCm()).isEqualTo(180.0);
    }

    @Test
    @DisplayName("타인 프로필 조회 - 비공개 계정일 경우 민감 정보 제외")
    void getUserProfile_Private() {
        // given
        Long targetId = 3L;
        User targetUser = User.builder()
                .id(targetId)
                .userName("비공개유저")
                .isPublic(false) // 비공개
                .heightCm(180.0)
                .role(UserRole.ROLE_USER)
                .build();

        given(userMapper.findById(targetId)).willReturn(Optional.of(targetUser));

        // when
        UserDto result = userService.getUserProfile(targetId);

        // then
        assertThat(result.getUserName()).isEqualTo("비공개유저");
        assertThat(result.getStatusMessage()).isEqualTo("비공개 프로필입니다.");
        assertThat(result.getHeightCm()).isNull(); // 민감 정보는 null이어야 함
    }

    @Test
    @DisplayName("존재하지 않는 유저 조회 시 CustomException 발생")
    void getUserProfile_NotFound() {
        Long invalidId = 999L;
        given(userMapper.findById(invalidId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserProfile(invalidId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorResponseCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("프로필 공개 여부 업데이트")
    void updateVisibilityTest() {
        Long userId = 1L;
        boolean isPublic = false;

        userService.updateVisibility(userId, isPublic);

        verify(userMapper, times(1)).updateVisibility(any(User.class));
    }

    @Test
    @DisplayName("프로필 업데이트 - 이미지 파일이 있을 경우 S3 업로드 후 URL 갱신")
    void updateProfile_WithImage() {
        //given
        Long userId = 1L;
        UserProfileDto dto = new UserProfileDto();
        dto.setUserName("수정된이름");
        dto.setAllergies(List.of("오이"));

        // Mock 파일
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test image content".getBytes());
        String s3Url = "https://s3.aws.com/profile/uuid_test.jpg";

        User existingUser = User.builder()
                .id(userId)
                .profileImage("old_url")
                .build();

        given(userMapper.findById(userId)).willReturn(Optional.of(existingUser));
        given(s3Service.uploadFile(file, "profile")).willReturn(s3Url);

        // when
        userService.updateProfile(userId, dto, file);

        // then
        verify(s3Service, times(1)).uploadFile(file, "profile"); // 업로드 호출 확인
        verify(userMapper).updateProfile(argThat(user ->
                user.getProfileImage().equals(s3Url) && // URL이 새것으로 바뀌었는지
                        user.getUserName().equals("수정된이름")
        ));
    }

    @Test
    @DisplayName("프로필 업데이트 - 이미지 파일이 없으면 기존 이미지 유지")
    void updateProfile_NoImage() {
        // given
        Long userId = 1L;
        UserProfileDto dto = new UserProfileDto();
        dto.setUserName("이름만수정");

        User existingUser = User.builder()
                .id(userId)
                .profileImage("original_url.jpg")
                .build();

        given(userMapper.findById(userId)).willReturn(Optional.of(existingUser));

        // when
        userService.updateProfile(userId, dto, null); // 파일 null 전달

        // then
        verify(s3Service, never()).uploadFile(any(), any(String.class)); // S3 호출되면 안 됨
        verify(userMapper).updateProfile(argThat(user ->
                user.getProfileImage().equals("original_url.jpg") // 기존 URL 유지 확인
        ));
    }

}
