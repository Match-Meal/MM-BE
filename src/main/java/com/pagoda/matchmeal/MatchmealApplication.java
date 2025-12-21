package com.pagoda.matchmeal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MatchmealApplication {

    public static void main(String[] args) {
        SpringApplication.run(MatchmealApplication.class, args);
    }

}
