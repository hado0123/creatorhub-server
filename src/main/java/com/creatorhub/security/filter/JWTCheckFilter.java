package com.creatorhub.security.filter;

import com.creatorhub.constant.ErrorCode;
import com.creatorhub.constant.Role;
import com.creatorhub.dto.auth.TokenPayload;
import com.creatorhub.security.auth.CustomUserPrincipal;
import com.creatorhub.exception.auth.JwtAuthenticationException;
import com.creatorhub.security.utils.JWTUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class JWTCheckFilter extends OncePerRequestFilter {
    private final JWTUtil jwtUtil;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        // 토큰 없이 접근 허용할 경로들
        return path.equals("/api/auth/login")          // 로그인
                || path.equals("/api/auth/refresh")    // 토큰 재발급
                || path.equals("/api/members/signup") // 회원가입
                || path.equals("/api/files/resize-complete"); // 이미지 리사이즈 완료 콜백
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    )
            throws ServletException, IOException {

        log.debug("JWTCheckFilter doFilter............ ");

        String headerStr = request.getHeader("Authorization");

        // 1) Authorization 헤더가 아예 없으면 → JWT 검사는 건너뛰고 다음으로 넘김
        //    (이 경우, 나중에 인가 단계에서 401 / 혹은 404 처리가 이뤄짐)
        if (headerStr == null || headerStr.isBlank()) {
            filterChain.doFilter(request, response);
            return; // 더 이상 체인 타지 않도록 종료
        }

        // 2) 형식은 있는데 Bearer로 안시작하면 → 잘못된 토큰 형식으로 처리
        if (!headerStr.startsWith("Bearer ")) {
            JwtAuthenticationException authEx =
                    new JwtAuthenticationException(ErrorCode.INVALID_TOKEN);
            authenticationEntryPoint.commence(request, response, authEx);
            return;
        }

        String accessToken = headerStr.substring(7);

        try {
            // 3) Access Token 검증
            TokenPayload payload = jwtUtil.validateToken(accessToken);

            Long id = payload.id();
            Role role = payload.role();

            // 4) Role enum 이 들고 있는 권한 세트로 GrantedAuthority 생성
            List<SimpleGrantedAuthority> authorities = role.getAuthorities().stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            // 5) Authentication 객체 생성
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            new CustomUserPrincipal(id),
                            null,
                            authorities
                    );

            // 6) SecurityContextHolder에 Authentication 객체 저장
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authenticationToken);
            SecurityContextHolder.setContext(context);

            // 7) 다음 필터로 진행
            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            log.warn("JWTCheckFilter - ExpiredJwtException 발생, EXPIRE_TOKEN 응답(토큰 유효기간 만료)", e);

            JwtAuthenticationException authEx =
                    new JwtAuthenticationException(ErrorCode.EXPIRE_TOKEN, e);

            authenticationEntryPoint.commence(request, response, authEx);

        } catch (JwtException e) {
            log.warn("JWTCheckFilter - JwtException 발생, INVALID_TOKEN 응답(유효하지 않는 토큰)", e);

            JwtAuthenticationException authEx =
                    new JwtAuthenticationException(ErrorCode.INVALID_TOKEN, e);

            authenticationEntryPoint.commence(request, response, authEx);
        }
    }
}
