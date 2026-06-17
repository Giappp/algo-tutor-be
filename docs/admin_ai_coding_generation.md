# Admin AI Coding Lesson Generation

Tài liệu định nghĩa API cho AI coding studio. Phạm vi hiện tại gồm:

1. Tạo nội dung đề bài: statement, constraints, examples, hints.
2. Tạo editorial/reference solution.
3. Tạo starter code đa ngôn ngữ.

Không tạo hoặc thay đổi test case.

## Tích hợp với luồng tạo coding lesson

Khi admin chọn **Tạo và mở AI studio**:

1. Frontend validate và tạo coding lesson bằng API create lesson hiện có.
2. Sau khi nhận `lessonId`, frontend điều hướng sang trang lesson với intent `openAi=coding-studio` hoặc lưu session intent tương đương trong inline builder.
3. Coding AI Studio tự mở cho lesson vừa tạo.

Coding lesson cần tồn tại trước khi mở studio để backend có thể xác định topic, learning path, nội dung hiện tại và quyền truy cập. Generic endpoint `generate-content` không được dùng cho coding lesson trong luồng này.

Nếu admin chưa nhập title hoặc statement, frontend tạo lesson draft bằng giá trị tạm hợp lệ trước khi mở studio. Luồng tạo coding lesson thông thường vẫn phải validate đầy đủ title và statement; chỉ action **Tạo và mở AI studio** được phép dùng draft tối thiểu này.

## Nguyên tắc chung

- Base path: `/api/v1/admin/ai/coding-lessons`
- Chỉ role `ADMIN`.
- Backend luôn xác minh lesson có type `CODING`.
- Backend tự lấy coding lesson hiện tại, topic và learning path làm context.
- Các API chỉ trả draft, không tự ghi đè lesson/editorial.
- `sourceLessonIds` chỉ được chứa theory lesson trong cùng learning path.

## Danh sách nguồn

```http
GET /api/v1/admin/ai/coding-lessons/{lessonId}/sources
```

Response giống `AiQuestionSource[]`:

```json
[{
  "lessonId": 101,
  "title": "Hash Map Fundamentals",
  "topicId": 12,
  "topicName": "Hashing",
  "displayOrder": 1,
  "estimatedMinutes": 10,
  "contentCharacterCount": 5400,
  "contentPreview": "A hash map stores...",
  "isPublished": true
}]
```

## Tạo nội dung đề bài

```http
POST /api/v1/admin/ai/coding-lessons/{lessonId}/generate-problem
```

```json
{
  "sourceLessonIds": [101],
  "provider": "GEMINI",
  "prompt": "Create a beginner-friendly array problem.",
  "difficulty": "EASY",
  "exampleCount": 2,
  "hintCount": 3
}
```

`exampleCount`: 1-4. `hintCount`: 0-3. `prompt`: tối đa 3000 ký tự.

Response `content`:

```json
{
  "statement": "# Pair Sum\n\nGiven...",
  "constraints": ["2 <= nums.length <= 10^4"],
  "examples": [{"input": "nums = [2,7], target = 9", "output": "[0,1]", "explanation": "...", "imageUrl": null}],
  "hints": ["Track values already visited."]
}
```

Backend phải giữ nguyên title, limits, starter code, test cases và editorials.

## Tạo editorial

```http
POST /api/v1/admin/ai/coding-lessons/{lessonId}/generate-editorial
```

```json
{
  "sourceLessonIds": [101],
  "provider": "OPENAI",
  "prompt": "Prefer a readable O(n) solution.",
  "language": "JAVA"
}
```

Response `content`:

```json
{
  "language": "JAVA",
  "sourceCode": "class Solution { ... }",
  "approachSummary": "Use a hash map to store complements.",
  "timeComplexity": "O(n)",
  "spaceComplexity": "O(n)"
}
```

Backend cần:

- Validate language thuộc `JAVA`, `PYTHON`, `CPP`.
- Compile source code trong sandbox tương ứng.
- Không tuyên bố code đã đúng với judge nếu chưa chạy test case.
- Trả lỗi validation rõ ràng nếu compile thất bại.
- Không tự lưu editorial. Frontend review rồi gọi API editorial hiện có.

## Tạo starter code

```http
POST /api/v1/admin/ai/coding-lessons/{lessonId}/generate-starter-code
```

```json
{
  "sourceLessonIds": [],
  "provider": null,
  "prompt": "Expose a solve method.",
  "languages": ["JAVA", "PYTHON", "CPP"]
}
```

Response `content`:

```json
{
  "starterCode": {
    "java": "class Solution { ... }",
    "python": "class Solution: ...",
    "cpp": "class Solution { ... };"
  },
  "signatureSummary": "All languages expose solve(nums, target)."
}
```

Backend cần:

- Nhận 1-3 language duy nhất.
- Dùng key response lowercase: `java`, `python`, `cpp`.
- Chỉ tạo scaffold/chữ ký hàm, không chứa lời giải hoàn chỉnh.
- Compile/parse starter code để bắt lỗi cú pháp.
- Xác minh chữ ký hàm và kiểu dữ liệu nhất quán giữa các ngôn ngữ.

## Response wrapper chung

```json
{
  "lessonId": 205,
  "content": {},
  "context": {
    "sources": [{"lessonId": 101, "title": "Hash Map Fundamentals", "topicName": "Hashing"}],
    "truncatedSourceIds": []
  },
  "inputTokens": 3200,
  "outputTokens": 900
}
```

## Backend components đề xuất

- `AdminAiCodingController`
- `CodingAiContextBuilder`
- `CodingProblemDraftValidator`
- `EditorialCompileValidator`
- `StarterCodeSignatureValidator`
- Structured-output JSON schema riêng cho từng endpoint.
- Rate limit, timeout 120 giây, provider retry có giới hạn.
- Audit log: admin, lesson ID, source IDs, asset type, provider, prompt hash, token usage, latency và validation result.
- Ghi token/quota vào hệ thống AI usage hiện có.

## Persistence

Problem content và starter code được frontend apply vào form coding rồi dùng API update lesson hiện có.

Editorial được frontend review rồi gọi:

```http
POST /api/v1/editorials/lessons/{lessonId}
```

Test cases không nằm trong bất kỳ request/response nào của các endpoint trên.
