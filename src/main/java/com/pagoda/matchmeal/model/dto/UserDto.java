package com.pagoda.matchmeal.model.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private long id;
    private String socialId;
    private String email;
    private String userName;
    private String role;
    private String createdAt;
}
