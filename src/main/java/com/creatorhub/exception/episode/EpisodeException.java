package com.creatorhub.exception.episode;

import com.creatorhub.constant.ErrorCode;
import lombok.Getter;

@Getter
public class EpisodeException extends RuntimeException {
    private final ErrorCode errorCode;

    public EpisodeException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public EpisodeException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
