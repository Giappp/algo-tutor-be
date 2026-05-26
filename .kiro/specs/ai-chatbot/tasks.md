# Implementation Plan: AI Chatbot

## Overview

Implement the AI Chatbot feature for AlgoTutor — an intelligent algorithm tutoring assistant powered by Spring AI. The implementation builds incrementally: enums/DTOs → database migration → configuration → context building → prompt service → provider routing → tool functions → core chat service → controller with rate limiting → suggestion generation → error handling → tests.

## Tasks

- [x] 1. Set up enums, DTOs, and data model foundations
  - [x] 1.1 Define enums `AiChatMode`, `LLMProvider`, and `AiMessageRole`
    - Update `AiChatMode` enum to include: HINT, EXPLAIN, DEBUG, REVIEW, COMPLEXITY, SOLUTION, NEXT_STEP
    - Update `LLMProvider` enum to include: OPENAI, GEMINI, CLAUDE_SONNET_4_6
    - Verify `AiMessageRole` enum includes: SYSTEM, USER, ASSISTANT, TOOL
    - _Requirements: 1.1, 2.1–2.7, 6.1_

  - [x] 1.2 Define request/response DTOs with validation
    - Update `AiChatRequest` record with Jakarta validation annotations (`@Size`, `@NotNull` on mode)
    - Add custom validation: at least one of `message` or `code` must be non-blank
    - Update `AiChatResponse` record with fields: conversationId, answer, mode, suggestions, sources, canAskNextHint
    - Verify `AiSuggestion` DTO has: label (max 100), mode, message (max 500)
    - _Requirements: 1.1, 8.1, 9.3, 9.4_

  - [x] 1.3 Update entity classes `AIConversation` and `AiMessage`
    - Update `AIConversation` entity to use `LLMProvider` enum instead of `AiProvider`
    - Add `@PrePersist` / `@PreUpdate` lifecycle callbacks for timestamps
    - Verify `AiMessage` entity has all required fields per design
    - _Requirements: 5.1, 5.2, 5.6_

  - [x] 1.4 Create Flyway migration for AI tables
    - Create `V4__create_ai_conversation_and_messages_tables.sql`
    - Define `ai_conversation` table with: id (UUID PK), user_id, lesson_id, title, provider, created_at, updated_at
    - Define `ai_messages` table with: id (UUID PK), conversation_id (FK), user_id, role, content (TEXT), mode, token_input, token_output, created_at
    - Add indexes on conversation_id, user_id, and created_at
    - _Requirements: 5.1, 5.2_

- [x] 2. Implement repository layer and AI configuration
  - [x] 2.1 Create `ConversationRepository` and update `AiMessageRepository`
    - Create `ConversationRepository` extending `JpaRepository<AIConversation, UUID>`
    - Add method `findByIdAndUserId(UUID id, UUID userId)` for ownership check
    - Add query method to `AiMessageRepository`: `findTop10ByConversationIdOrderByCreatedAtDesc`
    - _Requirements: 5.3, 9.2_

  - [x] 2.2 Configure multi-provider `ChatClient` beans in `AiConfig`
    - Create separate `ChatClient` beans for OpenAI, Gemini, and Claude using `ChatClient.Builder`
    - Configure default provider from application properties
    - Add timeout configuration (30 seconds) for each client
    - _Requirements: 6.1, 6.2, 6.4_

  - [x] 2.3 Add AI-specific error codes to `ErrorCode` enum
    - Add: `INVALID_CHAT_MODE`, `CODE_REQUIRED`, `CONVERSATION_NOT_FOUND`, `UNSUPPORTED_PROVIDER`, `RATE_LIMIT_EXCEEDED`, `AI_SERVICE_UNAVAILABLE`
    - Map each to appropriate HTTP status codes per design error handling table
    - _Requirements: 10.1, 10.2, 10.3, 10.4_

