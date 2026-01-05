package com.creatorhub.exception;

import com.creatorhub.constant.ErrorCode;
import lombok.Getter;

@Getter
public class FileObjectException extends RuntimeException {
    private final ErrorCode errorCode;

    public FileObjectException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public FileObjectException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
