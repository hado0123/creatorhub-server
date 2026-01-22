package com.creatorhub.exception.creation;

import com.creatorhub.constant.ErrorCode;
import lombok.Getter;

@Getter
public class CreationException extends RuntimeException {
    private final ErrorCode errorCode;

    public CreationException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public CreationException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
