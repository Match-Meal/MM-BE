package com.pagoda.matchmeal.model.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Follow {
    private Long id;
    private Long followerId ; // 나
    private Long followingId; // 상대방
    private LocalDateTime createdAt;

}
