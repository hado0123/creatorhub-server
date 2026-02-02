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
    ALREADY_CREATOR(HttpStatus.CONFLICT, "MC001", "이미 존재하는 작가입니다."),
    CREATOR_NOT_FOUND(HttpStatus.NOT_FOUND, "MC002", "존재하는 않는 작가입니다."),

    // Creation 관련 에러
    ALREADY_CREATION(HttpStatus.CONFLICT, "C001", "이미 존재하는 작품입니다."),
    CREATION_NOT_FOUND(HttpStatus.NOT_FOUND, "C002", "존재하는 않는 작품입니다."),
    ALREADY_CREATION_FAVORITE(HttpStatus.CONFLICT, "C003", "이미 관심작품으로 등록한 작품입니다."),
    CREATION_FAVORITE_NOT_FOUND(HttpStatus.NOT_FOUND, "C004", "관심 작품으로 등록되어있지 않습니다."),

    // Episode 관련 에러
    ALREADY_EPISODE(HttpStatus.CONFLICT, "E001", "이미 존재하는 회차입니다."),
    EPISODE_NOT_FOUND(HttpStatus.NOT_FOUND, "E002", "존재하는 않는 회차입니다."),
    ALREADY_EPISODE_LIKE(HttpStatus.CONFLICT, "E003", "이미 '좋아요'한 회차입니다."),
    EPISODE_LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "E004", "'좋아요'를 하지 않은 회차입니다."),
    ALREADY_EPISODE_RATING(HttpStatus.CONFLICT, "E003", "이미 별점을 준 회차입니다."),

    // Authorization 관련 에러
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "A001", "접근이 제한되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "유효하지 않은 Access토큰입니다."),
    EXPIRE_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "Access 토큰 유효기간이 만료되었습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A004", "유효하지 않은 Refresh 토큰입니다."),
    EXPIRE_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A005", "Refresh 토큰 유효기간이 만료되었습니다."),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "A006", "토큰 인증에 실패했습니다."),

    // FileObject 관련 에러
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "F001", "존재하지 않는 파일입니다."),
    FILE_STATUS_NOT_CORRECT(HttpStatus.CONFLICT, "F002", "파일의 상태가 일치하지 않거나 올바르지 못합니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, "F003", "파일의 사이즈가 제한 용량을 초과했습니다."),

    // s3 관련 에러
    PRESIGNED_URL_ISSUE(HttpStatus.BAD_GATEWAY, "P001", "Presigned URL 발급에 실패했습니다."),

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
