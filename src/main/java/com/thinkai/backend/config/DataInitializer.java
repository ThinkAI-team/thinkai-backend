package com.thinkai.backend.config;

import com.thinkai.backend.entity.User;
import com.thinkai.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public DataInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        ensureLessonProgressColumns();
        ensureExamColumns();
        ensureExamAttemptColumns();
        ensureUserMemoryColumns();

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

    private void ensureLessonProgressColumns() {
        addColumnIfMissing("progress_percent", "INT NOT NULL DEFAULT 0");
        addColumnIfMissing("current_time_seconds", "INT NULL DEFAULT 0");
        addColumnIfMissing("started_at", "DATETIME NULL");
    }

    private void ensureExamColumns() {
        addExamColumnIfMissing("description", "TEXT NULL");
        addExamColumnIfMissing("exam_type", "VARCHAR(20) NOT NULL DEFAULT 'TOEIC'");
        addExamColumnIfMissing("total_questions", "INT NOT NULL DEFAULT 0");
        addExamColumnIfMissing("duration", "INT NOT NULL DEFAULT 120");
        addExamColumnIfMissing("is_random_order", "BOOLEAN NOT NULL DEFAULT FALSE");
        addExamColumnIfMissing("part_config", "JSON NULL");
    }

    private void ensureExamAttemptColumns() {
        addExamAttemptColumnIfMissing("correct_answers", "INT NOT NULL DEFAULT 0");
    }

    private void ensureUserMemoryColumns() {
        addUserMemoryColumnIfMissing("adaptive_rules", "TEXT NULL");
    }

    private void addColumnIfMissing(String columnName, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'lesson_progress'
                  AND COLUMN_NAME = ?
                """,
                Integer.class,
                columnName);

        if (count != null && count == 0) {
            String sql = "ALTER TABLE lesson_progress ADD COLUMN " + columnName + " " + definition;
            jdbcTemplate.execute(sql);
            System.out.println(">>> Added missing column lesson_progress." + columnName);
        }
    }

    private void addExamColumnIfMissing(String columnName, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'exams'
                  AND COLUMN_NAME = ?
                """,
                Integer.class,
                columnName);

        if (count != null && count == 0) {
            String sql = "ALTER TABLE exams ADD COLUMN " + columnName + " " + definition;
            jdbcTemplate.execute(sql);
            System.out.println(">>> Added missing column exams." + columnName);
        }
    }

    private void addExamAttemptColumnIfMissing(String columnName, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'exam_attempts'
                  AND COLUMN_NAME = ?
                """,
                Integer.class,
                columnName);

        if (count != null && count == 0) {
            String sql = "ALTER TABLE exam_attempts ADD COLUMN " + columnName + " " + definition;
            jdbcTemplate.execute(sql);
            System.out.println(">>> Added missing column exam_attempts." + columnName);
        }
    }

    private void addUserMemoryColumnIfMissing(String columnName, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'user_memory'
                  AND COLUMN_NAME = ?
                """,
                Integer.class,
                columnName);

        if (count != null && count == 0) {
            String sql = "ALTER TABLE user_memory ADD COLUMN " + columnName + " " + definition;
            jdbcTemplate.execute(sql);
            System.out.println(">>> Added missing column user_memory." + columnName);
        }
    }
}
