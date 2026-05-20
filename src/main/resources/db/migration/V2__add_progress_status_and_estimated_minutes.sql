-- Add status column to lesson_progresses for tri-state progress tracking
ALTER TABLE lesson_progresses
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED';

-- Sync existing data: set status based on is_completed
UPDATE lesson_progresses SET status = 'COMPLETED' WHERE is_completed = true;
UPDATE lesson_progresses SET status = 'NOT_STARTED' WHERE is_completed = false;

-- Add estimated_minutes column to theory lessons
ALTER TABLE theory_lesson
    ADD COLUMN IF NOT EXISTS estimated_minutes INTEGER;

-- Add id field support for quiz choices (stored in JSONB, no schema change needed)
-- QuizChoice.id is part of the JSONB column 'choices' in quiz_questions table
