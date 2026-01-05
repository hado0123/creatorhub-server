package com.creatorhub.exception;

import com.creatorhub.constant.ErrorCode;

public class FileObjectNotFoundException extends CreatorException {

    public FileObjectNotFoundException() {
        super(ErrorCode.FILE_NOT_FOUND);
    }

    public FileObjectNotFoundException(String message) {
        super(ErrorCode.FILE_NOT_FOUND, message);
    }
}
