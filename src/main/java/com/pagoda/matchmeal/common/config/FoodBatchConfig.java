package com.pagoda.matchmeal.common.config;

import com.pagoda.matchmeal.model.dto.FoodCsvDto;
import com.pagoda.matchmeal.model.entity.Food;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.batch.MyBatisBatchItemWriter;
import org.mybatis.spring.batch.builder.MyBatisBatchItemWriterBuilder;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileParseException;
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

    /**
     * 배치의 상태(시작, 종료, 실패 등)를 저장하고 관리하는 저장소
     */
    private final JobRepository jobRepository;
    /**
     * 데이터베이스 트랜잭션 관리자 (Chunk 단위로 커밋/롤백 처리)
     */
    private final PlatformTransactionManager transactionManager;
    /**
     * MyBatis와 DB를 연결해주는 핵심 객체
     */
    private final SqlSessionFactory sqlSessionFactory;

    // --- 1. Job & Step ---

    /**
     * 배치 작업(Job)을 생성합니다.
     * - Job 이름: "foodJob"
     * - 이 Job은 'foodStep'이라는 하나의 단계를 실행합니다.
     */
    @Bean
    public Job foodJob() {
        // 기존: "foodJob" -> 수정: "foodJob_v2"
        return new JobBuilder("foodJob_v2", jobRepository)
                .start(foodStepA())
                .next(foodStepB())
                .incrementer(new RunIdIncrementer())
                .build();
    }

    /**
     * 실제 비즈니스 로직이 실행되는 단계(Step)입니다.
     * - Chunk 지향 처리 방식을 사용합니다.
     * - <FoodCsvDto, Food>: 입력(CSV)은 DTO로 받고, 출력(DB)은 Entity로 나갑니다.
     */
    @Bean
    public Step foodStepA() {
        return new StepBuilder("foodStepA", jobRepository)
                // [Chunk 설정]
                // 데이터를 1,000개씩 끊어서 처리합니다.
                // 즉, 1000개를 읽고 가공한 뒤 한 번에 DB에 커밋(Insert)합니다. (성능 최적화 핵심)
                .<FoodCsvDto, Food>chunk(200, transactionManager) // 1000개씩 처리
                .reader(foodReaderA()) // 1. 읽기
                .processor(foodProcessorA()) // 2. 가공
                .writer(foodWriter()) // 3. 쓰기
                .build();
    }

    @Bean
    public Step foodStepB() {
        return new StepBuilder("foodStepB", jobRepository)
                // [Chunk 설정]
                // 데이터를 1,000개씩 끊어서 처리합니다.
                // 즉, 1000개를 읽고 가공한 뒤 한 번에 DB에 커밋(Insert)합니다. (성능 최적화 핵심)
                .<FoodCsvDto, Food>chunk(1000, transactionManager) // 1000개씩 처리
                .reader(foodReaderB()) // 1. 읽기
                .processor(foodProcessorB()) // 2. 가공
                .writer(foodWriter()) // 3. 쓰기
                .faultTolerant() // "나 이제부터 관대해질 거야"
                .skip(FlatFileParseException.class) // "파싱 에러는 봐줄게"
                .skipLimit(100) // "하지만 100개 넘게 에러 나면 그땐 멈춰"
                .build();
    }

    // --- 2. Reader (CSV 읽기) ---

    /**
     * CSV 파일을 한 줄씩 읽어와서 FoodCsvDto 객체로 변환합니다.
     */
    @Bean
    public FlatFileItemReader<FoodCsvDto> foodReaderA() {
        return new FlatFileItemReaderBuilder<FoodCsvDto>()
                .name("foodReader")
                .resource(new ClassPathResource("data/400_Food_DB.csv"))
                .encoding("UTF-8") // 한글 깨짐 방지
                .linesToSkip(1) // 첫 번째 줄(헤더)은 데이터가 아니므로 건너뜀
                .delimited() // 쉼표(,)로 구분된 파일임
                // 0:이름, 1:중량, 2:칼로리, 3:탄수, 5:지방, 6:단백질, 4:당류, 9:나트륨
                .includedFields(0, 1, 2, 3, 5, 6, 4, 9)

                // 위에서 뽑은 순서대로 DTO의 어떤 변수에 넣을지 지정합니다.
                .names("foodName", "servingSize", "calories", "carbohydrate", "fat", "protein", "sugars", "sodium")

                .targetType(FoodCsvDto.class)
                .build();
    }

    // 1. Reader B (새로운 파일용)
    @Bean
    public FlatFileItemReader<FoodCsvDto> foodReaderB() {
        return new FlatFileItemReaderBuilder<FoodCsvDto>()
                .name("readerB")
                .resource(new ClassPathResource("data/50000_Food_DB.csv"))
                .encoding("UTF-8")

                // ★ 중요: 1,2,3줄은 공백/메타데이터, 4줄은 헤더 -> 총 4줄 스킵
                .linesToSkip(4)
                .delimited()
                .includedFields(2, 5, 9, 11, 12, 15, 19, 20, 21, 22, 45)

                // DTO 필드 매핑
                .names("foodCode", "foodName", "category", "servingSize", "unit", "calories", "protein", "fat", "carbohydrate", "sugars", "sodium")

                .targetType(FoodCsvDto.class)
                .build();
    }

    // --- Processor: 변환 및 로직 처리 ---

    /**
     * 읽어온 CSV 데이터(문자열 위주)를 DB 엔티티(올바른 타입)로 변환합니다.
     * - "N/A", "-", 공백 등 더러운 데이터를 0.0으로 정제하는 로직이 포함됩니다.
     */
    @Bean
    @StepScope // ★ 필수: 매 실행마다 index 0부터 시작
    public ItemProcessor<FoodCsvDto, Food> foodProcessorA() {
        return new ItemProcessor<FoodCsvDto, Food>() {

            private int index = 0; // 카운터 변수

            @Override
            public Food process(FoodCsvDto item) throws Exception {
                index++;
                String generatedCode = String.format("A%03d", index); // A001, A002...

                String rawServingSize = item.getServingSize();
                String unit = "g";
                if (rawServingSize != null && rawServingSize.toLowerCase().contains("ml")) {
                    unit = "ml";
                }

                return Food.builder()
                        .userId(null)
                        .foodCode(generatedCode) // A001 적용
                        .foodName(item.getFoodName().replace("_", " "))
                        .category("기타") // 혹은 CSV의 카테고리
                        .servingSize(parseDoubleSafe(rawServingSize))
                        .unit(unit)
                        .calories(parseDoubleSafe(item.getCalories()))
                        .protein(parseDoubleSafe(item.getProtein()))
                        .fat(parseDoubleSafe(item.getFat()))
                        .carbohydrate(parseDoubleSafe(item.getCarbohydrate()))
                        .sugars(parseDoubleSafe(item.getSugars()))
                        .sodium(parseDoubleSafe(item.getSodium()))
                        .build();
            }
        };
    }

    // 2. Processor B (DTO -> Entity 변환)
    @Bean
    public ItemProcessor<FoodCsvDto, Food> foodProcessorB() {
        return item -> {
            String rawUnit = item.getUnit();

            // 1. unit에 #NUM!이 포함되어 있다면 null 반환 (데이터 저장 스킵)
            if (rawUnit != null && rawUnit.contains("#NUM!")) {
                return null;
            }

            // 2. 단위 처리: 값이 null이거나 공백이면 기본값 "g", 그 외엔 원래 값 사용
            String unit = (rawUnit != null && !rawUnit.trim().isEmpty()) ? rawUnit : "g";

            return Food.builder()
                    // 파일에 있는 코드 그대로 사용
                    .foodCode(item.getFoodCode())

                    // 이름 정제 (필요하다면)
                    .foodName(item.getFoodName().replace("_", " "))
                    .category(item.getCategory())

                    // 숫자 변환 (헬퍼 메소드 사용)
                    .servingSize(parseDoubleSafe(item.getServingSize()))
                    .unit(unit)
                    .calories(parseDoubleSafe(item.getCalories()))
                    .protein(parseDoubleSafe(item.getProtein()))
                    .fat(parseDoubleSafe(item.getFat()))
                    .carbohydrate(parseDoubleSafe(item.getCarbohydrate()))
                    .sugars(parseDoubleSafe(item.getSugars()))
                    .sodium(parseDoubleSafe(item.getSodium()))
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
     *
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