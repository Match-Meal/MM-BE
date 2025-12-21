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

/**
 * AWS S3 파일 관리 서비스 구현체
 */
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
     * @param file    업로드할 파일 객체 (MultipartFile)
     * @param dirName S3 버킷 내 저장할 디렉토리 경로 (예: "profile", "post")
     * @return 업로드된 파일의 전체 접근 URL (HTTPS)
     */
    @Override
    public String uploadFile(MultipartFile file, String dirName) {
        if (file.isEmpty()) {
            throw new CustomException(ErrorResponseCode.INVALID_FILE);
        }

        String originalFilename = file.getOriginalFilename();
        String fileName = dirName + "/" + UUID.randomUUID() + "_" + originalFilename.replaceAll("\\s", "_");

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(file.getSize());
        objectMetadata.setContentType(file.getContentType());

        try (InputStream inputStream = file.getInputStream()) {
            amazonS3.putObject(new PutObjectRequest(bucket, fileName, inputStream, objectMetadata));
        } catch (IOException e) {
            throw new CustomException(ErrorResponseCode.FILE_UPLOAD_ERROR);
        }

        return amazonS3.getUrl(bucket, fileName).toString();
    }

    /**
     * S3 파일 삭제
     *
     * @param fileUrl 삭제할 파일의 전체 URL (예: https://.../profile/abc.jpg)
     */
    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        try {
            String splitStr = ".com/";
            int index = fileUrl.lastIndexOf(splitStr);

            if (index == -1) {
                log.warn("S3 삭제 실패: 올바르지 않은 URL 형식입니다. ({})", fileUrl);
                return;
            }

            String fileName = fileUrl.substring(index + splitStr.length());
            String decodedFileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8);

            amazonS3.deleteObject(bucket, decodedFileName);
            log.info("S3 파일 삭제 완료: {}", decodedFileName);

        } catch (Exception e) {
            log.error("S3 파일 삭제 중 오류 발생: {}", e.getMessage());
        }
    }
}