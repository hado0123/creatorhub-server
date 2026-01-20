package com.creatorhub.controller;

import com.creatorhub.dto.FileObjectResponse;
import com.creatorhub.dto.s3.*;
import com.creatorhub.service.FileObjectService;
import com.creatorhub.service.s3.S3PresignedUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileUploadController {
    private final S3PresignedUploadService uploadService;
    private final FileObjectService fileObjectService;

    /**
     * 작품 썸네일 presigned url 요청
     */
    @PostMapping("/creation-thumbnails/presigned")
    public ThumbnailPresignedUrlResponse createCreationThumbnailPresignedUrl(@RequestBody CreationThumbnailPresignedRequest req) {
        log.info("작품 썸네일 Presigned PUT 요청 - contentType={}, thumbnailType={}, originalFilename={}",
                req.contentType(), req.thumbnailType(), req.originalFilename());

        return uploadService.generatePresignedPutUrl(req);
    }

    /**
     * 회차 썸네일 presigned url 요청
     */
    @PostMapping("/episode-thumbnails/presigned")
    public ThumbnailPresignedUrlResponse createEpisodeThumbnailPresignedUrl(@RequestBody EpisodeThumbnailPresignedRequest req) {
        log.info("회차 썸네일 Presigned PUT 요청 - contentType={}, thumbnailType={}, originalFilename={}",
                req.contentType(), req.thumbnailType(), req.originalFilename());

        return uploadService.generatePresignedPutUrl(req);
    }

    /**
     * 원고 presigned url 요청
     */
    @PostMapping("/manuscripts/presigned")
    public ManuscriptPresignedResponse createManuscriptPresignedUrls(
            @Valid @RequestBody ManuscriptPresignedRequest req
    ) {
        // contentType 요약
        Map<String, Long> contentTypeSummary = req.files().stream()
                .collect(Collectors.groupingBy(
                        ManuscriptFileRequest::contentType,
                        Collectors.counting()
                ));

        log.info("원고 Presigned PUT 요청 - creationId={}, count={}, contentTypes={}",
                req.creationId(), req.files().size(), contentTypeSummary);

        return uploadService.generateManuscriptPresignedUrls(req);
    }

    /**
     * fileObject 썸네일 이미지 상태 변경(INIT -> READY)
     */
    @PostMapping("/{fileObjectId}/thumbnails/ready")
    public void markThumbnailReady(@PathVariable Long fileObjectId) {
        fileObjectService.markThumbnailReady(fileObjectId);
    }

    /**
     * fileObject 원고 이미지 상태 변경(INIT -> READY)
     */
    @PostMapping("/manuscripts/ready")
    public void markManuscriptsReady(@RequestBody @Valid ManuscriptReadyRequest req) {
        fileObjectService.markManuscriptsReady(req.fileObjectIds());
    }


    /**
     * fileObject 작품등록시 가로 리사이징 이미지 업로드 상태 확인(폴링용) & file_object insert
     */
    @GetMapping("/{fileObjectId}/status")
    public List<FileObjectResponse> getStatus(@PathVariable Long fileObjectId) {
        return fileObjectService.checkAndGetStatus(fileObjectId);
    }
}