- [x] 3. Checkpoint - Ensure project compiles
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Implement context building and prompt service
  - [x] 4.1 Complete `AiContextService` implementation
    - Add handling for `failedTestCases` list (labeled `[FAILED_TEST_CASES]` section)
    - Add handling for `code` + `language` fields (labeled `[USER_CODE]` section with language annotation)
    - Handle case where `code` is present but `language` is null (include without annotation)
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8_

  - [ ]* 4.2 Write property test for context completeness
    - **Property 3: Context completeness**
    - **Validates: Requirements 3.4, 3.5, 3.6, 3.7**

  - [ ]* 4.3 Write property tests for lesson context
    - **Property 4: Coding lesson context includes required fields**
    - **Property 5: Theory lesson context includes required fields**
    - **Validates: Requirements 3.1, 3.2**

  - [x] 4.4 Implement `AiPromptService` with mode-specific prompts
    - Define system prompt templates for each `AiChatMode` (HINT: single hint ≤2 sentences, EXPLAIN: theory + example, DEBUG: identify errors without full rewrite, REVIEW: correctness/style/optimization, COMPLEXITY: Big-O analysis, SOLUTION: full solution + explanation, NEXT_STEP: single next action)
    - Implement `buildSystemPrompt(AiChatMode mode)` returning base + mode-specific instructions
    - Implement `buildUserPrompt(request, context, history)` assembling structured prompt
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.9_

  - [ ]* 4.5 Write property test for mode-specific prompt inclusion
    - **Property 1: Mode-specific prompt inclusion**
    - **Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.9**

- [x] 5. Implement provider routing and tool functions
  - [x] 5.1 Implement `ProviderRouter` component
    - Create `ProviderRouter` class that maps `LLMProvider` enum to corresponding `ChatClient` bean
    - Handle null provider → use default from config
    - Throw `AppException` with `UNSUPPORTED_PROVIDER` for invalid provider values
    - _Requirements: 6.1, 6.2, 6.3_

  - [x] 5.2 Implement `AlgoTutorAiTools` with `@Tool` annotations
    - Implement `getCodingLesson(Long codingLessonId)` returning `ProblemToolResult`
    - Implement `getLatestSubmission(UUID userId, Long codingLessonId)` returning `SubmissionToolResult`
    - Return structured "not found" responses instead of throwing exceptions
    - Ensure user ID context is passed for submission scoping
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

  - [ ]* 5.3 Write property test for tool function lesson retrieval
    - **Property 12: Tool function lesson retrieval**
    - **Validates: Requirements 4.1**

- [x] 6. Implement core chat service logic
  - [x] 6.1 Implement request validation in `AiChatService`
    - Validate mode is a valid `AiChatMode` value
    - Validate code-required modes (DEBUG, REVIEW, COMPLEXITY) have non-blank code
    - Validate at least one of message/code is non-blank
    - Validate message length ≤ 5000 and code length ≤ 10000
    - _Requirements: 2.8, 9.3, 9.4, 10.2_

  - [ ]* 6.2 Write property tests for input validation
    - **Property 2: Code-required modes reject empty code**
    - **Property 9: Input validation — empty content rejection**
    - **Property 10: Input validation — length limits**
    - **Validates: Requirements 2.8, 9.3, 9.4**

  - [x] 6.3 Implement conversation management in `AiChatService`
    - Create new conversation when `conversationId` is null (set userId, lessonId, provider, timestamps)
    - Find existing conversation by ID and validate ownership (userId match)
    - Set conversation title from first user message or lesson title
    - Update `updatedAt` timestamp on new message
    - _Requirements: 1.2, 1.3, 1.5, 5.1, 5.5, 5.6, 9.2_

  - [ ]* 6.4 Write property test for conversation ownership enforcement
    - **Property 11: Conversation ownership enforcement**
    - **Validates: Requirements 9.2**

  - [x] 6.5 Implement conversation history retrieval and LLM call
    - Retrieve up to 10 most recent messages ordered oldest-to-newest
    - Implement truncation: if history exceeds context window, keep at least 4 most recent messages
    - Assemble full prompt (system + context + history + user message)
    - Call LLM via `ProviderRouter` with tools registered, handle 30s timeout
    - _Requirements: 5.3, 5.4, 6.4, 10.1_

  - [ ]* 6.6 Write property test for conversation history limit
    - **Property 7: Conversation history limit**
    - **Validates: Requirements 5.3**

  - [x] 6.7 Implement message persistence and response assembly
    - Save user message (role=USER, content, mode, timestamp)
    - Save assistant message (role=ASSISTANT, content, mode, tokenInput, tokenOutput, timestamp)
    - Assemble `AiChatResponse` with conversationId, answer, mode, suggestions, sources
    - _Requirements: 1.1, 1.4, 5.2_

  - [ ]* 6.8 Write property test for message persistence round-trip
    - **Property 6: Message persistence round-trip**
    - **Validates: Requirements 1.4, 5.2**

