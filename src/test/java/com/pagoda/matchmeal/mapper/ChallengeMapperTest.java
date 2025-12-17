package com.pagoda.matchmeal.mapper;

import com.pagoda.matchmeal.model.dto.response.ActiveChallengeDto;
import com.pagoda.matchmeal.model.dto.ChallengeSearchCondition;
import com.pagoda.matchmeal.model.dto.response.ChallengeResponseDto;
import com.pagoda.matchmeal.model.entity.Challenge;
import com.pagoda.matchmeal.model.enums.ChallengeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@MapperScan("com.pagoda.matchmeal.mapper")
class ChallengeMapperTest {

    @Autowired
    private ChallengeMapper challengeMapper;

    @Test
    @DisplayName("챌린지 생성 및 ID 자동 생성 확인")
    void insertChallenge_Success() {
        // given
        Challenge challenge = Challenge.builder()
                .ownerId(1L)
                .title("아침 먹기 챌린지")
                .description("설명")
                .type(ChallengeType.TIME_RANGE)
                .targetValue(9)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(7))
                .goalCount(5)
                .maxParticipants(10)
                .isPublic(true)
                .invitationCode(UUID.randomUUID().toString().substring(0, 8))
                .build();

        // when
        challengeMapper.insertChallenge(challenge);

        // then
        assertThat(challenge.getChallengeId()).isNotNull(); // useGeneratedKeys 작동 확인

