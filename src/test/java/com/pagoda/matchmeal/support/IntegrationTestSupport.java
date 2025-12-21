package com.pagoda.matchmeal.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pagoda.matchmeal.service.RedisService;
import com.pagoda.matchmeal.service.S3Service;
import com.pagoda.matchmeal.service.SocialUnlinkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc // API 테스트를 위한 MockMvc 설정
@Transactional // 테스트 끝나면 DB 롤백
@ActiveProfiles("test") // application-test.yml 적용
public abstract class IntegrationTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected RedisService redisService;

    @MockitoBean
    protected S3Service s3Service;

    @MockitoBean
    protected SocialUnlinkService socialUnlinkService;
}