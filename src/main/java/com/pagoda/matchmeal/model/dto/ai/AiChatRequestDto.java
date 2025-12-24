package com.pagoda.matchmeal.model.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class AiChatRequestDto {
    @JsonProperty("user_profile")
    private AiUserProfileDto userProfile;

    @JsonProperty("history")
    private List<Map<String, String>> history; // [{"role": "user", "content": "..."}, ...]

    @JsonProperty("message")
    private String message;
}
