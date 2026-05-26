# Requirements Document

## Introduction

Tính năng AI Chatbot cho nền tảng AlgoTutor, hỗ trợ người dùng giải đáp câu hỏi về thuật toán và lập trình. Chatbot cung cấp nhiều chế độ tương tác: giải thích lý thuyết, phân tích code, gợi ý hướng giải, debug lỗi, và đánh giá độ phức tạp. Hệ thống sử dụng Spring AI với khả năng chuyển đổi giữa nhiều LLM provider (OpenAI, Gemini) và lưu trữ lịch sử hội thoại.

## Glossary

- **AI_Chatbot_Service**: Dịch vụ backend xử lý yêu cầu chat AI, điều phối giữa context, prompt, và LLM provider
- **Conversation**: Phiên hội thoại giữa người dùng và AI, gắn với một bài học cụ thể
- **Message**: Một tin nhắn trong phiên hội thoại, có vai trò (USER, ASSISTANT, SYSTEM, TOOL)
- **Chat_Mode**: Chế độ tương tác xác định hành vi phản hồi của AI (HINT, EXPLAIN, DEBUG, REVIEW, COMPLEXITY, SOLUTION, NEXT_STEP)
- **Context_Builder**: Thành phần xây dựng ngữ cảnh từ bài học, kết quả judge, và lỗi để cung cấp cho LLM
- **Prompt_Service**: Thành phần tạo system prompt và user prompt phù hợp với chế độ chat
- **LLM_Provider**: Nhà cung cấp mô hình ngôn ngữ lớn (OpenAI, Gemini)
- **Rate_Limiter**: Cơ chế giới hạn số lượng yêu cầu chat của người dùng trong một khoảng thời gian
- **Tool_Function**: Hàm công cụ mà AI có thể gọi để truy vấn thông tin bài học hoặc submission của người dùng
- **Suggestion**: Gợi ý câu hỏi tiếp theo mà hệ thống đề xuất cho người dùng sau mỗi phản hồi

## Requirements

### Requirement 1: Gửi tin nhắn chat

**User Story:** As a learner, I want to send a message to the AI chatbot with a specific mode, so that I receive contextual assistance for my algorithm learning.

#### Acceptance Criteria

1. WHEN a user sends a chat request with a message between 1 and 5000 characters and a mode matching one of the defined AiChatMode values (HINT, EXPLAIN, DEBUG, REVIEW, COMPLEXITY, SOLUTION, NEXT_STEP), THE AI_Chatbot_Service SHALL forward the request to the configured LLM_Provider and return a response within 30 seconds containing the AI answer, conversation ID, mode, suggestions, and sources
2. WHEN a user sends a chat request without a conversation ID, THE AI_Chatbot_Service SHALL create a new Conversation entity with the user's ID, lesson ID, provider, and current timestamp, and associate subsequent messages with that conversation
3. WHEN a user sends a chat request with an existing conversation ID, THE AI_Chatbot_Service SHALL append the message to the existing Conversation and include conversation history in the LLM context
4. THE AI_Chatbot_Service SHALL persist the user message as a Message entity with role USER, and the AI response as a Message entity with role ASSISTANT, each including content, mode, token input count, token output count, and timestamp
5. IF a user sends a chat request with a conversation ID that does not exist in the system, THEN THE AI_Chatbot_Service SHALL return an error response indicating the conversation was not found

### Requirement 2: Chế độ chat theo ngữ cảnh học tập

**User Story:** As a learner, I want to choose different chat modes, so that I receive the appropriate level of assistance based on my learning needs.

#### Acceptance Criteria

1. WHILE the Chat_Mode is HINT, THE AI_Chatbot_Service SHALL instruct the LLM to provide only a single hint of no more than 2 sentences without revealing the full solution or complete code
2. WHILE the Chat_Mode is EXPLAIN, THE AI_Chatbot_Service SHALL instruct the LLM to explain the relevant algorithm theory including definition, working principle, and illustrative example
3. WHILE the Chat_Mode is DEBUG, THE AI_Chatbot_Service SHALL instruct the LLM to identify errors and edge cases in the user code and provide fix guidance limited to pointing out the root cause without rewriting the full solution
4. WHILE the Chat_Mode is REVIEW, THE AI_Chatbot_Service SHALL instruct the LLM to review the user code for correctness, style, and optimization opportunities
5. WHILE the Chat_Mode is COMPLEXITY, THE AI_Chatbot_Service SHALL instruct the LLM to analyze and explain the time and space complexity of the user code using Big-O notation
6. WHILE the Chat_Mode is SOLUTION, THE AI_Chatbot_Service SHALL instruct the LLM to provide a complete algorithm solution with step-by-step explanation of the approach
7. WHILE the Chat_Mode is NEXT_STEP, THE AI_Chatbot_Service SHALL instruct the LLM to suggest the single next actionable step the learner should take to progress toward a solution without revealing subsequent steps
8. IF the Chat_Mode is DEBUG, REVIEW, or COMPLEXITY and the chat request does not contain user code, THEN THE AI_Chatbot_Service SHALL return an error response indicating that user code is required for the selected mode
9. WHEN a user sends a chat request with a valid Chat_Mode, THE AI_Chatbot_Service SHALL include the mode-specific system prompt instruction in the LLM request regardless of whether the conversation already contains messages from a different mode

