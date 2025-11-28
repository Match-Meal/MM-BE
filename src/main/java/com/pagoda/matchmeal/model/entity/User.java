package com.pagoda.matchmeal.model.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private long id;
    private String email;
    private String status;
    private String userName;
    private String phoneNumber;
    private String gender;
    private Date birthDate;
    private Double heightCm;
    private Double weightKg;
    private String role;

    private LocalDateTime deletedAt;

    private String platform;
    private String socialId;
}
