package com.creatorhub.exception.episode;

import com.creatorhub.constant.ErrorCode;

public class EpisodeNotFoundException extends EpisodeException {

    public EpisodeNotFoundException() {
        super(ErrorCode.EPISODE_NOT_FOUND);
    }

    public EpisodeNotFoundException(String message) {
        super(ErrorCode.EPISODE_NOT_FOUND, message);
    }
}
