-- Add new columns to ai_chat_logs table for Harness support
ALTER TABLE ai_chat_logs ADD COLUMN source VARCHAR(20) DEFAULT 'tutor';
ALTER TABLE ai_chat_logs ADD COLUMN agent_type VARCHAR(50);

-- Create user_memory table for Harness user memory
CREATE TABLE IF NOT EXISTS user_memory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    user_level VARCHAR(20),
    target_exam VARCHAR(50),
    target_score INT,
    weak_points TEXT,
    strong_points TEXT,
    lesson_context VARCHAR(100),
    adaptive_rules TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
