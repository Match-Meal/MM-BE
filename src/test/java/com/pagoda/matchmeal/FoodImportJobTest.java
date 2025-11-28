package com.pagoda.matchmeal;

import com.pagoda.matchmeal.mapper.FoodBatchMapper;
import com.pagoda.matchmeal.model.Entity.Food;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration") // 스프링 컨텍스트 전체 로드 (통합 테스트)
@ActiveProfiles("test") // application-test.yml 설정을 사용
class FoodImportJobTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job foodJob; // BatchConfig에서 Bean으로 등록한 Job 이름

    @Autowired
    private FoodBatchMapper foodMapper; // DB에 잘 들어갔는지 확인할 매퍼

    @Test
    @DisplayName("음식 CSV 데이터가 H2 DB로 정상적으로 들어가는지 확인")
    void foodBatchTest() throws Exception {
        // given
        // 배치를 실행할 때마다 파라미터가 달라야 재실행 가능하므로 시간값을 넣어줍니다.
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        // when
        // 배치 잡 실행!
        JobExecution jobExecution = jobLauncher.run(foodJob, jobParameters);

        // then
        // 1. 배치가 성공적으로 끝났는지 확인 (COMPLETED)
        assertThat(jobExecution.getExitStatus().getExitCode()).isEqualTo("COMPLETED");

        // 2. 실제 DB(H2)에 데이터가 들어갔는지 확인
        // (FoodBatchMapper에 count 쿼리가 없다면 임시로 selectAll().size() 등으로 확인 가능하지만,
        //  보통 테스트용으로 갯수 세는 메소드를 하나 만들거나, 특정 데이터 조회를 시도합니다.)
        
        // 예시: 매퍼에 쿼리가 없다고 가정하고 간단한 검증 (실제로는 매퍼에 메소드가 있어야 함)
        // int count = foodMapper.countAll(); 
        // assertThat(count).isGreaterThan(0); 
        
        System.out.println("====== 배치 테스트 성공! Status: " + jobExecution.getExitStatus());

        List<Food> foods = foodMapper.findAll();
        System.out.println("===== 저장된 데이터 확인 (" + foods.size() + "개) =====");
        for (Food food : foods) {
            System.out.println("음식명: " + food.getFoodName() + " / 제공량: " + food.getServingSize() + "/ 단위: " + food.getUnit() + " / 칼로리: " + food.getCalories() + " / 탄수화물: " + food.getCarbohydrate() + " / 지방: " + food.getFat() + " / 단백질: " + food.getProtein());
        }
        System.out.println(foodMapper.countAll());
    }
}