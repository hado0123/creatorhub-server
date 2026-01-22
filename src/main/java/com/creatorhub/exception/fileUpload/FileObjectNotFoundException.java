package com.creatorhub.exception.fileUpload;

import com.creatorhub.constant.ErrorCode;
import com.creatorhub.exception.creator.CreatorException;

public class FileObjectNotFoundException extends CreatorException {

    public FileObjectNotFoundException() {
        super(ErrorCode.FILE_NOT_FOUND);
    }

    public FileObjectNotFoundException(String message) {
        super(ErrorCode.FILE_NOT_FOUND, message);
    }
}
