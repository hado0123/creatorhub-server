package com.creatorhub.exception;

import com.creatorhub.constant.ErrorCode;

public class FileObjectStatusException extends CreatorException {

    public FileObjectStatusException() {
        super(ErrorCode.FILE_STATUS_NOT_CORRECT);
    }

    public FileObjectStatusException(String message) {
        super(ErrorCode.FILE_STATUS_NOT_CORRECT, message);
    }
}
