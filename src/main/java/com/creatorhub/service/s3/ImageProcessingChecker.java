package com.creatorhub.service.s3;

import com.creatorhub.constant.ThumbnailKeys;
import com.creatorhub.dto.DerivativesCheckResponse;
import com.creatorhub.dto.s3.ResizeCompleteRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ImageProcessingChecker {

    private final S3Client s3Client;
    private final String bucket;


    public ImageProcessingChecker(S3Client s3Client,
                                  @Value("${cloud.aws.s3.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    /**
     * 6개 파생 리사이징 이미지 존재 확인 & 사이즈 읽기
     */
    public DerivativesCheckResponse checkDerivedAndFetchSizes(String baseKey) {
        Map<String, Long> sizeByKey = new HashMap<>(); // S3에 업로드된 이미지 key
        List<String> missingKeys = new ArrayList<>(); // S3에 업로드 실패한 이미지 key

        for (String suffix : ThumbnailKeys.DERIVED_SUFFIXES) {
            String key = baseKey + suffix;

            try {
                long size = s3Client.headObject(b -> b.bucket(bucket).key(key)).contentLength();
                sizeByKey.put(key, size);
            } catch (NoSuchKeyException e) {
                missingKeys.add(key);
                sizeByKey.put(key, 0L);
            } catch (S3Exception e) {
                if (e.statusCode() == 404) {
                    missingKeys.add(key);
                    sizeByKey.put(key, 0L);
                } else {
                    throw e;
                }
            }
        }

        boolean ready = missingKeys.isEmpty();
        return ready ? DerivativesCheckResponse.ready(sizeByKey)
                : DerivativesCheckResponse.notReady(sizeByKey, missingKeys);
    }

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


}