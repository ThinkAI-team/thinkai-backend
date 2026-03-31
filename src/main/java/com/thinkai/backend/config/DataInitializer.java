package com.thinkai.backend.config;

import com.thinkai.backend.entity.User;
import com.thinkai.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail("admin@gmail.com")) {
            User admin = User.builder()
                    .email("admin@gmail.com")
                    .passwordHash(passwordEncoder.encode("admin@gmail.com"))
                    .fullName("Admin User")
                    .role(User.Role.ADMIN)
                    .isActive(true)
                    .build();
            userRepository.save(admin);
            System.out.println(">>> Admin account created: admin@gmail.com / admin@gmail.com");
        }
    }
}
