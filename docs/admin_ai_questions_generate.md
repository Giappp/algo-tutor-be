# Admin AI Question Generation From Theory Lessons

## Mục tiêu

Admin chọn một hoặc nhiều bài học `THEORY` trong cùng learning path với quiz, thêm yêu cầu, sau đó AI tạo bản nháp câu hỏi. API generate **không tự lưu câu hỏi**. Frontend luôn cho admin review rồi mới gọi API tạo question hiện có.

Luồng:

1. Tải danh sách theory lesson có thể dùng làm nguồn.
2. Admin chọn nguồn và cấu hình yêu cầu.
3. Backend dựng context, gọi LLM, parse và validate kết quả.
4. Frontend review/chọn draft.
5. Frontend gọi `POST /api/v1/questions/lessons/{quizLessonId}` cho từng câu đã chọn.

## Endpoint 1: Danh sách nguồn

```http
GET /api/v1/admin/ai/quiz-lessons/{quizLessonId}/question-sources
```

Backend cần:

- Xác thực `ADMIN`.
- Kiểm tra `{quizLessonId}` tồn tại và có type `QUIZ`.
- Từ quiz truy ra topic và learning path.
- Trả các lesson type `THEORY` thuộc cùng learning path, có `content` không rỗng.
- Không trả toàn bộ content để response nhẹ; chỉ trả preview và metadata.

Response:

```json
[
  {
    "lessonId": 101,
    "title": "Binary Search Fundamentals",
    "topicId": 12,
    "topicName": "Searching",
    "displayOrder": 2,
    "estimatedMinutes": 12,
    "contentCharacterCount": 8420,
    "contentPreview": "Binary search repeatedly halves the search interval...",
    "isPublished": true
  }
]
```

## Endpoint 2: Generate draft questions

```http
POST /api/v1/admin/ai/quiz-lessons/{quizLessonId}/generate-questions
Content-Type: application/json
```

Request:

```json
{
  "sourceLessonIds": [101, 104],
  "prompt": "Focus on conceptual understanding and include one scenario-based question.",
  "provider": "GEMINI",
  "difficulty": "MEDIUM",
  "questionTypes": ["SINGLE_CHOICE", "MULTIPLE_CHOICE"],
  "count": 5,
  "choicesPerQuestion": 4,
  "includeExplanations": true
}
```

Validation đề xuất:

| Field | Quy tắc |
|---|---|
| `sourceLessonIds` | 1-10 ID duy nhất; tất cả phải là `THEORY` và cùng learning path với quiz |
| `prompt` | Tùy chọn, trim, tối đa 2.000 ký tự |
| `provider` | `OPENAI`, `GEMINI`, `CLAUDE` hoặc `null` |
| `difficulty` | `EASY`, `MEDIUM`, `HARD` |
| `questionTypes` | 1-2 giá trị: `SINGLE_CHOICE`, `MULTIPLE_CHOICE` |
| `count` | 1-10 |
| `choicesPerQuestion` | 2-5 |
| `includeExplanations` | boolean |

Response:

```json
{
  "quizLessonId": 205,
  "questions": [
    {
      "question": "Why does binary search require sorted input?",
      "type": "SINGLE_CHOICE",
      "points": 2,
      "orderIndex": 1,
      "explanation": "Sorting lets the algorithm safely discard half of the remaining range.",
      "choices": [
        {
          "text": "It makes the midpoint comparison meaningful",
          "isCorrect": true,
          "explanation": "The ordering determines which half can be discarded."
        },
        {
          "text": "It reduces memory allocation",
          "isCorrect": false,
          "explanation": null
        }
      ]
    }
  ],
  "context": {
    "sources": [
      {"lessonId": 101, "title": "Binary Search Fundamentals", "topicName": "Searching"}
    ],
    "truncatedSourceIds": []
  },
  "inputTokens": 4200,
  "outputTokens": 1600
}
```

## Backend cần bổ sung

### Controller và DTO

- `AdminAiQuizQuestionController`
- `AiQuestionSourceResponse`
- `GenerateQuestionsFromSourcesRequest`
- `GenerateQuestionsFromSourcesResponse`
- Bean validation và enum validation cho toàn bộ request.

### Truy vấn và phân quyền

- Query lesson hierarchy hiệu quả để xác minh quiz/source cùng learning path.
- Chỉ role `ADMIN` được gọi.
- Không tin `learningPathId` từ client; luôn suy ra từ `quizLessonId`.
- Có thể cho phép draft theory lesson, nhưng phải audit rõ source nào chưa publish.

### Context builder

- Load Markdown `content` của từng source theo đúng thứ tự curriculum.
- Bỏ script/HTML nguy hiểm và chuẩn hóa whitespace.
- Giới hạn token theo provider; chia budget công bằng giữa các source.
- Trả `truncatedSourceIds` nếu phải cắt nội dung.
- Prompt phải nói rõ: chỉ dùng kiến thức từ source, không bịa ngoài phạm vi.

### Structured output và validation

- Ưu tiên JSON schema/structured output của provider thay vì parse Markdown.
- Validate mỗi question:
  - Nội dung không rỗng và không trùng nhau.
  - Có đúng `choicesPerQuestion` choices.
  - `SINGLE_CHOICE` có đúng một đáp án đúng.
  - `MULTIPLE_CHOICE` có ít nhất hai đáp án đúng.
  - Choices không trùng nhau.
  - Điểm nằm trong giới hạn hệ thống.
- Reject hoặc repair một lần nếu output sai schema; không trả draft lỗi cho frontend.

### Chống trùng lặp và grounding

- Gửi danh sách câu hỏi quiz hiện có vào prompt để tránh tạo câu trùng.
- Có thể tính similarity với câu hiện có và giữa các draft.
- Với mỗi câu, backend nên giữ metadata nội bộ như `sourceLessonIds` hoặc grounding score để audit; không bắt buộc trả cho learner.

### Vận hành

- Timeout khoảng 120 giây, retry có giới hạn và circuit breaker theo provider.
- Rate limit theo admin và quiz lesson.
- Ghi audit log: admin ID, quiz ID, source IDs, provider, prompt hash, token usage, latency và kết quả.
- Ghi usage vào hệ thống quota/token hiện có.
- Không log nguyên nội dung source hoặc prompt nếu log có thể chứa dữ liệu nhạy cảm.

## Persistence tùy chọn

Luồng hiện tại giữ draft ở frontend và dùng API question hiện có để lưu. Nếu cần import nguyên tử, bổ sung:

```http
POST /api/v1/questions/lessons/{quizLessonId}/bulk
```

Endpoint bulk nên validate toàn bộ danh sách và lưu trong một transaction, tránh tình trạng chỉ thêm được một phần câu hỏi.
