package com.pagoda.matchmeal.common.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AWS S3 설정 클래스
 * - 이미지 업로드/다운로드를 위한 AmazonS3 클라이언트를 빈으로 등록
 */
@Configuration
public class S3Config {

    @Value("${cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${cloud.aws.region.static}")
    private String region;

    /**
     * AmazonS3 클라이언트 생성
     * application.yml에서 주입받은 AccessKey, SecretKey, Region 정보를 사용하여 S3 클라이언트를 빌드
     *
     * @return AmazonS3 클라이언트 객체
     */
    @Bean
    public AmazonS3 amazonS3Client() {
        System.out.println(">>> S3 Region Setting: " + region);
        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
                .withRegion(region)
                .build();

    }
}
