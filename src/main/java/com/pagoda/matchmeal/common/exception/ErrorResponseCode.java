package com.pagoda.matchmeal.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorResponseCode {

    //----------------------------공통코드----------------------------
    SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 에러"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "접근 권한이 없습니다."),
//    INVALID_FILE(HttpStatus.)

    //----------------------------음식DB 에러코드----------------------------
    FOOD_NOT_FOUND(HttpStatus.NOT_FOUND, "음식 데이터를 찾을 수 없습니다."),

    //---------------------------- 유저 에러코드----------------------------
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."),
    ALREADY_USER_DELETE(HttpStatus.BAD_REQUEST, "이미 탈퇴처리된 회원입니다."),
    USER_WITHDRAWN_WAITING(HttpStatus.BAD_REQUEST, "탈퇴한 회원입니다. 복구 또는 재가입을 선택해주세요."),

    //----------------------------식단 에러코드----------------------------
    DIET_NOT_FOUND(HttpStatus.NOT_FOUND, "식단 데이터를 찾을 수 없습니다."),

    //----------------------------파일 에러코드----------------------------
    INVALID_FILE(HttpStatus.BAD_REQUEST, "유효하지 않은 파일입니다."),
    FILE_UPLOAD_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드 중 오류가 발생했습니다."),
    FILE_DELETE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제 중 오류가 발생했습니다."),
    INVALID_FILE_EXTENSION(HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식입니다."),

    //----------------------------팔로우 에러코드----------------------------
    SELF_FOLLOW_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "자기 자신을 팔로우 할 수 없습니다."),


    //----------------------------챌린지 에러코드----------------------------
    ALREADY_JOINED_CHALLENGE(HttpStatus.BAD_REQUEST, "이미 참여중인 챌린지입니다."),
    ALREADY_INVITED(HttpStatus.CONFLICT, "이미 초대를 보낸 사용자입니다."),
    ALREADY_JOINED_USER(HttpStatus.CONFLICT, "이미 챌린지에 참여 중인 사용자입니다."),
    NOT_JOINED_USER(HttpStatus.NOT_FOUND, "참여중인 대상이 아닙니다."),
    CHALLENGE_FULL(HttpStatus.BAD_REQUEST, "챌린지 정원이 가득 찼습니다."),
    CHALLENGE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 챌린지를 찾을 수 없습니다."),
    NOT_FOLLOWING(HttpStatus.NOT_FOUND, "팔로우중이 아닙니다."),
    OWNER_CANNOT_LEAVE(HttpStatus.BAD_REQUEST, "소유자는 나갈 수 없습니다."),
    INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "초대장을 찾을 수 없습니다."),
    ALREADY_PROCESSED_INVITATION(HttpStatus.BAD_REQUEST, "이미 처리된 초대장입니다."),

    //----------------------------게시글 에러코드----------------------------
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),
    POST_UPDATE_ERROR(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),

    //----------------------------댓글 에러코드----------------------------
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다.");


    private final HttpStatus status; // 에러 HTTP 상태 코드
    private final String message; // 에러 메세지
}
