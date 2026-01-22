package com.creatorhub.config;

import com.creatorhub.security.filter.JWTCheckFilter;
import com.creatorhub.security.handler.JwtAuthenticationEntryPoint;
import com.creatorhub.security.utils.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@Slf4j
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JWTUtil jwtUtil;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public JWTCheckFilter jwtCheckFilter() {
        return new JWTCheckFilter(jwtUtil, jwtAuthenticationEntryPoint);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        log.debug("Security filter chain.....");

        // 로그인 페이지 사용 X
        httpSecurity.formLogin(AbstractHttpConfigurer::disable);

        // 로그아웃 사용 X
        httpSecurity.logout(AbstractHttpConfigurer::disable);

        // CSRF 사용 X(stateless 적용)
        httpSecurity.csrf(AbstractHttpConfigurer::disable);

        // HttpSession 사용 X(stateless 적용)
        httpSecurity.sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        // 인증 실패(401 Unauthorized) 발생 시 처리할 핸들러 등록
        httpSecurity.exceptionHandling(ex -> ex
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
        );

        // JWTCheckFilter를 UsernamePasswordAuthenticationFilter보다 먼저 실행하도록 등록
        httpSecurity.addFilterBefore(jwtCheckFilter(), UsernamePasswordAuthenticationFilter.class);

        // 나머지 인가 설정
        httpSecurity.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/api/auth/login",
                        "/api/auth/refresh",
                        "/api/members/signup",
                        "/api/files/resize-complete",
                        "/error"
                ).permitAll()
                .anyRequest().authenticated()
        );
        return httpSecurity.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
