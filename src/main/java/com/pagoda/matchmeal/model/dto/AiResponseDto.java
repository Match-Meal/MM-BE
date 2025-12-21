package com.pagoda.matchmeal.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiResponseDto {
    @JsonProperty("best_candidate")
    private String bestCandidate; // "김치찌개"

    @JsonProperty("candidates")
    private List<String> candidates; // ["김치찜", "부대찌개"]
}