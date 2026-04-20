package com.thinkai.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestClient;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * GLOBAL SECURITY CONFIG - Lõi bảo mật duy nhất của hệ thống
 * ============================================================================
 *
 * ⚠️ FILE NÀY ĐƯỢC "ĐÓNG BĂNG" - CHỈ BÌNH MINH (AUTH/DEVOPS) ĐƯỢC SỬA ⚠️
 *
 * Chiến lược phân quyền: Annotation-based Security (@EnableMethodSecurity)
 * - File này CHỈ chứa: CORS, JWT Filter, permitAll cho Auth endpoints
 * - Mọi endpoint khác mặc định yêu cầu Token hợp lệ (authenticated)
 * - Phân quyền theo Role: Mỗi dev tự gắn @PreAuthorize trên Controller
 *
 * Các annotation có sẵn (package com.thinkai.backend.security):
 * - @AdminOnly       → hasRole('ADMIN')
 * - @TeacherOnly     → hasRole('TEACHER')
 * - @StudentOnly     → hasRole('STUDENT')
 * - @TeacherOrAdmin  → hasAnyRole('TEACHER', 'ADMIN')
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class GlobalSecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final SecurityHeadersFilter securityHeadersFilter;

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(unauthorizedEntryPoint())
                    .accessDeniedHandler(accessDeniedHandler()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/auth/register",
                    "/auth/login",
                    "/auth/google",
                    "/auth/forgot-password",
                    "/auth/reset-password",
                    "/",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/ai-tutor/**",
                    "/ai-harness/**",
                    "/notifications/stream",
                    "/api/files/**",
                    "/api/v1/payments/webhook",
                    "/api/v1/payments/webhook-test",
                    "/api/public/**",
                    "/actuator/prometheus",
                    "/actuator/health"
                ).permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/courses", "/courses/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(securityHeadersFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toList()));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        config.setExposedHeaders(Arrays.asList("X-RateLimit-Remaining", "X-RateLimit-Reset"));
        config.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) -> writeUnauthorizedResponse(response, authException);
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"Bạn không có quyền truy cập tài nguyên này\"}"
            );
        };
    }

    private void writeUnauthorizedResponse(
            jakarta.servlet.http.HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Vui lòng đăng nhập để tiếp tục\"}"
        );
    }
}
