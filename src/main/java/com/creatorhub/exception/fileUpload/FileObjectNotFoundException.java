package com.creatorhub.exception.fileUpload;

import com.creatorhub.constant.ErrorCode;

public class FileObjectNotFoundException extends FileObjectException {

    public FileObjectNotFoundException() {
        super(ErrorCode.FILE_NOT_FOUND);
    }

    public FileObjectNotFoundException(String message) {
        super(ErrorCode.FILE_NOT_FOUND, message);
    }
}