- [x] 7. Checkpoint - Ensure core service logic compiles and unit tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. Implement controller, rate limiting, and suggestion generation
  - [x] 8.1 Update `AiChatController` with authentication and rate limiting
    - Add `@AuthenticationPrincipal UserPrincipal` parameter to extract authenticated user
    - Integrate `RateLimiter.isAllowed("ai-chat:" + userId, 20, 60000)` before service call
    - Return 429 with retry-after seconds when rate limited
    - Add `@Valid` annotation on request body
    - _Requirements: 7.1, 7.2, 9.1_

  - [ ]* 8.2 Write property test for rate limiter sliding window correctness
    - **Property 8: Rate limiter sliding window correctness**
    - **Validates: Requirements 7.1, 7.2, 7.3, 7.4**

  - [x] 8.3 Implement `SuggestionGenerator` component
    - Generate 2–4 contextual `AiSuggestion` objects based on current mode and lesson type
    - Each suggestion has: label (≤100 chars), mode (valid AiChatMode), message (≤500 chars)
    - Implement `canAskNextHint` logic: count ASSISTANT HINT messages vs available hints
    - Handle suggestion generation failure gracefully (return empty list)
    - _Requirements: 8.1, 8.2, 8.3, 8.4_

  - [ ]* 8.4 Write property test for suggestion structure validity
    - **Property 13: Suggestion structure validity**
    - **Validates: Requirements 8.1**

  - [ ]* 8.5 Write property test for canAskNextHint flag correctness
    - **Property 14: canAskNextHint flag correctness**
    - **Validates: Requirements 8.3**

- [x] 9. Implement error handling and edge cases
  - [x] 9.1 Implement global error handling for AI module
    - Catch LLM timeout exceptions → return 503 `AI_SERVICE_UNAVAILABLE`
    - Catch LLM provider errors (rate limit, invalid key) → return 503 with generic message
    - Catch unexpected exceptions → log full stack trace, return 500 generic message
    - Ensure no internal details (API keys, connection strings, class names) are exposed
    - _Requirements: 10.1, 10.3, 10.4_

  - [x] 9.2 Add application properties for AI configuration
    - Add properties: `ai.default-provider`, `ai.rate-limit.max-requests`, `ai.rate-limit.window-seconds`
    - Add Spring AI provider configurations (API keys, model names, timeouts)
    - _Requirements: 6.2, 7.1_

- [x] 10. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- The project already has skeleton files for the AI module (controller, services, entities, DTOs, enums, tools) — tasks involve completing/updating these existing files
- The existing `RateLimiter` component in `common/ratelimit` is reused — no new rate limiting infrastructure needed
- jqwik dependency needs to be added to `pom.xml` for property-based testing
- Spring AI 1.1.0 is already configured in the project BOM

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3", "1.4"] },
    { "id": 1, "tasks": ["2.1", "2.2", "2.3"] },
    { "id": 2, "tasks": ["4.1", "4.4", "5.1", "5.2"] },
    { "id": 3, "tasks": ["4.2", "4.3", "4.5", "5.3", "6.1"] },
    { "id": 4, "tasks": ["6.2", "6.3"] },
    { "id": 5, "tasks": ["6.4", "6.5"] },
    { "id": 6, "tasks": ["6.6", "6.7"] },
    { "id": 7, "tasks": ["6.8", "8.1", "8.3"] },
    { "id": 8, "tasks": ["8.2", "8.4", "8.5", "9.1", "9.2"] }
  ]
}
```
