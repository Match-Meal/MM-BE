package com.pagoda.matchmeal.service.impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.pagoda.matchmeal.common.exception.CustomException;
import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import com.pagoda.matchmeal.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3ServiceImpl implements S3Service {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    /**
     * S3에 파일 업로드
     *
     * @param file 업로드할 파일
     * @return 업로드된 파일의 전체 URL
     */
    @Override
    public String uploadFile(MultipartFile file, String dirName) {
        // 파일 유효성 검사
        if (file.isEmpty()) {
            throw new CustomException(ErrorResponseCode.INVALID_FILE);
        }

        // 파일명 중복 방지 (uuid)
        String originalFilename = file.getOriginalFilename();
        String fileName = dirName + "/" + UUID.randomUUID() + "_" + originalFilename.replaceAll("\\s", "_");

        // 메타 데이터 설정
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(file.getSize());
        objectMetadata.setContentType(file.getContentType());

        // 4. S3 업로드 실행
        try (InputStream inputStream = file.getInputStream()) {
            amazonS3.putObject(new PutObjectRequest(bucket, fileName, inputStream, objectMetadata));
        } catch (IOException e) {
            throw new CustomException(ErrorResponseCode.FILE_UPLOAD_ERROR);
        }

        // 업로드된 파일의 접근 URL 반환
        return amazonS3.getUrl(bucket, fileName).toString();

    }

    /**
     * S3 파일 삭제
     *
     * @param fileUrl 삭제할 파일의 전체 URL
     */
    @Override
    public void deleteFile(String fileUrl) {
        // URL이 없으면 삭제 로직 수행 X
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        try {
            // 1. 전체 URL에서 도메인을 제외한 "Key"(파일 경로) 추출
            // 예: https://s3.ap-northeast-2.amazonaws.com/{bucket}/profile/abc.jpg -> profile/abc.jpg
            // ".com/" 문자열을 기준으로 뒷부분을 잘라냅니다.
            String splitStr = ".com/";
            int index = fileUrl.lastIndexOf(splitStr);

            // URL 형식이 맞지 않으면 로그 남기고 종료 (에러 발생시키지 않음)
            if (index == -1) {
                log.warn("S3 삭제 실패: 올바르지 않은 URL 형식입니다. ({})", fileUrl);
                return;
            }

            // ".com/" 길이(5)만큼 더해서 그 뒤부터 잘라냄
            String fileName = fileUrl.substring(index + splitStr.length());

            // 2. 한글 파일명 등을 위해 디코딩 (필수)
            String decodedFileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8);

            // 3. S3에서 삭제 요청
            amazonS3.deleteObject(bucket, decodedFileName);
            log.info("S3 파일 삭제 완료: {}", decodedFileName);

        } catch (Exception e) {
            // 삭제 실패하더라도 메인 비즈니스 로직(회원 수정 등)에는 영향을 주지 않도록
            // 예외를 던지지 않고 에러 로그만 남깁니다.
            log.error("S3 파일 삭제 중 오류 발생: {}", e.getMessage());
        }
    }
}
