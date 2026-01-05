package com.creatorhub.service.s3;

import com.creatorhub.constant.CreationThumbnailType;
import com.creatorhub.constant.FileObjectStatus;
import com.creatorhub.constant.ThumbnailKeys;
import com.creatorhub.dto.S3PresignedUrlRequest;
import com.creatorhub.dto.S3PresignedUrlResponse;
import com.creatorhub.entity.FileObject;
import com.creatorhub.repository.FileObjectRepository;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class S3PresignedUploadService {

    private static final String BUCKET_NAME = "creatorhub-dev-bucket";

    private final S3Presigner presigner;
    private final FileObjectRepository fileObjectRepository;

    public S3PresignedUploadService(S3Presigner presigner,
                                    FileObjectRepository fileObjectRepository) {
        this.presigner = presigner;
        this.fileObjectRepository = fileObjectRepository;
    }

    public S3PresignedUrlResponse generatePresignedPutUrl(S3PresignedUrlRequest req) {

        // 1. 검증 (지금은 jpeg 고정 정책)
        if (!"image/jpeg".equals(req.contentType())) {
            throw new IllegalArgumentException("image/jpeg 타입만 허용 가능합니다.");
        }

        // 2. storageKey 생성
        String storageKey = createObjectKey(req.thumbnailType());

        // 3. FileObject 생성
        FileObject fileObject = FileObject.create(
                storageKey,
                req.originalFilename(),
                FileObjectStatus.INIT,
                req.contentType(),
                0L
        );
        FileObject saved = fileObjectRepository.save(fileObject);

        // 4. presigned 발급
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(storageKey)
                .contentType(req.contentType())
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(2))
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);

        // 5. 응답에 fileObjectId 포함
        return new S3PresignedUrlResponse(
                saved.getId(),
                presignedRequest.url().toString(),
                storageKey
        );
    }

    private String createObjectKey(CreationThumbnailType type) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String base = "upload/" + datePath + "/" + UUID.randomUUID();

        return switch (type) {
            case POSTER -> base + ThumbnailKeys.POSTER_SUFFIX;
            case HORIZONTAL -> base + ThumbnailKeys.HORIZONTAL_SUFFIX; // Lambda 트리거
            case DERIVED -> null;
            case EXTRA -> null;
        };
    }
}
