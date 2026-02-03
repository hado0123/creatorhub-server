package com.creatorhub.exception.fileUpload;

import com.creatorhub.constant.ErrorCode;
import com.creatorhub.exception.creator.CreatorException;

public class FileObjectStatusException extends CreatorException {

    public FileObjectStatusException() {
        super(ErrorCode.FILE_STATUS_NOT_CORRECT);
    }

    public FileObjectStatusException(String message) {
        super(ErrorCode.FILE_STATUS_NOT_CORRECT, message);
    }
}
