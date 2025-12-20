package com.pagoda.matchmeal.service;

import com.pagoda.matchmeal.common.exception.CustomException;
import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import com.pagoda.matchmeal.mapper.ChallengeMapper;
import com.pagoda.matchmeal.model.dto.ChallengeSearchCondition;
import com.pagoda.matchmeal.model.dto.request.ChallengeCreateRequestDto;
import com.pagoda.matchmeal.model.dto.response.ActiveChallengeDto;
import com.pagoda.matchmeal.model.entity.Challenge;
import com.pagoda.matchmeal.model.entity.ChallengeInvitation;
import com.pagoda.matchmeal.model.entity.Diet;
import com.pagoda.matchmeal.model.entity.Follow;
import com.pagoda.matchmeal.model.enums.ChallengeType;
import com.pagoda.matchmeal.model.enums.MealType;
import com.pagoda.matchmeal.service.impl.ChallengeServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChallengeServiceTest {

    @InjectMocks
    private ChallengeServiceImpl challengeService;

    @Mock
    private ChallengeMapper challengeMapper;

    @Mock
    private FollowService followService;

    @Test
    @DisplayName("챌린지 생성 성공")
    void createChallenge_Success() {
        //given
        Long userId = 1L;
        ChallengeCreateRequestDto dto = new ChallengeCreateRequestDto();


        doAnswer(invocation -> {
            Challenge arg = invocation.getArgument(0);
            // 수동 주입 (실제는 MyBatis가 해줌)
            return null;
        }).when(challengeMapper).insertChallenge(any(Challenge.class));

        // when
        challengeService.createChallenge(userId, dto);

        // then
        verify(challengeMapper, times(1)).insertChallenge(any(Challenge.class));
        verify(challengeMapper, times(1)).insertUserChallenge(eq(userId), any());
    }

    @Test
    @DisplayName("공개 챌린지 참여 - 성공")
    void joinPublicChallenge_Success() {
        // given
        Long userId = 10L;
        Long challengeId = 1L;
        Challenge challenge = Challenge.builder()
                .challengeId(challengeId)
                .isPublic(true)
                .maxParticipants(5)
                .build();

        given(challengeMapper.findById(challengeId)).willReturn(challenge);
        given(challengeMapper.existsByUserIdAndChallengeId(userId, challengeId)).willReturn(false);
        given(challengeMapper.countParticipants(challengeId)).willReturn(3);

        // when
        challengeService.joinPublicChallenge(userId, challengeId);

        // then
        verify(challengeMapper).insertUserChallenge(userId, challengeId);
    }

    @Test
    @DisplayName("공개 챌린지 참여 - 실패: 비공개 방")
    void joinPublicChallenge_Fail_Private() {
        // given
        Long userId = 10L;
        Long challengeId = 1L;
        Challenge challenge = Challenge.builder()
                .challengeId(challengeId)
                .isPublic(false) // 비공개 설정
                .build();

        // [수정] eq(challengeId)를 사용하여 명확하게 매칭
        given(challengeMapper.findById(eq(challengeId))).willReturn(challenge);

        // when & then
        assertThatThrownBy(() -> challengeService.joinPublicChallenge(userId, challengeId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorResponseCode.UNAUTHORIZED); // 필드명 확인 (errorCode 또는 code)
    }

    @Test
    @DisplayName("공개 챌린지 참여 - 실패: 인원 초과")
    void joinPublicChallenge_Fail_Full() {
        // given
        Long userId = 10L;
        Long challengeId = 1L;
        Challenge challenge = Challenge.builder()
                .challengeId(challengeId)
                .isPublic(true)
                .maxParticipants(5)
                .build();

        given(challengeMapper.findById(eq(challengeId))).willReturn(challenge);
        given(challengeMapper.existsByUserIdAndChallengeId(eq(userId), eq(challengeId))).willReturn(false);
        given(challengeMapper.countParticipants(eq(challengeId))).willReturn(5);

        // when & then
        assertThatThrownBy(() -> challengeService.joinPublicChallenge(userId, challengeId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorResponseCode.CHALLENGE_FULL);
    }

    @Test
    @DisplayName("진척도 업데이트 - 성공: 목표 칼로리 달성 시 스트릭 증가")
    void updateChallengeProgress_Success_CalorieLimit() {
        // given
        Long userId = 1L;
        LocalDate today = LocalDate.now();

        // 상황: 현재 1일차 성공, 1 스트릭 중
        ActiveChallengeDto activeDto = ActiveChallengeDto.builder()
                .userChallengeId(100L)
                .type(ChallengeType.CALORIE_LIMIT)
                .targetValue(500) // 500kcal 이하 목표
                .startDate(today.minusDays(1))
                .endDate(today.plusDays(5))
                .currentCount(1)
                .currentStreak(1)
                .maxStreak(1)
                .lastSuccessDate(today.minusDays(1)) // 어제 성공함
                .goalCount(7)
                .build();

        // 400kcal 식단 (성공 조건)
        Diet diet = Diet.builder()
                .totalCalories(400)
                .eatDate(today)
                .build();

        given(challengeMapper.findActiveChallengesByUserId(userId))
                .willReturn(List.of(activeDto));

        // when
        challengeService.updateChallengeProgress(userId, diet, Collections.emptyList());

        // then
        // updateProgress가 호출되었는지 확인
        // 어제 성공했으므로 streak은 2가 되어야 함 (1 + 1)
        verify(challengeMapper).updateProgress(argThat(dto ->
                dto.getCurrentStreak() == 2 &&
                        dto.getCurrentCount() == 2 &&
                        dto.getLastSuccessDate().equals(today)
        ));
    }

    @Test
    @DisplayName("진척도 업데이트 - 실패: 목표 칼로리 초과")
    void updateChallengeProgress_Fail_CalorieOver() {
        // given
        Long userId = 1L;
        LocalDate today = LocalDate.now();

        ActiveChallengeDto activeDto = ActiveChallengeDto.builder()
                .userChallengeId(100L)
                .type(ChallengeType.CALORIE_LIMIT)
                .targetValue(500)
                .startDate(today)
                .endDate(today.plusDays(5))
                .currentCount(0)
                .build();

        // 600kcal 식단 (실패 조건)
        Diet diet = Diet.builder()
                .totalCalories(600)
                .eatDate(today)
                .build();

        given(challengeMapper.findActiveChallengesByUserId(userId))
                .willReturn(List.of(activeDto));

        // when
        challengeService.updateChallengeProgress(userId, diet, Collections.emptyList());

        // then
        // 조건 불충족으로 updateProgress가 호출되지 않아야 함
        verify(challengeMapper, never()).updateProgress(any());
    }

    @Test
    @DisplayName("친구 초대 - 실패: 팔로우하지 않음")
    void inviteUser_Fail_NotFollowing() {
        // given
        Long inviterId = 1L;
        Long targetId = 2L;
        Long challengeId = 100L;

        // 내가 챌린지 멤버는 맞음
        given(challengeMapper.existsByUserIdAndChallengeId(inviterId, challengeId)).willReturn(true);
        // 하지만 팔로우는 안함
        given(followService.isFollowing(inviterId, targetId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> challengeService.inviteUser(inviterId, challengeId, targetId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorResponseCode.NOT_FOLLOWING);
    }

    @Test
    @DisplayName("챌린지 검색 - 성공: Mapper에 userId와 Condition 전달")
    void searchChallenges_Success() {
        // given
        Long userId = 1L;
        ChallengeSearchCondition condition = new ChallengeSearchCondition();
        condition.setKeyword("테스트");

        // Mock 반환값 설정
        given(challengeMapper.searchPublicChallenges(userId, condition))
                .willReturn(List.of()); // 빈 리스트 반환 가정

        // when
        challengeService.searchChallenges(userId, condition);

        // then
        // Mapper가 올바른 인자(userId, condition)로 호출되었는지 검증
        verify(challengeMapper).searchPublicChallenges(eq(userId), eq(condition));
    }

    /* =======================================================
       [NEW] 1. 진척도 업데이트 테스트 (로직 변경 반영)
       ======================================================= */

    @Test
    @DisplayName("진척도 업데이트 - 성공: 타임 어택 (모든 식사 시간 체크)")
    void updateChallengeProgress_Success_TimeRange() {
        // given
        Long userId = 1L;
        LocalDate today = LocalDate.now();

        ActiveChallengeDto activeDto = ActiveChallengeDto.builder()
                .userChallengeId(100L)
                .type(ChallengeType.TIME_RANGE)
                .targetValue(20) // 20시(저녁 8시) 이전 식사 목표
                .startDate(today.minusDays(1))
                .endDate(today.plusDays(5))
                .currentCount(0)
                .build();

        // 19:30에 먹은 저녁 식사 (성공 조건)
        Diet diet = Diet.builder()
                .eatDate(today)
                .eatTime(LocalTime.of(19, 30))
                .mealType(MealType.DINNER) // 아침이 아니어도 성공해야 함
                .build();

        given(challengeMapper.findActiveChallengesByUserId(userId))
                .willReturn(List.of(activeDto));

        // when
        challengeService.updateChallengeProgress(userId, diet, Collections.emptyList());

        // then
        verify(challengeMapper).updateProgress(argThat(dto ->
                dto.getCurrentCount() == 1 // 카운트 1 증가 확인
        ));
    }

    @Test
    @DisplayName("진척도 업데이트 - 성공: 기록형 (어떤 식사든 성공)")
    void updateChallengeProgress_Success_RecordFrequency() {
        // given
        Long userId = 1L;
        LocalDate today = LocalDate.now();

        ActiveChallengeDto activeDto = ActiveChallengeDto.builder()
                .userChallengeId(200L)
                .type(ChallengeType.RECORD_FREQUENCY)
                .targetValue(0)
                .startDate(today)
                .endDate(today.plusDays(5))
                .currentCount(5)
                .goalCount(10)
                .build();

        // 점심 식사 기록
        Diet diet = Diet.builder()
                .eatDate(today)
                .mealType(MealType.LUNCH)
                .build();

        given(challengeMapper.findActiveChallengesByUserId(userId))
                .willReturn(List.of(activeDto));

        // when
        challengeService.updateChallengeProgress(userId, diet, Collections.emptyList());

        // then
        verify(challengeMapper).updateProgress(any());
    }

    /* =======================================================
       [NEW] 2. 챌린지 나가기 테스트
       ======================================================= */

    @Test
    @DisplayName("챌린지 나가기 - 성공")
    void leaveChallenge_Success() {
        // given
        Long userId = 10L;
        Long challengeId = 50L;
        Long ownerId = 99L; // 방장은 다른 사람

        Challenge challenge = Challenge.builder()
                .challengeId(challengeId)
                .ownerId(ownerId)
                .build();

        given(challengeMapper.findById(challengeId)).willReturn(challenge);
        given(challengeMapper.existsByUserIdAndChallengeId(userId, challengeId)).willReturn(true);

        // when
        challengeService.leaveChallenge(userId, challengeId);

        // then
        verify(challengeMapper).deleteUserChallenge(userId, challengeId);
        verify(challengeMapper).decreaseHeadCount(challengeId);
    }

    @Test
    @DisplayName("챌린지 나가기 - 실패: 방장은 나갈 수 없음")
    void leaveChallenge_Fail_Owner() {
        // given
        Long userId = 10L;
        Long challengeId = 50L;

        Challenge challenge = Challenge.builder()
                .challengeId(challengeId)
                .ownerId(userId) // 내가 방장
                .build();

        given(challengeMapper.findById(challengeId)).willReturn(challenge);

        // when & then
        assertThatThrownBy(() -> challengeService.leaveChallenge(userId, challengeId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorResponseCode.OWNER_CANNOT_LEAVE);
    }

    /* =======================================================
       [NEW] 3. 초대 응답 테스트
       ======================================================= */

    @Test
    @DisplayName("초대 승인 - 성공")
    void respondInvitation_Accept_Success() {
        // given
        Long userId = 10L;
        Long invitationId = 1L;
        Long challengeId = 100L;

        ChallengeInvitation invitation = ChallengeInvitation.builder()
                .invitationId(invitationId)
                .inviteeId(userId)
                .challengeId(challengeId)
                .status("PENDING")
                .build();

        Challenge challenge = Challenge.builder()
                .challengeId(challengeId)
                .maxParticipants(10)
                .build();

        given(challengeMapper.findInvitationById(invitationId)).willReturn(invitation);
        given(challengeMapper.findById(challengeId)).willReturn(challenge);
        // 참여 가능 상태 가정 (중복X, 인원초과X)
        given(challengeMapper.existsByUserIdAndChallengeId(userId, challengeId)).willReturn(false);
        given(challengeMapper.countParticipants(challengeId)).willReturn(5);

        // when
        challengeService.respondInvitation(userId, invitationId, true);

        // then
        verify(challengeMapper).insertUserChallenge(userId, challengeId); // 참여
        verify(challengeMapper).increaseHeadCount(challengeId); // 인원 증가
        verify(challengeMapper).updateInvitationStatus(invitationId, "ACCEPTED"); // 상태 변경
    }

    @Test
    @DisplayName("초대 거절 - 성공")
    void respondInvitation_Reject_Success() {
        // given
        Long userId = 10L;
        Long invitationId = 1L;

        ChallengeInvitation invitation = ChallengeInvitation.builder()
                .invitationId(invitationId)
                .inviteeId(userId)
                .status("PENDING")
                .build();

        given(challengeMapper.findInvitationById(invitationId)).willReturn(invitation);

        // when
        challengeService.respondInvitation(userId, invitationId, false); // 거절(false)

        // then
        verify(challengeMapper, never()).insertUserChallenge(anyLong(), anyLong()); // 참여 안 함
        verify(challengeMapper).updateInvitationStatus(invitationId, "REJECTED"); // 거절 상태 변경
    }

    @Test
    @DisplayName("초대 응답 - 실패: 본인 초대가 아님")
    void respondInvitation_Fail_NotOwner() {
        // given
        Long userId = 10L;
        Long otherUserId = 20L;
        Long invitationId = 1L;

        ChallengeInvitation invitation = ChallengeInvitation.builder()
                .invitationId(invitationId)
                .inviteeId(otherUserId) // 다른 사람이 받은 초대
                .build();

        given(challengeMapper.findInvitationById(invitationId)).willReturn(invitation);

        // when & then
        assertThatThrownBy(() -> challengeService.respondInvitation(userId, invitationId, true))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorResponseCode.UNAUTHORIZED);
    }
}
