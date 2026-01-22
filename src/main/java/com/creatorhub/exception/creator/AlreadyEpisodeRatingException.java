package com.creatorhub.exception.creator;

import com.creatorhub.constant.ErrorCode;

public class AlreadyEpisodeRatingException extends CreatorException {

    public AlreadyEpisodeRatingException() {
        super(ErrorCode.ALREADY_EPISODE_RATING);
    }

    public AlreadyEpisodeRatingException(String message) {
        super(ErrorCode.ALREADY_EPISODE_RATING, message);
    }
}
