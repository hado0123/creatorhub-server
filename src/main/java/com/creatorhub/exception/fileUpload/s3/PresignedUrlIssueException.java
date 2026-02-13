package com.creatorhub.exception.fileUpload.s3;

import com.creatorhub.constant.ErrorCode;
import lombok.Getter;

@Getter
public class PresignedUrlIssueException  extends RuntimeException {
    private final ErrorCode errorCode;

    public PresignedUrlIssueException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = ErrorCode.PRESIGNED_URL_ISSUE;
    }

    public PresignedUrlIssueException(String message) {
        super(message);
        this.errorCode = ErrorCode.PRESIGNED_URL_ISSUE;
    }

}
