package com.pagoda.matchmeal.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
/**
 * 공통 응답 DTO (Wrapper Class)
 *
 * 제네릭 <T>를 사용하여 어떤 종류의 데이터(식단, 댓글, 회원 등)든
 * 이 클래스로 감싸서 '데이터 리스트'와 '페이지 정보'를 함께 반환합니다.
 * 이를 통해 API 응답 형식을 통일합니다.
 *
 * @param <T> 반환할 데이터의 타입
 */
public class PageInfoResponseDto<T> {

    // 실제 데이터 리스트
    private List<T> content;
    // 페이지네이션 정보
    private PageResponseDto pageInfo;

    /**
     * 페이지네이션 정보를 계산하고 DTO를 생성하는 정적 팩토리 메서드
     * 서비스 로직에서 일일이 페이지 계산을 하지 않고,
     * 데이터 리스트와 Pageable 정보만 넘기면 알아서 계산하여 응답 객체를 만들어줍니다.
     *
     * @param pageable      Spring Data의 Pageable 객체
     * @param content       현재 페이지에 포함될 실제 데이터 리스트
     * @param totalCount    전체 데이터의 총 개수
     * @return              계산된 페이지 정보(PageResponseDto)와 데이터 리스트가 포함된 객체
     * @param <T>           데이터 리스트에 담긴 객체의 타입
     */
    public static <T> PageInfoResponseDto<T> of(Pageable pageable, List<T> content, int totalCount) {
        // Pageable은 0부터 시작하므로, 사용자 친화적인 1부터 시작하는 번호로 변환
        int pageNo = pageable.getPageNumber() + 1;
        int size = pageable.getPageSize();

        // 리스트 크기와 요청 사이즈 중 작은 값을 선택 (IndexOutOfBounds 방지 및 실제 개수 확인)
        int numberOfElements = Math.min(content.size(), size);

        // 전체 페이지 수 계산 (전체 개수 / 사이즈 -> 올림 처리)
        int totalPage = (int) Math.ceil((double) totalCount / size);

        // 현재 페이지가 전체 페이지 수보다 작으면 다음 페이지가 존재함
        boolean hasNext = pageNo < totalPage;

        // 메타데이터 객체 생성
        PageResponseDto pageResponseDto = new PageResponseDto(
                pageNo,
                size,
                numberOfElements,
                totalCount,
                totalPage,
                hasNext
        );

        // 입력받은 리스트를 실제 개수만큼 안전하게 자름 (혹시 모를 오버패칭 방지)
        List<T> subList = content.subList(0, numberOfElements);

        return new PageInfoResponseDto<>(subList, pageResponseDto);
    }
}
