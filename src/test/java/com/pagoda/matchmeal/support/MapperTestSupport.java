package com.pagoda.matchmeal.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pagoda.matchmeal.common.config.RedisConfig;
import com.pagoda.matchmeal.common.config.SecurityConfig;
import com.pagoda.matchmeal.common.config.jwt.JwtAuthenticationFilter;
import com.pagoda.matchmeal.common.config.jwt.JwtTokenProvider;
import com.pagoda.matchmeal.common.config.oauth.OAuth2FailureHandler;
import com.pagoda.matchmeal.common.config.oauth.OAuth2SuccessHandler;
import com.pagoda.matchmeal.service.CustomOAuth2UserService;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@MybatisTest(excludeAutoConfiguration = {
        // 2. 보안 관련 자동 설정은 Mapper 테스트에 불필요하므로 차단
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        OAuth2ClientAutoConfiguration.class
})
// 3. 실제 DB(H2) 설정 사용 (Embedded DB 강제 전환 방지)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
// 4. application.properties의 test 프로파일 적용
@ActiveProfiles("test")
@org.mybatis.spring.annotation.MapperScan("com.pagoda.matchmeal.mapper")
public abstract class MapperTestSupport {

}