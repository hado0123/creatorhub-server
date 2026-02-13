package com.creatorhub.exception.episode.rating;

import com.creatorhub.constant.ErrorCode;
import com.creatorhub.exception.episode.EpisodeException;

public class AlreadyEpisodeRatingException extends EpisodeException {

    public AlreadyEpisodeRatingException() {
        super(ErrorCode.ALREADY_EPISODE_RATING);
    }

    public AlreadyEpisodeRatingException(String message) {
        super(ErrorCode.ALREADY_EPISODE_RATING, message);
    }
}
