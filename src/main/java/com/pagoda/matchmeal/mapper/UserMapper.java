package com.pagoda.matchmeal.mapper;

import com.pagoda.matchmeal.model.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 회원(User) 데이터베이스 접근 매퍼
 * - 회원가입, 정보 수정, 조회, 탈퇴(Soft/Hard) 관리
 */
@Mapper
public interface UserMapper {
    /**
     * 신규 회원 정보 저장 (회원가입)
     */
    void save(User user);

    /**
     * 사용자 닉네임(UserName) 변경
     */
    void updateUserName(User user);

    /**
     * 소셜 ID(OAuth Provider ID)로 회원 조회
     * - 로그인 시 가입 여부 확인용
     */
    Optional<User> findBySocialId(String socialId);

    /**
     * PK(UserId)로 회원 상세 조회
     */
    Optional<User> findById(Long userId);

    /**
     * 프로필 정보(이미지, 한줄 소개 등) 수정
     */
    void updateProfile(User user);

    /**
     * 계정 공개/비공개 여부 수정
     */
    void updateVisibility(User user);

    /**
     * 회원 탈퇴 처리 (Soft Delete)
     * - 데이터를 삭제하지 않고 상태(Role/Status)만 변경
     */
    void softDeleteUser(Long userId);

    /**
     * 탈퇴 회원 복구
     * - Soft Delete 된 회원을 정상 상태로 변경
     */
    void restoreUser(Long userId);

    /**
     * 만료된 탈퇴 회원 영구 삭제 (Hard Delete / Batch)
     * - 탈퇴 후 일정 기간(예: 7일)이 지난 데이터를 DB에서 완전히 삭제
     */
    void hardDeleteExpiredUsers(@Param("thresholdDate") LocalDateTime thresholdDate);

    /**
     * 특정 회원 영구 삭제 (관리자용/즉시삭제)
     */
    void hardDeleteUserById(Long userId);

    /**
     * 유저 권한 업그레이드
     */
    void updateUserRole(User user);
}