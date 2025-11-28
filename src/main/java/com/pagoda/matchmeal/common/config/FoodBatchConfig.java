package com.pagoda.matchmeal.common.config;

import com.pagoda.matchmeal.model.entity.Food;
import com.pagoda.matchmeal.model.dto.FoodCsvDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.batch.MyBatisBatchItemWriter;
import org.mybatis.spring.batch.builder.MyBatisBatchItemWriterBuilder;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * [음식 데이터 대량 등록 배치 설정]
 * - 역할: CSV 파일을 읽어 데이터를 정제한 후, DB에 대량으로 Insert/Upsert 합니다.
 * - 구조: Reader(CSV 읽기) -> Processor(데이터 변환/정제) -> Writer(DB 저장)
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class FoodBatchConfig {

    /** 배치의 상태(시작, 종료, 실패 등)를 저장하고 관리하는 저장소 */
    private final JobRepository jobRepository;
    /** 데이터베이스 트랜잭션 관리자 (Chunk 단위로 커밋/롤백 처리) */
    private final PlatformTransactionManager transactionManager;
    /** MyBatis와 DB를 연결해주는 핵심 객체 */
    private final SqlSessionFactory sqlSessionFactory;

    // --- 1. Job & Step ---
    /**
     * 배치 작업(Job)을 생성합니다.
     * - Job 이름: "foodJob"
     * - 이 Job은 'foodStep'이라는 하나의 단계를 실행합니다.
     */
    @Bean
    public Job foodJob() {
        return new JobBuilder("foodJob", jobRepository)
                .start(foodStep())
                .build();
    }

    /**
     * 실제 비즈니스 로직이 실행되는 단계(Step)입니다.
     * - Chunk 지향 처리 방식을 사용합니다.
     * - <FoodCsvDto, Food>: 입력(CSV)은 DTO로 받고, 출력(DB)은 Entity로 나갑니다.
     */
    @Bean
    public Step foodStep() {
        return new StepBuilder("foodStep", jobRepository)
                // [Chunk 설정]
                // 데이터를 1,000개씩 끊어서 처리합니다.
                // 즉, 1000개를 읽고 가공한 뒤 한 번에 DB에 커밋(Insert)합니다. (성능 최적화 핵심)
                .<FoodCsvDto, Food>chunk(1000, transactionManager) // 1000개씩 처리
                .reader(foodReader()) // 1. 읽기
                .processor(foodProcessor()) // 2. 가공
                .writer(foodWriter()) // 3. 쓰기
                .build();
    }

    // --- 2. Reader (CSV 읽기) ---
    /**
     * CSV 파일을 한 줄씩 읽어와서 FoodCsvDto 객체로 변환합니다.
     */
    @Bean
    public FlatFileItemReader<FoodCsvDto> foodReader() {
        return new FlatFileItemReaderBuilder<FoodCsvDto>()
                .name("foodReader")
                .resource(new ClassPathResource("data/20250408_FoodDB.csv"))
                .encoding("UTF-8") // 한글 깨짐 방지
                .linesToSkip(1) // 첫 번째 줄(헤더)은 데이터가 아니므로 건너뜀
                .delimited() // 쉼표(,)로 구분된 파일임
                // 0:코드, 1:이름, 7:대분류, 16:기준량, 17:에너지, 19:단백질, 20:지방, 22:탄수화물
                .includedFields(0, 1, 7, 16, 17, 19, 20, 22)

                // 위에서 뽑은 순서대로 DTO의 어떤 변수에 넣을지 지정합니다.
                .names("foodCode", "foodName", "category", "servingSize", "calories", "protein", "fat", "carbohydrate")

                .targetType(FoodCsvDto.class)
                .build();
    }

    // --- Processor: 변환 및 로직 처리 ---
    /**
     * 읽어온 CSV 데이터(문자열 위주)를 DB 엔티티(올바른 타입)로 변환합니다.
     * - "N/A", "-", 공백 등 더러운 데이터를 0.0으로 정제하는 로직이 포함됩니다.
     */
    @Bean
    public ItemProcessor<FoodCsvDto, Food> foodProcessor() {
        return item -> {

            String rawServingSize = item.getServingSize(); // 예: "200ml", "100g"
            // 단위 판별 로직
            String unit = "g"; // 기본값
            if (rawServingSize != null && rawServingSize.toLowerCase().contains("ml")) {
                 unit = "ml";
            }

            // Builder 패턴을 사용하여 Food 엔티티 생성
            return Food.builder()
                    .foodCode(item.getFoodCode())
                    .foodName(item.getFoodName().replace("_", " ")) // foodName의 언더바(_) 공백 치환
                    .category(item.getCategory())
                    // [데이터 정제]
                    // CSV에는 "1,200"(쉼표), "N/A"(문자), ""(공백) 등이 섞여 있습니다.
                    // 이를 안전하게 Double(숫자)로 바꾸는 헬퍼 메소드를 사용합니다.
                    .servingSize(parseDoubleSafe(rawServingSize))
                    .unit(unit)
                    .calories(parseDoubleSafe(item.getCalories()))
                    .protein(parseDoubleSafe(item.getProtein()))
                    .fat(parseDoubleSafe(item.getFat()))
                    .carbohydrate(parseDoubleSafe(item.getCarbohydrate()))
                    .build();
        };
    }

    // --- 4. Writer (MyBatis Insert) ---
    /**
     * 가공된 Food 데이터를 MyBatis를 통해 DB에 저장합니다.
     * - 실제 쿼리는 resources/mappers/FoodBatchMapper.xml 에 있습니다.
     */
    @Bean
    public MyBatisBatchItemWriter<Food> foodWriter() {
        return new MyBatisBatchItemWriterBuilder<Food>()
                .sqlSessionFactory(sqlSessionFactory)
                .statementId("com.pagoda.matchmeal.mapper.FoodBatchMapper.insertFood") // 매퍼 ID 확인
                .build();
    }

    /**
     * [안전한 숫자 변환기]
     * 공공데이터 특성상 숫자 컬럼에 문자나 특수문자가 섞여 있는 경우가 많습니다.
     * 이를 에러 없이 0.0으로 처리해주는 메소드입니다.
     * @param value CSV에서 읽은 문자열 값 (예: "1,500", "N/A", "-")
     * @return 변환된 double 값 (변환 불가 시 0.0 반환)
     */
    private double parseDoubleSafe(String value) {
        if (value == null || value.trim().isEmpty() || value.equals("-") || value.equals("N/A")) {
            return 0.0;
        }
        try {
            // 쉼표와 공백 제거
            String cleanValue = value.replace(",", "");

            // "[^0-9.]" 의 의미: "0부터 9까지의 숫자와 점(.)이 '아닌(^) 것'들"
            // 즉, 숫자와 점을 제외한 모든 문자(g, ml, 공백, 한글 등)를 빈 문자열("")로 치환해서 지워버립니다.
            cleanValue = cleanValue.replaceAll("[^0-9.]", "");

            // 단위를 지웠더니 빈 문자열이 된 경우 (예: "g"만 있었던 오타 등)
            if (cleanValue.isEmpty()) {
                return 0.0;
            }

            return Double.parseDouble(cleanValue);
        } catch (NumberFormatException e) {
            log.warn("숫자 변환 실패: {}", value);
            return 0.0; // 변환 실패 시 0.0 처리
        }
    }
}