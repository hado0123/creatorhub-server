package com.creatorhub.exception.creator;

import com.creatorhub.constant.ErrorCode;

public class CreationFavoriteNotFoundException extends CreatorException {

    public CreationFavoriteNotFoundException() {
        super(ErrorCode.CREATION_FAVORITE_NOT_FOUND);
    }

    public CreationFavoriteNotFoundException(String message) {
        super(ErrorCode.CREATION_FAVORITE_NOT_FOUND, message);
    }
}
