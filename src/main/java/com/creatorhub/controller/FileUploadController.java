package com.creatorhub.controller;

import com.creatorhub.common.sse.SseEmitters;
import com.creatorhub.dto.fileUpload.ManuscriptsMarkResult;
import com.creatorhub.dto.fileUpload.FileObjectResponse;
import com.creatorhub.dto.fileUpload.ThumbnailMarkResult;
import com.creatorhub.dto.s3.*;
import com.creatorhub.service.FileObjectService;
import com.creatorhub.service.s3.S3PresignedUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;


@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileUploadController {
    private final S3PresignedUploadService uploadService;
    private final FileObjectService fileObjectService;
    private final SseEmitters sseEmitters;


    /**
     * 작품 썸네일 presigned url 요청
     */
    @PreAuthorize("hasRole('ROLE_CREATOR')")
    @PostMapping("/creation-thumbnails/presigned")
    public ThumbnailPresignedUrlResponse createCreationThumbnailPresignedUrl(@Valid @RequestBody CreationThumbnailPresignedRequest req) {
        return uploadService.generatePresignedPutUrl(req);
    }

    /**
     * 회차 썸네일 presigned url 요청
     */
    @PreAuthorize("hasRole('ROLE_CREATOR')")
    @PostMapping("/episode-thumbnails/presigned")
    public ThumbnailPresignedUrlResponse createEpisodeThumbnailPresignedUrl(@Valid @RequestBody EpisodeThumbnailPresignedRequest req) {
        return uploadService.generatePresignedPutUrl(req);
    }

    /**
     * 원고 presigned url 요청
     */
    @PreAuthorize("hasRole('ROLE_CREATOR')")
    @PostMapping("/manuscripts/presigned")
    public ManuscriptPresignedResponse createManuscriptPresignedUrls(@Valid @RequestBody ManuscriptPresignedRequest req) {
        return uploadService.generateManuscriptPresignedUrls(req);
    }

    /**
     * fileObject 썸네일 이미지 상태 변경(INIT -> READY)
     */
    @PreAuthorize("hasRole('ROLE_CREATOR')")
    @PostMapping("/{fileObjectId}/thumbnails/ready")
    public ResponseEntity<ThumbnailMarkResult> markThumbnailReady(@PathVariable Long fileObjectId) {
        ThumbnailMarkResult thumbnailMarkResult
                = fileObjectService.markThumbnailReady(fileObjectId);

        return ResponseEntity.ok(thumbnailMarkResult);
    }

    /**
     * fileObject 원고 이미지 상태 변경(INIT -> READY)
     */
    @PreAuthorize("hasRole('ROLE_CREATOR')")
    @PostMapping("/manuscripts/ready")
    public ResponseEntity<ManuscriptsMarkResult> markManuscriptsReady(@Valid @RequestBody ManuscriptReadyRequest req) {
        ManuscriptsMarkResult manuscriptsMarkResult
                = fileObjectService.markManuscriptsReady(req.fileObjectIds());

        return ResponseEntity.ok(manuscriptsMarkResult);
    }

    /**
     * fileObject 이미지 상태 변경(INIT -> FAILED)
     */
    @PreAuthorize("hasRole('ROLE_CREATOR')")
    @PostMapping("/{fileObjectId}/failed")
    public ResponseEntity<Void> markManuscriptsReady(@PathVariable Long fileObjectId) {
        fileObjectService.markFailed(fileObjectId);
        return ResponseEntity.ok().build();
    }

    /**
     * fileObject 작품등록시 리사이징 이미지 콜백용 & file_object insert
     */
    @PostMapping("/resize-complete")
    public ResponseEntity<Void> resizeComplete(@Valid @RequestBody ResizeCompleteRequest req) {
        log.info(
                "이미지 리사이즈 callback received - triggerKey={}, baseKey={}, derivedCount={}",
                req.triggerKey(),
                req.baseKey(),
                req.derivedFiles().size()
        );

        List<FileObjectResponse> result = fileObjectService.resizeComplete(req);

        // SSE send
        String baseKey = req.baseKey();
        if (baseKey != null && !baseKey.isBlank()) {
            sseEmitters.send(baseKey, "resize-complete", result);
        }

        return ResponseEntity.ok().build();
    }
}
