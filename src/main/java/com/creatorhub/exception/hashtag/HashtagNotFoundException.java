package com.creatorhub.exception.hashtag;

import com.creatorhub.constant.ErrorCode;

public class HashtagNotFoundException extends HashtagException {

    public HashtagNotFoundException() {
        super(ErrorCode.HASHTAG_NOT_FOUND);
    }

    public HashtagNotFoundException(String message) {
        super(ErrorCode.HASHTAG_NOT_FOUND, message);
    }
}
