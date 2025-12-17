package com.pagoda.matchmeal.model.dto.request;

import com.pagoda.matchmeal.model.enums.PostCategory;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class PostRequestDto {

    private PostCategory category;
    private String title;
    private String content;

    private List<Long> deleteFileIds;
}
