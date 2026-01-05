package com.creatorhub.exception;

import com.creatorhub.constant.ErrorCode;
import com.creatorhub.dto.ErrorResponse;
import com.creatorhub.security.exception.JwtAuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;
import java.util.Objects;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Member 관련 예외 처리
     */
    @ExceptionHandler(MemberException.class)
    public ResponseEntity<ErrorResponse> handleMemberException(
            MemberException ex,
            HttpServletRequest request) {
        
        log.warn("MemberException occurred - Message: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(ex.getErrorCode(), request.getRequestURI());

        return ResponseEntity
                .status(ex.getErrorCode().getHttpStatus())
                .body(errorResponse);
    }

    /**
     * Creator 관련 예외 처리
     */
    @ExceptionHandler(CreatorException.class)
    public ResponseEntity<ErrorResponse> handleCreatorException(
            CreatorException ex,
            HttpServletRequest request) {

        log.warn("CreatorException occurred - Message: {}", ex.getMessage());

        ErrorResponse errorResponse =
                ErrorResponse.of(ex.getErrorCode(), request.getRequestURI());

        return ResponseEntity
                .status(ex.getErrorCode().getHttpStatus())
                .body(errorResponse);
    }


    /**
     * 요청 데이터 검증 오류 처리 (@Valid 실패)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        String message = Objects.requireNonNull(ex.getBindingResult().getFieldError())
                        .getDefaultMessage();
        
        log.warn("Validation failed - Message: {}", message);
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.INVALID_REQUEST.getCode(), message, request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    /**
     * 접근 권한이 없는 사용자 예외 처리
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request) {

        log.warn("handleAccessDeniedException occurred - Message: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.ACCESS_DENIED, request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(errorResponse);
    }

    /**
     * JWT 토큰 관련 예외 처리
     */
    @ExceptionHandler(JwtAuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleJwtAuthenticationException(
            JwtAuthenticationException ex,
            HttpServletRequest request) {

        log.warn("handleJwtAuthenticationException occurred - Message: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(ex.getErrorCode(), request.getRequestURI());

        return ResponseEntity
                .status(ex.getErrorCode().getHttpStatus())
                .body(errorResponse);
    }

    /**
     * File 관련 예외 처리
     */
    @ExceptionHandler(FileObjectException.class)
    public ResponseEntity<ErrorResponse> handleFileObjectException(
            CreatorException ex,
            HttpServletRequest request) {

        log.warn("FileObjectException occurred - Message: {}", ex.getMessage());

        ErrorResponse errorResponse =
                ErrorResponse.of(ex.getErrorCode(), request.getRequestURI());

        return ResponseEntity
                .status(ex.getErrorCode().getHttpStatus())
                .body(errorResponse);
    }

    /**
     * 예상하지 못한 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            HttpServletRequest request) {
        
        log.error("Unexpected exception occurred", ex);
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }

    /**
     * 경로 관련 예외처리
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(
            NoHandlerFoundException ex,
            HttpServletRequest request) {

        log.warn("handleNoHandlerFoundException occurred - Message: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.INVALID_PATH, request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse);
    }
}