### Requirement 3: Xây dựng ngữ cảnh từ bài học

**User Story:** As a learner, I want the AI to understand the problem I'm working on, so that it provides relevant and accurate assistance.

#### Acceptance Criteria

1. WHEN a chat request includes a lesson slug matching a CodingLesson, THE Context_Builder SHALL retrieve and include the lesson title, difficulty, statement, and constraints in a labeled context section of the LLM prompt
2. WHEN a chat request includes a lesson slug matching a TheoryLesson, THE Context_Builder SHALL retrieve and include the lesson title, difficulty, and content in a labeled context section of the LLM prompt
3. IF a chat request includes a lesson slug that does not match any existing lesson, THEN THE Context_Builder SHALL omit the lesson context section and proceed with the remaining available context
4. WHEN a chat request includes a non-null judge result string, THE Context_Builder SHALL include the judge execution result in a labeled context section of the LLM prompt
5. WHEN a chat request includes a non-null list of failed test cases, THE Context_Builder SHALL include the failed test case details in the LLM context alongside the judge result
6. WHEN a chat request includes a non-null error message, THE Context_Builder SHALL include the error message text in a labeled context section of the LLM prompt
7. WHEN a chat request includes both a non-null code field and a non-null programming language field, THE Context_Builder SHALL include the code snippet annotated with the specified programming language in a labeled context section of the LLM prompt
8. IF a chat request includes a code field but the programming language field is null, THEN THE Context_Builder SHALL include the code snippet without language annotation in the LLM prompt

### Requirement 4: Tích hợp Tool Function cho AI

**User Story:** As a learner, I want the AI to access my problem details and submission history, so that it provides personalized feedback based on my actual progress.

#### Acceptance Criteria

1. WHEN the LLM invokes the get-coding-lesson tool function during a conversation, THE Tool_Function SHALL retrieve the coding lesson details (ID, title, difficulty, statement, constraints) by the provided coding lesson ID and return the result to the LLM within 3 seconds
2. WHEN the LLM invokes the get-latest-submission tool function during a conversation, THE Tool_Function SHALL retrieve the most recent submission (ID, verdict, language, source code) for the specified user ID and coding lesson ID, ordered by creation timestamp descending, and return the result to the LLM within 3 seconds
3. IF the Tool_Function cannot find a coding lesson for the provided lesson ID, THEN THE Tool_Function SHALL return a structured response indicating that the requested lesson does not exist, without throwing an unhandled exception
4. IF the Tool_Function cannot find any submission for the specified user ID and coding lesson ID, THEN THE Tool_Function SHALL return a structured response indicating that no submission exists for the given user and lesson combination
5. WHEN the Tool_Function is registered with the LLM, THE AI_Chatbot_Service SHALL provide the current user's ID as context so that submission lookups are scoped to the authenticated user

### Requirement 5: Quản lý lịch sử hội thoại

**User Story:** As a learner, I want the AI to remember our conversation context, so that I can have a continuous and coherent learning dialogue.

#### Acceptance Criteria

1. THE AI_Chatbot_Service SHALL store each Conversation with user ID, lesson ID, title (maximum 255 characters), provider, created-at timestamp, and updated-at timestamp
2. THE AI_Chatbot_Service SHALL store each Message with conversation ID, user ID, role (one of SYSTEM, USER, ASSISTANT, TOOL), content (text, no fixed upper limit), mode, token input count, token output count, and created-at timestamp
3. WHEN building the LLM prompt for an existing conversation, THE AI_Chatbot_Service SHALL include up to 10 most recent messages from that conversation, ordered from oldest to newest, as conversation history in the prompt
4. IF the conversation history exceeds the LLM context window limit, THEN THE AI_Chatbot_Service SHALL truncate messages starting from the oldest while preserving at least the 4 most recent messages (2 user-assistant exchanges)
5. WHEN a new Conversation is created, THE AI_Chatbot_Service SHALL set the conversation title based on the first user message content or the associated lesson title
6. WHEN a new Message is persisted to a conversation, THE AI_Chatbot_Service SHALL update the conversation's updated-at timestamp to reflect the time of the latest message

### Requirement 6: Hỗ trợ nhiều LLM Provider

**User Story:** As a platform operator, I want to support multiple LLM providers, so that I can switch between providers for cost optimization and availability.

#### Acceptance Criteria

