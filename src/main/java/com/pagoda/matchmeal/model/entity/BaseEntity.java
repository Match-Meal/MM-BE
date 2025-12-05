package com.pagoda.matchmeal.model.entity;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BaseEntity {
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
