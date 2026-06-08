-- Add is_published column to lessons table for publish/unpublish functionality
ALTER TABLE lessons
    ADD COLUMN IF NOT EXISTS is_published BOOLEAN NOT NULL DEFAULT false;
