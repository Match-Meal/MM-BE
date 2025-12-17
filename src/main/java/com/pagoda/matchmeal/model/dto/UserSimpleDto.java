package com.pagoda.matchmeal.model.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserSimpleDto {
    private Long userId;          // 클릭해서 프로필 이동할 때 필요
    private String userName;      // 화면 표시용 이름 (실명보다는 닉네임 권장)
    private String profileImage;  // 프사
}