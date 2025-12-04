package com.pagoda.matchmeal.model.dto;

import com.pagoda.matchmeal.model.enums.Gender;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class UserProfileDto {
    private String userName;
    private Gender gender;
    private LocalDate birthDate;
    private Double heightCm;
    private Double weightKg;
    private String statusMessage;

    // 프론트에서 배열로 받음
    private List<String> allergies;
    private List<String> diseases;
}
