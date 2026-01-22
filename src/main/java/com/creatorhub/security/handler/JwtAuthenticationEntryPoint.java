package com.creatorhub.security.handler;

import com.creatorhub.constant.ErrorCode;
import com.creatorhub.dto.error.ErrorResponse;
import com.creatorhub.security.exception.JwtAuthenticationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        log.warn("JwtAuthenticationEntryPoint - Unauthorized: {}", authException.getMessage());

        // 예외가 뭐가 들어올지 모르는 상태에서 최소한 AUTHENTICATION_FAILED 처리
        ErrorCode errorCode = ErrorCode.AUTHENTICATION_FAILED;

        // 만약 JWT 관련 예외라면 JwtAuthenticationException 처리
        if (authException instanceof JwtAuthenticationException jwtEx) {
            errorCode = jwtEx.getErrorCode();
        }

        ErrorResponse errorResponse = ErrorResponse.of(errorCode, request.getRequestURI());

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType("application/json;charset=UTF-8");

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
