package com.creatorhub.exception.hashtag;

import com.creatorhub.constant.ErrorCode;

public class HashNotFoundException extends HashtagException {

    public HashNotFoundException() {
        super(ErrorCode.HASHTAG_NOT_FOUND);
    }

    public HashNotFoundException(String message) {
        super(ErrorCode.HASHTAG_NOT_FOUND, message);
    }
}
