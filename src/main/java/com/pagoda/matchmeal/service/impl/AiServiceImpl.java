package com.pagoda.matchmeal.service.impl;

import com.pagoda.matchmeal.service.AiService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class AiServiceImpl implements AiService {
    @Override
    public String getPeriodFeedback(Long userId, LocalDate startDate, LocalDate endDate) {
        return "";
    }
}
