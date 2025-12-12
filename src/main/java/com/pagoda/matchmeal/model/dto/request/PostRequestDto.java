package com.pagoda.matchmeal.model.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class PostRequestDto {

    private String category;
    private String title;
    private String content;
    
}
