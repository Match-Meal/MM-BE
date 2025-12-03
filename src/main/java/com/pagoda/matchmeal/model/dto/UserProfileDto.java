package com.pagoda.matchmeal.model.dto;

import com.pagoda.matchmeal.model.enums.Gender;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UserProfileDto {
    private String userName;
    private String phoneNumber;
    private Gender gender;
    private LocalDate birthDate;
    private Double heightCm;
    private Double weightKg;
}
