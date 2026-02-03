package com.creatorhub.service.s3;

import com.creatorhub.constant.ThumbnailKeys;
import com.creatorhub.dto.fileUpload.DerivativesCheckResponse;
import com.creatorhub.dto.s3.ResizeCompleteRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.*;
import java.util.stream.Collectors;

import static com.creatorhub.common.logging.LogMasking.maskStoragekey;

@Service
@Slf4j
public class ImageProcessingChecker {

    private final S3Client s3Client;
    private final String bucket;


    public ImageProcessingChecker(S3Client s3Client,
                                  @Value("${cloud.aws.s3.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    /**
     * 6개 파생 리사이징 이미지 사이즈 읽기
     */
    public DerivativesCheckResponse getDerivedSizes(
            String baseKey,
            List<ResizeCompleteRequest.DerivedFile> derivedFiles
    ) {
        // key -> sizeBytes (중복 key가 들어오면 마지막 값으로 덮어씀)
        Map<String, Long> uploadedSizeByKey = derivedFiles.stream()
                .collect(Collectors.toMap(
                        ResizeCompleteRequest.DerivedFile::key,
                        ResizeCompleteRequest.DerivedFile::sizeBytes
                ));

        Map<String, Long> sizeByKey = new LinkedHashMap<>();
        List<String> missingKeys = new ArrayList<>();

        for (String suffix : ThumbnailKeys.DERIVED_SUFFIXES) {
            String key = baseKey + suffix;

            Long size = uploadedSizeByKey.get(key);
            if (size == null) {
                sizeByKey.put(key, 0L);
                missingKeys.add(key);
            } else {
                sizeByKey.put(key, size);
            }
        }

        return missingKeys.isEmpty()
                ? DerivativesCheckResponse.ready(sizeByKey)
                : DerivativesCheckResponse.notReady(sizeByKey, missingKeys);
    }

    /**
     * 단일 파일 사이즈 읽기
     */
    public long fetchSize(String key) {
        return s3Client.headObject(b -> b.bucket(bucket).key(key)).contentLength();
    }

    /**
     * 파일 삭제
     */
    public void deleteObject(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        } catch (S3Exception e) {
            log.warn("S3 파일 삭제 실패 storageKeyTail={}, awsMessage={}",
                    maskStoragekey(key),
                    e.getMessage());
        }
    }

}