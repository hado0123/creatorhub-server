package com.creatorhub.exception.creator;

import com.creatorhub.constant.ErrorCode;

public class AlreadyEpisodeLikeException extends CreatorException {

    public AlreadyEpisodeLikeException() {
        super(ErrorCode.ALREADY_EPISODE_LIKE);
    }

    public AlreadyEpisodeLikeException(String message) {
        super(ErrorCode.ALREADY_EPISODE_LIKE, message);
    }
}
