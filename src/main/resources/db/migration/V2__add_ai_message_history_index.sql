CREATE INDEX idx_ai_messages_conversation_created_at
    ON ai_messages (conversation_id, created_at);
