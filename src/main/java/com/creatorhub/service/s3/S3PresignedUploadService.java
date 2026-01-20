package com.creatorhub.service.s3;

import com.creatorhub.constant.FileObjectStatus;
import com.creatorhub.constant.ThumbnailKeys;
import com.creatorhub.dto.s3.*;
import com.creatorhub.entity.FileObject;
import com.creatorhub.repository.FileObjectRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class S3PresignedUploadService {

    private final S3Presigner presigner;
    private final FileObjectRepository fileObjectRepository;
    private final String bucket;

    public S3PresignedUploadService(S3Presigner presigner,
                                    FileObjectRepository fileObjectRepository,
                                    @Value("${cloud.aws.s3.bucket}") String bucket) {
        this.presigner = presigner;
        this.fileObjectRepository = fileObjectRepository;
        this.bucket = bucket;
    }

    public ThumbnailPresignedUrlResponse generatePresignedPutUrl(PresignedPutRequest req) {

        // 1. contentType 검증 (지금은 jpeg 고정 정책)
        validateContentType(req.contentType());

        // 2. storageKey 생성
        String storageKey = createThumbnailObjectKey(req.resolveSuffix());

        // 3. FileObject 저장
        FileObject fo = FileObject.create(
                storageKey,
                req.originalFilename(),
                FileObjectStatus.INIT,
                req.contentType(),
                0L
        );
        fileObjectRepository.save(fo);

        // 4. presigned 발급
        PresignedPutObjectRequest presigned = presignPut(storageKey, req.contentType());

        // 5. 응답에 fileObjectId 포함
        return new ThumbnailPresignedUrlResponse(
                fo.getId(),
                presigned.url().toString(),
                storageKey
        );
    }


    public ManuscriptPresignedResponse generateManuscriptPresignedUrls(ManuscriptPresignedRequest req) {

        int count = req.files().size();

        // 1. contentType 검증 + displayOrder 중복 확인
        Set<Integer> seenOrders = new HashSet<>();
        for (ManuscriptFileRequest f : req.files()) {
            validateContentType(f.contentType());

            if (!seenOrders.add(f.displayOrder())) {
                throw new IllegalArgumentException("중복된 displayOrder가 있습니다: " + f.displayOrder());
            }
        }

        // 2. displayOrder로 원고 오름차순 정렬 (같은 순서 보장)
        List<ManuscriptFileRequest> sorted = new ArrayList<>(req.files());
        sorted.sort(Comparator.comparingInt(ManuscriptFileRequest::displayOrder));


        // 3. storageKey + FileObject 생성 (N개)
        List<FileObject> toSave = new ArrayList<>(count);
        List<Integer> displayOrders = new ArrayList<>(count);
        List<String> storageKeys = new ArrayList<>(count);
        List<String> contentTypes = new ArrayList<>(count);

        for (ManuscriptFileRequest fileReq : sorted) {
            int order = fileReq.displayOrder();
            String storageKey = createManuscriptObjectKey(req.creationId(), order);

            FileObject fo = FileObject.create(
                    storageKey,
                    fileReq.originalFilename(),
                    FileObjectStatus.INIT,
                    fileReq.contentType(),
                    0L
            );

            toSave.add(fo);
            displayOrders.add(order);
            storageKeys.add(storageKey);
            contentTypes.add(fileReq.contentType());
        }

        // 4. 저장
        List<FileObject> savedList = fileObjectRepository.saveAll(toSave);

        // 5. presigned url 발급
        List<ManuscriptPresignedUrlResponse> items = new ArrayList<>(count);

        for (int i = 0; i < savedList.size(); i++) {
            String key = storageKeys.get(i);
            String contentType = contentTypes.get(i);

            PresignedPutObjectRequest presigned = presignPut(key, contentType);

            items.add(new ManuscriptPresignedUrlResponse(
                    displayOrders.get(i),
                    savedList.get(i).getId(),
                    presigned.url().toString(),
                    key
            ));
        }

        return new ManuscriptPresignedResponse(items);
    }

    private void validateContentType(String contentType) {
        if (!"image/jpeg".equals(contentType)) {
            throw new IllegalArgumentException("image/jpeg 타입만 허용 가능합니다.");
        }
    }

    private String createThumbnailObjectKey(String suffix) {
        if (suffix == null || suffix.isBlank()) {
            throw new IllegalArgumentException("suffix가 비어있습니다.");
        }
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String base = "upload/" + datePath + "/" + UUID.randomUUID();
        return base + suffix;
    }

    private String createManuscriptObjectKey(Long creationId, int order) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));

        String base = "upload/" + datePath
                + "/" + creationId + "/"
                + String.format("%03d", order)
                + "_" + UUID.randomUUID();

        return base + ThumbnailKeys.MANUSCRIPT_SUFFIX;
    }


    private PresignedPutObjectRequest presignPut(String key, String contentType) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(objectRequest)
                .build();

        return presigner.presignPutObject(presignRequest);
    }
}
