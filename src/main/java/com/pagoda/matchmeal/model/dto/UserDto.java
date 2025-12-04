package com.pagoda.matchmeal.model.dto;

import com.pagoda.matchmeal.model.enums.Gender;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private Long id;
    private String socialId;
    private String email;
    private String userName;
    private String role;
    private String createdAt;

    // 프로필 정보 (DB 조회 후 채워질 필드들)
    private String statusMessage;
    private Gender gender;
    private LocalDate birthDate;
    private Double heightCm;
    private Double weightKg;

    // 콤마로 구분된 문자열을 리스트로 변환하여 전달
    private List<String> allergies;
    private List<String> diseases;
}
