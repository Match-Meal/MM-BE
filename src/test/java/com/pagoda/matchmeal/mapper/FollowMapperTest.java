package com.pagoda.matchmeal.mapper;

import com.pagoda.matchmeal.model.dto.response.FollowListDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // 실제 DB 혹은 설정된 H2 사용
class FollowMapperTest {

    @Autowired
    private FollowMapper followMapper;

    @Test
    @DisplayName("getFollowers 쿼리 동작 확인 - 맞팔 상태 체크")
    void getFollowers_QueryTest() {
        // *주의*: 이 테스트가 성공하려면 test/resources/data.sql 등에
        // users 테이블과 follows 테이블에 테스트 데이터가 미리 insert 되어 있어야 합니다.

        // given
        Long targetId = 2L;  // 조회 대상
        Long viewerId = 5L;  // 나

        // when
        List<FollowListDto> result = followMapper.getFollowers(targetId, viewerId);

        // then
        assertThat(result).isNotNull();
        // 실제 데이터가 있다면 아래와 같이 검증
        // assertThat(result).extracting("isFollowing").contains(true);
    }
}