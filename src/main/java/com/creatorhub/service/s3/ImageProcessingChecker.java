package com.creatorhub.service.s3;

import com.creatorhub.constant.ThumbnailKeys;
import com.creatorhub.dto.DerivativesCheckResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * 단일 파일 사이즈 읽기
     */
    public long fetchSize(String key) {
        return s3Client.headObject(b -> b.bucket(bucket).key(key)).contentLength();
    }


}