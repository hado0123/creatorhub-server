package com.creatorhub.dto.fileUpload.s3;


import java.util.List;

public record ManuscriptPresignedResponse(
        List<ManuscriptPresignedUrlResponse> items
) {}