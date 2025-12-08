package com.pagoda.matchmeal.service;

import org.springframework.web.multipart.MultipartFile;

public interface S3Service {
    String uploadFile(MultipartFile file);
    void deleteFile(String fileUrl); // [추가]
}
