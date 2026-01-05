package com.creatorhub.exception;

import com.creatorhub.constant.ErrorCode;
import lombok.Getter;

@Getter
public class CreatorException extends RuntimeException {
    private final ErrorCode errorCode;

    public CreatorException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public CreatorException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
