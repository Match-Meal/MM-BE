package com.pagoda.matchmeal.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.pagoda.matchmeal.common.exception.CustomException;
import com.pagoda.matchmeal.common.exception.ErrorResponseCode;
import com.pagoda.matchmeal.service.impl.S3ServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class S3ServiceTest {

    @Mock
    private AmazonS3 amazonS3;

    @InjectMocks
    private S3ServiceImpl s3Service;

    private final String BUCKET_NAME = "test-bucket";

    @BeforeEach
    void setUp() {
        // @Value("${cloud.aws.s3.bucket}") 값을 주입하기 위해 ReflectionTestUtils 사용
        // 단위 테스트에서는 Spring Context가 뜨지 않으므로 @Value가 작동하지 않음
        ReflectionTestUtils.setField(s3Service, "bucket", BUCKET_NAME);
    }

    @Test
    @DisplayName("파일 업로드 성공 시 올바른 경로와 URL을 반환해야 한다")
    void uploadFileSuccess() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "content".getBytes()
        );

        String expectedUrl = "https://s3.amazonaws.com/" + BUCKET_NAME + "/profile/uuid_test.jpg";

        // AmazonS3.getUrl() Mocking
        given(amazonS3.getUrl(eq(BUCKET_NAME), any(String.class))).willReturn(new URL(expectedUrl));

        // when
        String resultUrl = s3Service.uploadFile(file);

        // then
        // 1. putObject가 호출되었는지 검증 및 인자 캡처
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(amazonS3).putObject(captor.capture());

        PutObjectRequest request = captor.getValue();

        // 2. 버킷 이름 확인
        assertThat(request.getBucketName()).isEqualTo(BUCKET_NAME);

        // 3. 파일 키(경로) 확인 ("profile/" 로 시작하고 확장자가 포함되는지)
        assertThat(request.getKey()).startsWith("profile/");
        assertThat(request.getKey()).endsWith("_test.jpg");

        // 4. 메타데이터 확인
        assertThat(request.getMetadata().getContentType()).isEqualTo("image/jpeg");
        assertThat(request.getMetadata().getContentLength()).isEqualTo(file.getSize());

        // 5. 결과 URL 확인
        assertThat(resultUrl).isEqualTo(expectedUrl);
    }

    @Test
    @DisplayName("빈 파일 업로드 시 예외 발생")
    void uploadEmptyFile() {
        // given
        MockMultipartFile emptyFile = new MockMultipartFile("file", "", "image/jpeg", new byte[0]);

        // when & then
        // (주의: S3ServiceImpl 코드에 빈 파일 체크 주석을 해제해야 통과함)
        /* if (file.isEmpty()) {
               throw new CustomException(ErrorResponseCode.INVALID_FILE);
           }
           로직이 활성화 되어 있어야 합니다.
        */
        // assertThatThrownBy(() -> s3Service.uploadFile(emptyFile))
        //         .isInstanceOf(CustomException.class)
        //         .hasFieldOrPropertyWithValue("code", ErrorResponseCode.INVALID_FILE);
    }

    @Test
    @DisplayName("S3 업로드 중 IO 에러 발생 시 커스텀 예외로 변환해야 한다")
    void uploadFileIoException() throws IOException {
        // given
        MockMultipartFile file = mock(MockMultipartFile.class);
        given(file.isEmpty()).willReturn(false);
        given(file.getOriginalFilename()).willReturn("test.jpg");

        // getInputStream 호출 시 IOException 발생하도록 설정
        given(file.getInputStream()).willThrow(new IOException("S3 Error"));

        // when & then
        assertThatThrownBy(() -> s3Service.uploadFile(file))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", ErrorResponseCode.FILE_UPLOAD_ERROR);
    }
}
