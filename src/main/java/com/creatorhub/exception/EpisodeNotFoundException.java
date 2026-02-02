package com.creatorhub.exception;

import com.creatorhub.constant.ErrorCode;

public class EpisodeNotFoundException extends CreatorException {

    public EpisodeNotFoundException() {
        super(ErrorCode.EPISODE_NOT_FOUND);
    }

    public EpisodeNotFoundException(String message) {
        super(ErrorCode.EPISODE_NOT_FOUND, message);
    }
}
