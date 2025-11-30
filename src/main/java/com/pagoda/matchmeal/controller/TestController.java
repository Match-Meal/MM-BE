package com.pagoda.matchmeal.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/test/token")
    public String token(@RequestParam String accessToken) {
        return accessToken;
    }

}
