-- Create ai_conversation table for storing AI chat conversations
CREATE TABLE IF NOT EXISTS ai_conversation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    lesson_id BIGINT,
    title VARCHAR(255),
    provider VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_ai_conversation_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_conversation_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE SET NULL
);

-- Create ai_messages table for storing individual messages within conversations
CREATE TABLE IF NOT EXISTS ai_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    mode VARCHAR(30),
    token_input INTEGER,
    token_output INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_ai_messages_conversation FOREIGN KEY (conversation_id) REFERENCES ai_conversation(id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_messages_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Add indexes for query performance
CREATE INDEX idx_ai_conversation_user_id ON ai_conversation(user_id);
CREATE INDEX idx_ai_conversation_created_at ON ai_conversation(created_at);
CREATE INDEX idx_ai_messages_conversation_id ON ai_messages(conversation_id);
CREATE INDEX idx_ai_messages_user_id ON ai_messages(user_id);
CREATE INDEX idx_ai_messages_created_at ON ai_messages(created_at);
