package com.pagoda.matchmeal.model.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class BaseEntity {
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
