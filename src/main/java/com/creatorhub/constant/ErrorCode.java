package com.creatorhub.constant;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // Member 관련 에러
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "M001", "이미 가입된 이메일입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M002", "회원을 찾을 수 없습니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "M003", "비밀번호가 올바르지 않습니다."),

    // Creator 관련 에러
    ALREADY_CREATOR(HttpStatus.CONFLICT, "C001", "이미 존재하는 작가입니다."),
    CREATOR_NOT_FOUND(HttpStatus.NOT_FOUND, "C002", "존재하는 않는 작가입니다."),

    // Authorization 관련 에러
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "A001", "접근이 제한되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "유효하지 않은 Access토큰입니다."),
    EXPIRE_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "Access 토큰 유효기간이 만료되었습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A004", "유효하지 않은 Refresh 토큰입니다."),
    EXPIRE_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A005", "Refresh 토큰 유효기간이 만료되었습니다."),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "A006", "토큰 인증에 실패했습니다."),

    // FileObject 관련 에러
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "F001", "존재하는 파일입니다."),
    FILE_STATUS_NOT_CORRECT(HttpStatus.CONFLICT, "F002", "파일의 상태가 일치하지 않거나 올바르지 못합니다."),

    // 기타 에러
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "E001", "잘못된 형식입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E002", "서버 오류가 발생했습니다."),
    INVALID_PATH(HttpStatus.NOT_FOUND, "E003", "잘못된 경로입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
