package com.pagoda.matchmeal.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
/**
 * 페이지네이션 메타데이터(Metadata) 클래스
 *
 * 실제 데이터(List)는 포함하지 않고,
 * 프론트엔드에서 '페이지 번호 버튼'이나 '이전/다음 버튼'을 렌더링할 때
 * 필요한 정보들만 모아둔 객체입니다.
 */
public class PageResponseDto {

    // 현재 사용자가 보고 있는 페이지 번호
    private int pageNo;
    // 한 페이지당 보여줄 데이터의 최대 개수
    private int size;
    // 실제 현재 페이지에 조회된 데이터의 개수
    private int numberOfElements;
    // DB에 있는 데이터의 총 개수
    private int totalCount;
    // 전체 페이지 수 (totalCount / size 결과를 올림 처리한 값)
    private int totalPage;
    // 다음 페이지가 존재하는지 여부 (더보기 버튼이나 다음 화살표 활성화 여부 판단)
    private boolean hasNext;

}