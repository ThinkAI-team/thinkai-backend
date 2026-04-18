package com.thinkai.backend.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityStartupValidator {

    private static final String DEFAULT_JWT_SECRET_PREFIX = "thinkai_default_secret_key";

    private final Environment environment;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @PostConstruct
    public void validate() {
        boolean localProfile = Arrays.stream(environment.getActiveProfiles())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.contains("local"));

        if (!localProfile && (jwtSecret == null || jwtSecret.startsWith(DEFAULT_JWT_SECRET_PREFIX))) {
            throw new IllegalStateException("JWT secret không hợp lệ cho môi trường non-local. Hãy set JWT_SECRET mạnh.");
        }

        if (!localProfile && allowedOrigins.contains("*")) {
            throw new IllegalStateException("CORS_ORIGINS không được chứa * ở môi trường non-local.");
        }

        if (!localProfile && allowedOrigins.toLowerCase(Locale.ROOT).contains("localhost")) {
            log.warn("CORS_ORIGINS đang chứa localhost ở non-local profile: {}", allowedOrigins);
        }
    }
}

