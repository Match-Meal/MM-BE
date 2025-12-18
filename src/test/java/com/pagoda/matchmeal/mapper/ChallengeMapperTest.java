package com.pagoda.matchmeal.mapper;

import com.pagoda.matchmeal.model.dto.ChallengeSearchCondition;
import com.pagoda.matchmeal.model.dto.response.ActiveChallengeDto;
import com.pagoda.matchmeal.model.dto.response.ChallengeResponseDto;
import com.pagoda.matchmeal.model.entity.Challenge;
import com.pagoda.matchmeal.model.enums.ChallengeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

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
        Challenge challenge = createDummyChallengeEntity("아침 먹기 챌린지", ChallengeType.TIME_RANGE, true);

        // when
        challengeMapper.insertChallenge(challenge);

        // then
        assertThat(challenge.getChallengeId()).isNotNull();
        Challenge saved = challengeMapper.findById(challenge.getChallengeId());
        assertThat(saved.getTitle()).isEqualTo("아침 먹기 챌린지");
    }

    @Test
    @DisplayName("공개 챌린지 검색 - userId 파라미터 적용 및 동적 쿼리 테스트")
    void searchPublicChallenges_DynamicQuery() {
        // given
        Challenge c1 = createDummyChallenge("공개 챌린지 1", ChallengeType.CALORIE_LIMIT, true);
        Challenge c2 = createDummyChallenge("공개 챌린지 2", ChallengeType.TIME_RANGE, true);
        Challenge c3 = createDummyChallenge("비공개 챌린지", ChallengeType.CALORIE_LIMIT, false);

        Long myUserId = 100L;
        challengeMapper.insertUserChallenge(myUserId, c1.getChallengeId());

        // --- Case 1: 타입 검색 ---
        ChallengeSearchCondition condType = new ChallengeSearchCondition();
        condType.setType(ChallengeType.CALORIE_LIMIT);

        // when
        List<ChallengeResponseDto> resultType = challengeMapper.searchPublicChallenges(myUserId, condType);

        // then
        // [수정 전] assertThat(resultType).hasSize(1);  <-- 기존 데이터 때문에 실패함

        // [수정 후] "내가 만든 c1이 조회 결과에 있는가?"로 검증 변경
        boolean containsC1 = resultType.stream()
                .anyMatch(dto -> dto.getChallengeId().equals(c1.getChallengeId()));
        assertThat(containsC1).isTrue();

        // [수정 후] "비공개인 c3는 조회 결과에 없는가?" 확인
        boolean containsC3 = resultType.stream()
                .anyMatch(dto -> dto.getChallengeId().equals(c3.getChallengeId()));
        assertThat(containsC3).isFalse();

        // [수정 후] "타입이 다른 c2는 조회 결과에 없는가?" 확인
        boolean containsC2 = resultType.stream()
                .anyMatch(dto -> dto.getChallengeId().equals(c2.getChallengeId()));
        assertThat(containsC2).isFalse();
    }

    @Test
    @DisplayName("내 챌린지 목록 조회 - 참여한 것만 조회되는지 확인 (Inner Join)")
    void findMyChallenges_StatusCheck() {
        // given
        Long myId = 10L;
        Challenge c1 = createDummyChallenge("내가 참여한 방", ChallengeType.TIME_RANGE, true);
        Challenge c2 = createDummyChallenge("참여 안 한 방", ChallengeType.TIME_RANGE, true);

        // c1만 참여
        challengeMapper.insertUserChallenge(myId, c1.getChallengeId());

        // when
        List<ChallengeResponseDto> list = challengeMapper.findAllChallenges(myId);

        // then
        // 1. 참여한 챌린지(c1)는 있어야 함
        assertThat(list).hasSize(1);
        ChallengeResponseDto res1 = list.get(0);

        assertThat(res1.getChallengeId()).isEqualTo(c1.getChallengeId());
        assertThat(res1.isJoined()).isTrue();
        assertThat(res1.getUserChallengeId()).isNotNull();

        // 2. 참여하지 않은 챌린지(c2)는 목록에 없어야 함 (Inner Join 이므로)
        boolean existsC2 = list.stream().anyMatch(d -> d.getChallengeId().equals(c2.getChallengeId()));
        assertThat(existsC2).isFalse();
    }

    // ... (기타 insertUserChallenge, updateProgress 등의 테스트는 기존과 동일하므로 유지) ...

    @Test
    @DisplayName("챌린지 참여 및 진행 중인 챌린지 조회")
    void joinAndFindActiveChallenges() {
        Long userId = 100L;
        Challenge challenge = createDummyChallenge("참여할 챌린지", ChallengeType.RECORD_FREQUENCY, true);
        Long challengeId = challenge.getChallengeId();

        challengeMapper.insertUserChallenge(userId, challengeId);

        boolean exists = challengeMapper.existsByUserIdAndChallengeId(userId, challengeId);
        assertThat(exists).isTrue();

        List<ActiveChallengeDto> activeList = challengeMapper.findActiveChallengesByUserId(userId);
        assertThat(activeList).hasSize(1);
    }

    @Test
    @DisplayName("진척도 업데이트 확인")
    void updateProgress_Success() {
        Long userId = 200L;
        Challenge challenge = createDummyChallenge("업데이트 테스트", ChallengeType.CALORIE_LIMIT, true);
        Long challengeId = challenge.getChallengeId();
        challengeMapper.insertUserChallenge(userId, challengeId);

        ActiveChallengeDto activeDto = challengeMapper.findActiveChallengesByUserId(userId).get(0);
        activeDto.setCurrentCount(5);
        activeDto.setCurrentStreak(3);
        activeDto.setLastSuccessDate(LocalDate.now());

        challengeMapper.updateProgress(activeDto);

        ActiveChallengeDto updatedDto = challengeMapper.findActiveChallengesByUserId(userId).get(0);
        assertThat(updatedDto.getCurrentCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("초대장 발송 확인")
    void invite_Success() {
        Long challengeId = 1L;
        Long inviterId = 10L;
        Long inviteeId = 20L;

        challengeMapper.insertInvitation(challengeId, inviterId, inviteeId);
        boolean exists = challengeMapper.existsInvitation(challengeId, inviteeId);
        assertThat(exists).isTrue();
    }

    // --- Helper Methods ---

    private Challenge createDummyChallenge(String title, ChallengeType type, boolean isPublic) {
        Challenge c = Challenge.builder()
                .ownerId(1L) // [주의] users 테이블에 id=1인 유저가 있어야 합니다!
                .title(title)
                .description("테스트 설명입니다.") // Not Null일 수 있음
                .type(type)
                .targetValue(100)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .goalCount(10)
                .maxParticipants(5)
                .currentHeadCount(1) // 현재 인원 1명 (방장)
                .isPublic(isPublic)  // 공개 여부
                .invitationCode(UUID.randomUUID().toString().substring(0, 8)) // [중요] 난수 생성 (Unique 위반 방지)
                .build();

        challengeMapper.insertChallenge(c);
        return c;
    }

    private Challenge createDummyChallengeEntity(String title, ChallengeType type, boolean isPublic) {
        return Challenge.builder()
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
    }
}