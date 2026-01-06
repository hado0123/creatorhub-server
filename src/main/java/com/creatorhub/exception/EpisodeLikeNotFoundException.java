package com.creatorhub.exception;

import com.creatorhub.constant.ErrorCode;

public class EpisodeLikeNotFoundException extends CreatorException {

    public EpisodeLikeNotFoundException() {
        super(ErrorCode.EPISODE_LIKE_NOT_FOUND);
    }

    public EpisodeLikeNotFoundException(String message) {
        super(ErrorCode.EPISODE_LIKE_NOT_FOUND, message);
    }
}