1. WHEN a chat request specifies a provider value matching a supported LLM_Provider (OPENAI, GEMINI, or CLAUDE_SONNET_4_6), THE AI_Chatbot_Service SHALL route the request to the corresponding LLM_Provider and return the response from that provider
2. WHEN a chat request does not specify a provider value or the provider field is null, THE AI_Chatbot_Service SHALL route the request to the default LLM_Provider defined in the application configuration
3. IF the specified provider value does not match any supported LLM_Provider, THEN THE AI_Chatbot_Service SHALL reject the request and return an error response indicating the unsupported provider value and listing the accepted provider values
4. IF the specified LLM_Provider is unavailable or returns an error within 30 seconds, THEN THE AI_Chatbot_Service SHALL return an error response to the client indicating which provider failed and the nature of the failure (timeout or provider error), without exposing internal connection details

### Requirement 7: Giới hạn tần suất sử dụng

**User Story:** As a platform operator, I want to limit the number of AI chat requests per user, so that I can control API costs and prevent abuse.

#### Acceptance Criteria

1. WHEN a user sends a chat request, THE Rate_Limiter SHALL check whether the user has exceeded 20 requests within a sliding window of 60 seconds, identified by the authenticated user ID
2. IF the user has exceeded the rate limit, THEN THE AI_Chatbot_Service SHALL reject the request and return an error response indicating the rate limit has been reached and the number of seconds remaining until the next request is allowed
3. THE Rate_Limiter SHALL use a sliding window algorithm that records the timestamp of each request per user and removes timestamps older than the configured window before evaluating the limit
4. IF the user has not exceeded the rate limit, THEN THE Rate_Limiter SHALL record the current request timestamp and allow the request to proceed to the AI_Chatbot_Service

### Requirement 8: Gợi ý câu hỏi tiếp theo

**User Story:** As a learner, I want to receive suggested follow-up questions after each AI response, so that I can continue exploring the topic effectively.

#### Acceptance Criteria

1. WHEN the AI_Chatbot_Service returns a response, THE AI_Chatbot_Service SHALL include a list of 2 to 4 Suggestion objects, each containing a label (maximum 100 characters), a mode (a valid AiChatMode value), and a pre-filled message (maximum 500 characters) for follow-up questions
2. THE AI_Chatbot_Service SHALL generate suggestions where each suggestion's mode field contains one of the valid AiChatMode values (HINT, EXPLAIN, DEBUG, REVIEW, COMPLEXITY, SOLUTION, NEXT_STEP) that is contextually applicable to the current conversation topic and lesson type
3. WHEN the Chat_Mode is HINT, THE AI_Chatbot_Service SHALL set the canAskNextHint flag to true if the number of HINT-mode messages from the ASSISTANT in the current conversation is fewer than the total number of available hints for the associated coding lesson, and false otherwise
4. IF the AI_Chatbot_Service fails to generate suggestions, THEN THE AI_Chatbot_Service SHALL return the AI response with an empty suggestions list rather than failing the entire request

### Requirement 9: Xác thực và bảo mật

**User Story:** As a platform operator, I want to ensure only authenticated users can access the AI chatbot, so that the service is protected from unauthorized access.

#### Acceptance Criteria

1. WHEN an unauthenticated user sends a chat request, THE AI_Chatbot_Service SHALL reject the request with an error response indicating that authentication is required
2. IF a chat request specifies a conversation ID that does not belong to the requesting user, THEN THE AI_Chatbot_Service SHALL reject the request with an error response indicating access is denied and SHALL NOT append the message to the conversation
3. IF a chat request contains a message field that is null, empty, or consists only of whitespace, AND the code field is also null, empty, or consists only of whitespace, THEN THE AI_Chatbot_Service SHALL reject the request with a validation error response indicating that at least one of message or code must be provided
4. THE AI_Chatbot_Service SHALL enforce a maximum length of 5000 characters for the message field and 10000 characters for the code field, and SHALL reject requests exceeding these limits with a validation error response indicating the maximum allowed length

### Requirement 10: Xử lý lỗi

**User Story:** As a learner, I want to receive clear error messages when something goes wrong, so that I understand what happened and can retry.

#### Acceptance Criteria

1. IF the LLM_Provider does not respond within 30 seconds or returns a connection error, THEN THE AI_Chatbot_Service SHALL return an error response with a message indicating temporary unavailability and that the user may retry the request
2. IF the chat request contains a mode value that is not one of the defined AiChatMode values (HINT, EXPLAIN, DEBUG, REVIEW, COMPLEXITY, SOLUTION, NEXT_STEP), THEN THE AI_Chatbot_Service SHALL return a validation error listing the accepted mode values
3. IF an unexpected error occurs during processing, THEN THE AI_Chatbot_Service SHALL log the error details including exception type and stack trace, and return a generic error response that does not expose stack traces, class names, database details, or provider configuration
4. IF the LLM_Provider returns a non-timeout error (such as rate limiting from the provider or invalid API key), THEN THE AI_Chatbot_Service SHALL return an error response with a message indicating the service is temporarily unable to process the request and that the user should retry later
