package com.pagoda.matchmeal.common.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = "com.pagoda.matchmeal.mapper")
public class MybatisConfig {
}
