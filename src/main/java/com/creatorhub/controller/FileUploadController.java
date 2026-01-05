package com.creatorhub.controller;

import com.creatorhub.dto.FileObjectResponse;
import com.creatorhub.dto.S3PresignedUrlRequest;
import com.creatorhub.dto.S3PresignedUrlResponse;
import com.creatorhub.service.FileObjectService;
import com.creatorhub.service.s3.S3PresignedUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileUploadController {
    private final S3PresignedUploadService uploadService;
    private final FileObjectService fileObjectService;

    /**
     * presigned url 요청
     */
    @PostMapping("/presigned-url")
    public S3PresignedUrlResponse createPresignedUrl(@RequestBody S3PresignedUrlRequest req) {
        log.info("Presigned PUT 요청 - contentType={}, thumbnailType={}, originalFilename={}",
                req.contentType(), req.thumbnailType(), req.originalFilename());

        return uploadService.generatePresignedPutUrl(req);
    }

    /**
     * fileObject 작품등록시 이미지 상태 변경(INIT -> READY)
     */
    @PostMapping("/{fileObjectId}/uploaded")
    public void complete(@PathVariable Long fileObjectId) {
        fileObjectService.markReady(fileObjectId);
    }

    /**
     * fileObject 작품등록시 가로 리사이징 이미지 업로드 상태 확인(폴링용) & file_object insert
     */
    @GetMapping("/{fileObjectId}/status")
    public List<FileObjectResponse> getStatus(@PathVariable Long fileObjectId) {
        return fileObjectService.checkAndGetStatus(fileObjectId);
    }
}
