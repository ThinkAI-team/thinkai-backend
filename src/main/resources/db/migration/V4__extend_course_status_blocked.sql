-- Ensure course status can store new value BLOCKED
-- In some existing databases, status was created as ENUM without BLOCKED.
-- Convert to VARCHAR to avoid future enum migration issues.
ALTER TABLE courses
    MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'DRAFT';

-- Backfill invalid/empty values defensively
UPDATE courses
SET status = 'DRAFT'
WHERE status IS NULL OR TRIM(status) = '';
