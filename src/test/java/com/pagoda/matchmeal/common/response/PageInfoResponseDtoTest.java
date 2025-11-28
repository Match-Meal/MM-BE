package com.pagoda.matchmeal.common.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class PageInfoResponseDtoTest {

    @Test
    @DisplayName("페이지네이션 계산 로직 검증 - 첫 번째 페이지 (정상 케이스)")
    void calculatePagination_FirstPage() {
        // given
        // 총 데이터 25개, 한 페이지당 10개, 현재 0번 페이지(첫 페이지) 요청
        int totalCount = 25;
        Pageable pageable = PageRequest.of(0, 10);
        
        // DB에서 조회된 결과라고 가정 (10개)
        List<String> content = createMockData(10);

        // when
        PageInfoResponseDto<String> result = PageInfoResponseDto.of(pageable, content, totalCount);

        // then
        PageResponseDto info = result.getPageInfo();

        assertThat(info.getPageNo()).isEqualTo(1);       // 0 + 1 = 1페이지
        assertThat(info.getSize()).isEqualTo(10);        // 요청 사이즈
        assertThat(info.getNumberOfElements()).isEqualTo(10); // 현재 페이지 데이터 수
        assertThat(info.getTotalPage()).isEqualTo(3);    // 25 / 10 올림 = 3페이지
        assertThat(info.isHasNext()).isTrue();           // 1 < 3 이므로 다음 페이지 있음
        assertThat(result.getContent()).hasSize(10);
    }

    @Test
    @DisplayName("페이지네이션 계산 로직 검증 - 마지막 페이지 (자투리 데이터)")
    void calculatePagination_LastPage_Partial() {
        // given
        // 총 데이터 25개, 한 페이지당 10개, 현재 2번 페이지(세 번째 페이지) 요청
        int totalCount = 25;
        Pageable pageable = PageRequest.of(2, 10);
        
        // DB에서 조회된 결과라고 가정 (남은 5개)
        List<String> content = createMockData(5);

        // when
        PageInfoResponseDto<String> result = PageInfoResponseDto.of(pageable, content, totalCount);

        // then
        PageResponseDto info = result.getPageInfo();

        assertThat(info.getPageNo()).isEqualTo(3);       // 2 + 1 = 3페이지
        assertThat(info.getTotalPage()).isEqualTo(3);    // 총 3페이지
        assertThat(info.getNumberOfElements()).isEqualTo(5); // 남은 데이터 5개
        assertThat(info.isHasNext()).isFalse();          // 마지막 페이지므로 false
    }

    @Test
    @DisplayName("페이지네이션 계산 로직 검증 - 마지막 페이지 (딱 떨어지는 경우)")
    void calculatePagination_LastPage_Exact() {
        // given
        // 총 데이터 20개, 한 페이지당 10개, 현재 1번 페이지(두 번째 페이지) 요청
        int totalCount = 20;
        Pageable pageable = PageRequest.of(1, 10);

        // DB에서 조회된 결과 (10개)
        List<String> content = createMockData(10);

        // when
        PageInfoResponseDto<String> result = PageInfoResponseDto.of(pageable, content, totalCount);

        // then
        PageResponseDto info = result.getPageInfo();

        assertThat(info.getPageNo()).isEqualTo(2);
        assertThat(info.getTotalPage()).isEqualTo(2);    // 20 / 10 = 2페이지
        assertThat(info.isHasNext()).isFalse();          // 2 < 2 는 false
    }

    // 테스트용 더미 데이터 생성 헬퍼 메서드
    private List<String> createMockData(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> "Data " + i)
                .collect(Collectors.toList());
    }
}