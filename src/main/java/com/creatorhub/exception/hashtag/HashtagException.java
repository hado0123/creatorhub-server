package com.creatorhub.exception.hashtag;

import com.creatorhub.constant.ErrorCode;
import lombok.Getter;

@Getter
public class HashtagException extends RuntimeException {
    private final ErrorCode errorCode;

    public HashtagException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public HashtagException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
