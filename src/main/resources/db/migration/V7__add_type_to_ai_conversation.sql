alter table ai_conversation
    add column if not exists type varchar(255) not null default 'general';