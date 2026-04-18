-- Performance indexes for ThinkAI hot queries.
-- Safe to re-run (idempotent) thanks to information_schema checks.

SET @schema_name = DATABASE();

DROP PROCEDURE IF EXISTS create_index_if_absent;
DELIMITER $$
CREATE PROCEDURE create_index_if_absent(
    IN p_table_name VARCHAR(128),
    IN p_index_name VARCHAR(128),
    IN p_index_columns VARCHAR(512)
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = @schema_name
          AND table_name = p_table_name
    ) THEN
        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = @schema_name
              AND table_name = p_table_name
              AND index_name = p_index_name
        ) THEN
            SET @sql = CONCAT(
                'CREATE INDEX ',
                p_index_name,
                ' ON ',
                p_table_name,
                ' (',
                p_index_columns,
                ')'
            );
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
    END IF;
END$$
DELIMITER ;

-- 1) ai_chat_logs
CALL create_index_if_absent('ai_chat_logs', 'idx_ai_chat_user_source_created', 'user_id, source, created_at');
CALL create_index_if_absent('ai_chat_logs', 'idx_ai_chat_user_conv_created', 'user_id, conversation_id, created_at');
CALL create_index_if_absent('ai_chat_logs', 'idx_ai_chat_conv_created', 'conversation_id, created_at');

-- 2) courses
CALL create_index_if_absent('courses', 'idx_courses_pub_status_created', 'is_published, status, created_at');
CALL create_index_if_absent('courses', 'idx_courses_instructor_created', 'instructor_id, created_at');

-- 3) enrollments
CALL create_index_if_absent('enrollments', 'idx_enrollments_user_enrolled', 'user_id, enrolled_at');
CALL create_index_if_absent('enrollments', 'idx_enrollments_course_completed', 'course_id, completed_at');
CALL create_index_if_absent('enrollments', 'idx_enrollments_course_user', 'course_id, user_id');

-- 4) users + reviews + pending actions
CALL create_index_if_absent('users', 'idx_users_role_active', 'role, is_active');
CALL create_index_if_absent('course_reviews', 'idx_reviews_course_approved_created', 'course_id, is_approved, created_at');
CALL create_index_if_absent('ai_pending_actions', 'idx_pending_user_conv_status_created', 'user_id, conversation_id, status, created_at');

-- notifications already has indexes from migration V5.

DROP PROCEDURE IF EXISTS create_index_if_absent;

ANALYZE TABLE
  ai_chat_logs,
  courses,
  enrollments,
  users,
  course_reviews,
  ai_pending_actions;
