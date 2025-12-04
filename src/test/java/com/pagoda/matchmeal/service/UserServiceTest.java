package com.pagoda.matchmeal.service;

import com.pagoda.matchmeal.mapper.UserMapper;
import com.pagoda.matchmeal.model.entity.User;
import com.pagoda.matchmeal.service.impl.UserServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("신규 회원이면 save가 호출되어야 함")
    void saveNewUserTest() {
        // given
        String socialId = "12345";
        String email = "test@gmail.com";

        given(userMapper.findBySocialId(socialId)).willReturn(Optional.empty());

        // when
        Map<String, Object> result = userService.processLoginOrRegister(socialId, email, "테스트유저", "google");

        // then
        verify(userMapper, times(1)).save(any(User.class));

        assertThat(result.get("isNew")).isEqualTo(true);
        assertThat(((User)result.get("user")).getSocialId()).isEqualTo(socialId);
    }
}
