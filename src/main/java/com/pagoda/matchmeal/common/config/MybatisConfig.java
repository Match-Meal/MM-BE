package com.pagoda.matchmeal.common.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 설정 클래스
 * - @MapperScan을 통해 MyBatis Mapper 인터페이스가 위치한 패키지를 지정
 */
@Configuration
@MapperScan(basePackages = "com.pagoda.matchmeal.mapper")
public class MybatisConfig {
}
