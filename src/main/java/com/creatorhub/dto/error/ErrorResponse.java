package com.creatorhub.dto.error;

import com.creatorhub.constant.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String code,
        String message,
        LocalDateTime timestamp,
        String path
) {
    public static ErrorResponse of(ErrorCode errorCode, String path) {
        return new ErrorResponse(
                errorCode.getCode(),
                errorCode.getMessage(),
                LocalDateTime.now(),
                path
        );
    }

    public static ErrorResponse of(String errorCode, String errorMessage, String path) {
        return new ErrorResponse(
                errorCode,
                errorMessage,
                LocalDateTime.now(),
                path
        );
    }
}