        // 조회해서 값 검증
        Challenge saved = challengeMapper.findById(challenge.getChallengeId());
        assertThat(saved.getTitle()).isEqualTo("아침 먹기 챌린지");
        assertThat(saved.getCurrentHeadCount()).isEqualTo(1); // Default 1
    }

    @Test
    @DisplayName("공개 챌린지 검색 - 동적 쿼리 테스트")
    void searchPublicChallenges_DynamicQuery() {
        // given
        createDummyChallenge("공개 챌린지 1", ChallengeType.CALORIE_LIMIT, true);
        createDummyChallenge("공개 챌린지 2", ChallengeType.TIME_RANGE, true);
        createDummyChallenge("비공개 챌린지", ChallengeType.CALORIE_LIMIT, false);

        // Case 1: 타입 검색 (CALORIE_LIMIT만 검색 -> 비공개 제외하고 1개 나와야 함)
        ChallengeSearchCondition condType = new ChallengeSearchCondition();
        condType.setType(ChallengeType.CALORIE_LIMIT);

        List<ChallengeResponseDto> resultType = challengeMapper.searchPublicChallenges(condType);
        assertThat(resultType).hasSize(1);
        assertThat(resultType.get(0).getTitle()).isEqualTo("공개 챌린지 1");

        // Case 2: 키워드 검색
        ChallengeSearchCondition condKeyword = new ChallengeSearchCondition();
        condKeyword.setKeyword("공개");

        List<ChallengeResponseDto> resultKeyword = challengeMapper.searchPublicChallenges(condKeyword);
        assertThat(resultKeyword).hasSize(2); // 공개 1, 2 둘 다 나와야 함
    }

    @Test
    @DisplayName("챌린지 참여 및 진행 중인 챌린지 조회")
    void joinAndFindActiveChallenges() {
        // given
        Long userId = 100L;
        Challenge challenge = createDummyChallenge("참여할 챌린지", ChallengeType.RECORD_FREQUENCY, true);
        Long challengeId = challenge.getChallengeId();

        // when
        challengeMapper.insertUserChallenge(userId, challengeId);

        // then
        // 1. 중복 확인 메서드 테스트
        boolean exists = challengeMapper.existsByUserIdAndChallengeId(userId, challengeId);
        assertThat(exists).isTrue();

        // 2. Active 챌린지 조회 테스트 (ActiveChallengeDto 매핑 확인)
        List<ActiveChallengeDto> activeList = challengeMapper.findActiveChallengesByUserId(userId);
        assertThat(activeList).hasSize(1);
        assertThat(activeList.get(0).getTargetValue()).isEqualTo(challenge.getTargetValue());
        assertThat(activeList.get(0).getCurrentStreak()).isEqualTo(0);
    }

    @Test
    @DisplayName("진척도(스트릭) 업데이트 반영 확인")
    void updateProgress_Success() {
        // given
        Long userId = 200L;
        Challenge challenge = createDummyChallenge("업데이트 테스트", ChallengeType.CALORIE_LIMIT, true);
        Long challengeId = challenge.getChallengeId();

        // 참여 시키기
        challengeMapper.insertUserChallenge(userId, challengeId);

        // 방금 참여한 정보 가져오기 (ID를 알기 위해)
        ActiveChallengeDto activeDto = challengeMapper.findActiveChallengesByUserId(userId).get(0);

        // 업데이트할 값 세팅 (서비스 로직에서 계산된 결과라고 가정)
        activeDto.setCurrentCount(5);
        activeDto.setCurrentStreak(3);
        activeDto.setMaxStreak(3);
        activeDto.setLastSuccessDate(LocalDate.now());

        // when
        challengeMapper.updateProgress(activeDto);

        // then
        // 다시 조회해서 업데이트 되었는지 확인
        ActiveChallengeDto updatedDto = challengeMapper.findActiveChallengesByUserId(userId).get(0);

        assertThat(updatedDto.getCurrentCount()).isEqualTo(5);
        assertThat(updatedDto.getCurrentStreak()).isEqualTo(3);
        assertThat(updatedDto.getLastSuccessDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("전체 챌린지 목록 조회 - 내 참여 상태 매핑 확인")
    void findAllChallenges_StatusCheck() {
        // given
        Long myId = 10L;
        Challenge c1 = createDummyChallenge("내가 참여한 방", ChallengeType.TIME_RANGE, true);
        Challenge c2 = createDummyChallenge("참여 안 한 방", ChallengeType.TIME_RANGE, true);

        challengeMapper.insertUserChallenge(myId, c1.getChallengeId());

        // when
        List<ChallengeResponseDto> list = challengeMapper.findAllChallenges(myId);

        // then (최신순 정렬이므로 c2, c1 순서 혹은 생성 시간에 따라 다름)
        // c1(참여함) 검증
        ChallengeResponseDto res1 = list.stream()
                .filter(d -> d.getChallengeId().equals(c1.getChallengeId()))
                .findFirst().orElseThrow();

        assertThat(res1.isJoined()).isTrue();
        assertThat(res1.getUserChallengeId()).isNotNull();

        // c2(참여안함) 검증
        ChallengeResponseDto res2 = list.stream()
                .filter(d -> d.getChallengeId().equals(c2.getChallengeId()))
                .findFirst().orElseThrow();

        assertThat(res2.isJoined()).isFalse();
        assertThat(res2.getUserChallengeId()).isNull();
    }

    @Test
    @DisplayName("초대장 발송 및 중복 확인")
    void invite_Success() {
        // given
        Long challengeId = 1L;
        Long inviterId = 10L;
        Long inviteeId = 20L;

        // when
        challengeMapper.insertInvitation(challengeId, inviterId, inviteeId);

        // then
        boolean exists = challengeMapper.existsInvitation(challengeId, inviteeId);
        assertThat(exists).isTrue();

        // 다른 사람 확인
        boolean notExists = challengeMapper.existsInvitation(challengeId, 99L);
        assertThat(notExists).isFalse();
    }

    // Helper Method
    private Challenge createDummyChallenge(String title, ChallengeType type, boolean isPublic) {
        Challenge c = Challenge.builder()
                .ownerId(1L)
                .title(title)
                .description("desc")
                .type(type)
                .targetValue(100)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .goalCount(10)
                .maxParticipants(5)
                .isPublic(isPublic)
                .invitationCode(UUID.randomUUID().toString().substring(0,8))
                .build();
        challengeMapper.insertChallenge(c);
        return c;
    }
}