package com.creatorhub.exception.episode;

import com.creatorhub.constant.ErrorCode;

public class AlreadyEpisodeException extends EpisodeException {

    public AlreadyEpisodeException() {
        super(ErrorCode.ALREADY_EPISODE);
    }

    public AlreadyEpisodeException(String message) {
        super(ErrorCode.ALREADY_EPISODE, message);
    }
}
