package com.creatorhub.dto.s3;


import java.util.List;

public record ManuscriptPresignedResponse(
        List<ManuscriptPresignedUrlResponse> items
) {}