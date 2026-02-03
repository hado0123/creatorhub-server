package com.creatorhub.security.filter;

import com.creatorhub.config.CallbackProperties;
import com.creatorhub.security.utils.HmacUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
public class LambdaCallbackAuthFilter extends OncePerRequestFilter {

    private final CallbackProperties props;

    public LambdaCallbackAuthFilter(CallbackProperties props) {
        this.props = props;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 콜백 엔드포인트에만 적용
        return !"/api/files/resize-complete".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        log.info("LambdaCallbackAuthFilter doFilter............ ");
        log.info("LambdaCallback - ENTER uri={} method={}", request.getRequestURI(), request.getMethod());

        String tsHeader = request.getHeader("X-Timestamp");
        String sigHeader = request.getHeader("X-Signature");

        if (tsHeader == null || sigHeader == null) {
            log.warn("LambdaCallback - BLOCK 발생, reason=missing_header");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        long ts;
        try {
            ts = Long.parseLong(tsHeader);
        } catch (Exception e) {
            log.warn("LambdaCallback - BLOCK 발생, reason=bad_timestamp");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        long now = System.currentTimeMillis();
        long skew = Math.abs(now - ts);

        if (skew > props.allowedSkewMillis()) {
            log.warn("LambdaCallback - BLOCK 발생, reason=skew_exceeded skewMs={} allowedMs={}", skew, props.allowedSkewMillis());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // 바디를 한번 읽어서 서명 검증에 쓰고,
        // 컨트롤러(@RequestBody)가 다시 읽을 수 있게 재공급 래퍼로 넘긴다.
        byte[] bodyBytes = StreamUtils.copyToByteArray(request.getInputStream());
        String rawBody = new String(bodyBytes, StandardCharsets.UTF_8);

        String canonical = tsHeader + "." + rawBody;
        String expected = HmacUtil.hmacSha256Hex(props.secret(), canonical);

        if (!HmacUtil.constantTimeEquals(expected, sigHeader)) {
            log.warn("LambdaCallback - BLOCK 발생, reason=signature_mismatch");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        log.info("LambdaCallback PASS");

        // 컨트롤러가 바디를 다시 읽도록 재공급
        CachedBodyHttpServletRequest replayable = new CachedBodyHttpServletRequest(request, bodyBytes);
        filterChain.doFilter(replayable, response);
    }
}
