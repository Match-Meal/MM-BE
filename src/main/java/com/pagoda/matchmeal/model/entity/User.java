package com.pagoda.matchmeal.model.entity;

import com.pagoda.matchmeal.model.enums.Gender;
import com.pagoda.matchmeal.model.enums.UserRole;
import com.pagoda.matchmeal.model.enums.UserStatus;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class User extends BaseEntity {

    // user_id (PK)
    private Long id;
    private String email;
    private UserStatus status;
    private String userName;
    private Gender gender;
    private LocalDate birthDate;
    private Double heightCm;
    private Double weightKg;
    private String profileImage;
    private UserRole role;
    private String statusMessage;

    // 질병과 알레르기 목록 (콤마 구분)
    private String allergies;
    private String diseases;

    // 프로필 공개여부 설정
    private Boolean isPublic;

    private LocalDateTime deletedAt;

    private String platform;
    private String socialId;
}
