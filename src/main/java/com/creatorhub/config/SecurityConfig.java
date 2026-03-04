package com.creatorhub.config;

import com.creatorhub.security.filter.JWTCheckFilter;
import com.creatorhub.security.handler.JwtAuthenticationEntryPoint;
import com.creatorhub.security.utils.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.withDefaults;

@Configuration
@Slf4j
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JWTUtil jwtUtil;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public JWTCheckFilter jwtCheckFilter() {
        return new JWTCheckFilter(jwtUtil, jwtAuthenticationEntryPoint);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        log.debug("Security filter chain.....");

        // CORS 설정 적용 (WebConfig의 corsConfigurationSource 사용)
        httpSecurity.cors(cors -> cors.configurationSource(corsConfigurationSource));

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

        PathPatternRequestMatcher.Builder path = withDefaults();

        // 나머지 인가 설정
        httpSecurity.authorizeHttpRequests(auth -> auth
                // auth / signup
                .requestMatchers("/api/auth/login", "/api/auth/refresh", "/api/members/signup").permitAll()

                // 내 작품은 무조건 인증
                .requestMatchers(path.matcher(HttpMethod.GET, "/api/creations/my")).authenticated()

                // 공개 조회 API (GET만)
                .requestMatchers(path.matcher(HttpMethod.GET, "/api/creations/by-days")).permitAll()
                .requestMatchers(path.matcher(HttpMethod.GET, "/api/creations/{creationId}")).permitAll()

                // episodes 공개 조회
                .requestMatchers(path.matcher(HttpMethod.GET, "/api/episodes/creation/{creationId}")).permitAll()
                .requestMatchers(path.matcher(HttpMethod.GET, "/api/episodes/{creationId}/detail/{episodeId}")).permitAll()

                .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                .requestMatchers("/actuator/**").denyAll()

                .requestMatchers("/api/files/resize-complete", "/error").permitAll()
                .anyRequest().authenticated()
        );
        return httpSecurity.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
