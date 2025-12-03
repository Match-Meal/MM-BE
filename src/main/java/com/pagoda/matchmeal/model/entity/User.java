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
    private String phoneNumber;
    private Gender gender;
    private LocalDate birthDate;
    private Double heightCm;
    private Double weightKg;
    private UserRole role;

    private LocalDateTime deletedAt;

    private String platform;
    private String socialId;
}
