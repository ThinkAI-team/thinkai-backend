package com.thinkai.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestClient;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

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

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/auth/**",
                    "/",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/ai-tutor/**"
                ).permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/courses", "/courses/**").permitAll()
                .anyRequest().authenticated()
            )
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
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
