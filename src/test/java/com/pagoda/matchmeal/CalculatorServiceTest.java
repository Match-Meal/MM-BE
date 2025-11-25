package com.pagoda.matchmeal;

import com.pagoda.matchmeal.service.CalculatorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CalculatorServiceTest {
    @Test
    @DisplayName("단위 테스트: 1 + 1 = 2")
    void addTest() {
        // given
        CalculatorService calcService = new CalculatorService();

        // when
        int result = calcService.add(1, 1);

        // then
        assertThat(result).isEqualTo(2);
    }
}
