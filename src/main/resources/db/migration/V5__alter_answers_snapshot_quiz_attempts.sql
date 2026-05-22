ALTER TABLE quiz_attempts
ALTER COLUMN answers_snapshot TYPE jsonb
USING COALESCE(NULLIF(answers_snapshot, '')::jsonb, '[]'::jsonb);

ALTER TABLE quiz_attempts
ALTER COLUMN answers_snapshot SET DEFAULT '[]'::jsonb;