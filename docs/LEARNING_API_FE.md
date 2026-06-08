# AlgoTutor — Learning Flow API Documentation

Tài liệu mô tả các API endpoints cần thiết để tích hợp backend với quy trình học tập trên frontend.

---

## Tổng quan kiến trúc

```
Frontend Flow:
/roadmaps → /roadmaps/:slug → (Enroll) → /learn/:roadmapSlug/:lessonSlug

API Base URL: {NEXT_PUBLIC_API_BASE_URL}/api/v1
Authentication: Cookie-based (withCredentials: true)
Response Format: ApiResponse<T> wrapper
```

### Response Wrapper

Tất cả API đều trả về format:

```json
{
  "success": true,
  "message": "OK",
  "data": { ... },
  "errors": null
}
```

Khi lỗi validation:
```json
{
  "success": false,
  "message": "Validation failed",
  "data": null,
  "errors": {
    "fieldName": ["Error message 1", "Error message 2"]
  }
}
```

### Paginated Response

Cho các endpoint có phân trang:
```json
{
  "data": [...],
  "pageSize": 10,
  "totalPages": 5,
  "totalElements": 48,
  "currentPage": 0
}
```

---

## 1. Roadmaps (Danh sách khóa học)

### 1.1 GET /roadmaps — Danh sách roadmaps

**Query Params:**
| Param | Type | Default | Description |
|-------|------|---------|-------------|
| page | number | 0 | Trang hiện tại (0-indexed) |
| size | number | 10 | Số items mỗi trang |
| level | string? | - | Filter: `BEGINNER`, `INTERMEDIATE`, `ADVANCED` |

**Response:** `PageResponse<RoadmapListItem>`

```json
{
  "data": [
    {
      "name": "DSA Fundamentals",
      "slug": "dsa-fundamentals",
      "level": "BEGINNER",
      "thumbnailUrl": "https://...",
      "description": "Master the essential data structures...",
      "goal": "Build a solid foundation in DSA...",
      "isPremium": false,
      "enrollmentCount": 2847,
      "topicCount": 4,
      "lessonCount": 14
    }
  ],
  "pageSize": 10,
  "totalPages": 1,
  "totalElements": 3,
  "currentPage": 0
}
```

---

### 1.2 GET /roadmaps/:slug — Chi tiết roadmap

Trả về thông tin đầy đủ roadmap bao gồm topics, lessons, và trạng thái enrollment + progress của user hiện tại.

**Response:** `ApiResponse<RoadmapDetailResponse>`

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "DSA Fundamentals",
    "slug": "dsa-fundamentals",
    "level": "BEGINNER",
    "description": "Master the essential data structures...",
    "goal": "Build a solid foundation...",
    "thumbnailUrl": "https://...",
    "isPublished": true,
    "isPremium": false,
    "enrollmentCount": 2847,
    "topicCount": 4,
    "lessonCount": 14,
    "enrolled": true,
    "createdAt": "2024-01-15T00:00:00Z",
    "updatedAt": "2024-06-01T00:00:00Z",
    "topics": [
      {
        "id": 1,
        "name": "Arrays & Hashing",
        "description": "Learn the fundamentals of arrays...",
        "displayOrder": 1,
        "isLocked": false,
        "lessonCount": 5,
        "createdAt": "2024-01-15T00:00:00Z",
        "updatedAt": "2024-06-01T00:00:00Z",
        "lessons": [
          {
            "id": 1,
            "title": "Introduction to Arrays",
            "slug": "intro-to-arrays",
            "type": "THEORY",
            "displayOrder": 1,
            "difficulty": "EASY",
            "progress": "COMPLETED",
            "createdAt": "2024-01-15T00:00:00Z",
            "updatedAt": "2024-06-01T00:00:00Z"
          }
        ]
      }
    ]
  }
}
```

**Lưu ý quan trọng:**
- `enrolled`: `true` nếu user đã đăng ký, `false` nếu chưa (hoặc chưa login)
- `progress`: `null` nếu chưa bắt đầu, `"NOT_STARTED"` | `"IN_PROGRESS"` | `"COMPLETED"`
- `isLocked`: Topic bị khóa → user không thể truy cập lessons bên trong
- Frontend dùng endpoint này cho cả trang detail VÀ trang learn (sidebar navigator)

---

### 1.3 POST /roadmaps/:slug/enroll — Đăng ký khóa học

**Request Body:** (empty)

**Response:** `ApiResponse<EnrollmentResponse>`

```json
{
  "success": true,
  "data": {
    "id": "uuid-enrollment-id",
    "userId": "uuid-user-id",
    "learningPathId": 1
  }
}
```

**Error cases:**
- 401: Chưa đăng nhập
- 409: Đã enrolled rồi

---

### 1.4 GET /roadmaps/:slug/enrollment — Thông tin enrollment

**Response:** `ApiResponse<EnrollmentDetailResponse>`

```json
{
  "success": true,
  "data": {
    "id": "uuid-enrollment-id",
    "userId": "uuid-user-id",
    "learningPathId": 1,
    "learningPathName": "DSA Fundamentals",
    "status": "ACTIVE",
    "completedAt": null,
    "createdAt": "2024-01-15T00:00:00Z",
    "lessonProgressions": [
      { "lessonId": 1, "status": "COMPLETED", "updatedAt": "2024-02-01T00:00:00Z" },
      { "lessonId": 2, "status": "COMPLETED", "updatedAt": "2024-02-02T00:00:00Z" },
      { "lessonId": 3, "status": "IN_PROGRESS", "updatedAt": "2024-02-03T00:00:00Z" }
    ]
  }
}
```

---

### 1.5 PATCH /roadmaps/:pathSlug/lessons/:lessonSlug/progress — Cập nhật tiến độ

**Request Body:**
```json
{
  "status": "COMPLETED"
}
```

**Possible values:** `"NOT_STARTED"` | `"IN_PROGRESS"` | `"COMPLETED"`

**Response:** `ApiResponse<LessonProgressUpdateResponse>`

```json
{
  "success": true,
  "data": {
    "lessonId": 4,
    "status": "COMPLETED",
    "updatedAt": "2024-02-05T10:30:00Z"
  }
}
```

---

## 2. Lesson Content (Nội dung bài học)

Frontend hiện tại dùng mock data. Cần 3 endpoint mới để fetch nội dung bài học theo type.

---

### 2.1 GET /lessons/:lessonSlug/theory — Nội dung lý thuyết

**Response:** `ApiResponse<TheoryLesson>`

```json
{
  "success": true,
  "data": {
    "id": 1,
    "slug": "intro-to-arrays",
    "title": "Introduction to Arrays",
    "content": "# Introduction to Arrays\n\nArrays are one of the most fundamental...",
    "estimatedMinutes": 8
  }
}
```

**Lưu ý:**
- `content`: Markdown string (hỗ trợ GFM — tables, code blocks, blockquotes)
- Frontend render bằng `react-markdown` + `remark-gfm`
- Nên hỗ trợ: headings, code blocks (với language hint), tables, blockquotes, lists, images
- Ước tính `estimatedMinutes` dựa trên word count (~200 words/min)

---

### 2.2 GET /lessons/:lessonSlug/quiz — Nội dung quiz

**Response:** `ApiResponse<Quiz>`

```json
{
  "success": true,
  "data": {
    "id": 1,
    "slug": "arrays-fundamentals-quiz",
    "title": "Arrays Fundamentals Quiz",
    "passingScore": 70,
    "questions": [
      {
        "id": 1,
        "text": "What is the time complexity of accessing an element at index i in an array?",
        "type": "SINGLE_CHOICE",
        "options": [
          { "id": "a", "text": "O(n)" },
          { "id": "b", "text": "O(1)" },
          { "id": "c", "text": "O(log n)" },
          { "id": "d", "text": "O(n²)" }
        ],
        "explanation": "Array elements are stored at contiguous memory locations...",
        "correctOptionIds": ["b"]
      },
      {
        "id": 2,
        "text": "Which operations are O(n)? (Select all that apply)",
        "type": "MULTIPLE_CHOICE",
        "options": [
          { "id": "a", "text": "Searching (unsorted)" },
          { "id": "b", "text": "Access by index" },
          { "id": "c", "text": "Insert at beginning" },
          { "id": "d", "text": "Access last element" }
        ],
        "explanation": "Searching requires checking all elements...",
        "correctOptionIds": ["a", "c"]
      }
    ]
  }
}
```

**Lưu ý:**
- `type`: `"SINGLE_CHOICE"` (radio) hoặc `"MULTIPLE_CHOICE"` (checkbox)
- `correctOptionIds`: Array of option IDs đúng
- `explanation`: Hiển thị sau khi user submit quiz
- `passingScore`: Phần trăm (0-100) cần đạt để pass
- Frontend tính điểm ở client-side, không cần gửi lên server (chỉ cần mark progress)

---

### 2.3 GET /lessons/:lessonSlug/coding — Nội dung coding problem

**Response:** `ApiResponse<CodingProblem>`

```json
{
  "success": true,
  "data": {
    "id": 1,
    "slug": "two-sum-coding",
    "title": "Two Sum",
    "description": "## Two Sum\n\nGiven an array of integers `nums`...",
    "starterCode": {
      "javascript": "function twoSum(nums, target) {\n    // Your code here\n}",
      "python": "def two_sum(nums: list[int], target: int) -> list[int]:\n    # Your code here\n    pass",
      "java": "class Solution {\n    public int[] twoSum(int[] nums, int target) {\n        return new int[]{};\n    }\n}",
      "cpp": "class Solution {\npublic:\n    vector<int> twoSum(vector<int>& nums, int target) {\n        return {};\n    }\n};"
    },
    "testCases": [
      {
        "input": "2,7,11,15|9",
        "expectedOutput": "0,1",
        "isHidden": false
      },
      {
        "input": "3,2,4|6",
        "expectedOutput": "1,2",
        "isHidden": false
      },
      {
        "input": "1,5,3,7,9,2|8",
        "expectedOutput": "0,3",
        "isHidden": true
      }
    ],
    "hints": [
      "Think about what two numbers add up to the target.",
      "Use a hash map to store numbers you've seen.",
      "For each number, check if target - number is in the map."
    ],
    "timeLimit": 2000,
    "memoryLimit": 256
  }
}
```

**Lưu ý:**
- `description`: Markdown (problem statement giống Codeforces/LeetCode)
- `starterCode`: Map<language, code> — mỗi ngôn ngữ có template riêng
- `testCases.input`: Format tùy bài, dùng `|` để phân tách các tham số
- `testCases.isHidden`: `true` = không hiện input/output cho user, chỉ hiện kết quả pass/fail sau submit
- `hints`: Array string, frontend reveal từng hint một
- `timeLimit`: milliseconds
- `memoryLimit`: MB

---

## 3. Code Execution (Chạy & Submit code)

### 3.1 POST /judge/run — Chạy code (chỉ visible test cases)

Chạy code của user với các test case visible (không chạy hidden).

**Request Body:**
```json
{
  "lessonSlug": "two-sum-coding",
  "language": "javascript",
  "code": "function twoSum(nums, target) {\n  const map = new Map();\n  for (let i = 0; i < nums.length; i++) {\n    const complement = target - nums[i];\n    if (map.has(complement)) return [map.get(complement), i];\n    map.set(nums[i], i);\n  }\n}"
}
```

**Response:** `ApiResponse<RunResult>`

```json
{
  "success": true,
  "data": {
    "results": [
      {
        "stdin": "2,7,11,15|9",
        "expected": "0,1",
        "actual": "0,1",
        "passed": true,
        "hidden": false,
        "executionTime": 12
      },
      {
        "stdin": "3,2,4|6",
        "expected": "1,2",
        "actual": "1,2",
        "passed": true,
        "hidden": false,
        "executionTime": 8
      }
    ],
    "totalTime": 20,
    "compilationError": null
  }
}
```

---

### 3.2 POST /judge/submit — Submit code (tất cả test cases)

Chạy code với TẤT CẢ test cases (bao gồm hidden). Nếu pass hết → tự động mark lesson COMPLETED.

**Request Body:**
```json
{
  "lessonSlug": "two-sum-coding",
  "language": "python",
  "code": "def two_sum(nums, target):\n    seen = {}\n    for i, num in enumerate(nums):\n        complement = target - num\n        if complement in seen:\n            return [seen[complement], i]\n        seen[num] = i"
}
```

**Response:** `ApiResponse<SubmitResult>`

```json
{
  "success": true,
  "data": {
    "id": "submission-uuid",
    "status": "ACCEPTED",
    "results": [
      {
        "stdin": "2,7,11,15|9",
        "expected": "0,1",
        "actual": "0,1",
        "passed": true,
        "hidden": false,
        "executionTime": 5
      },
      {
        "stdin": "1,5,3,7,9,2|8",
        "expected": "0,3",
        "actual": "0,3",
        "passed": true,
        "hidden": true,
        "executionTime": 7
      }
    ],
    "totalTime": 35,
    "memoryUsed": 14,
    "compilationError": null,
    "lessonProgressUpdated": true
  }
}
```

**Status values:**
| Status | Description |
|--------|-------------|
| `ACCEPTED` | Tất cả test cases pass |
| `WRONG_ANSWER` | Có test case output sai |
| `RUNTIME_ERROR` | Code bị lỗi runtime (exception, segfault...) |
| `TIME_LIMIT_EXCEEDED` | Vượt quá timeLimit |
| `COMPILATION_ERROR` | Code không compile được |

**Lưu ý:**
- Khi `status === "ACCEPTED"`, backend tự động update lesson progress → `COMPLETED`
- `lessonProgressUpdated`: `true` nếu đây là lần đầu ACCEPTED
- Với hidden test cases: `actual` field có thể trả `""` (không reveal output)
- `compilationError`: String mô tả lỗi compile (null nếu không có)

---

### 3.3 GET /judge/submissions?lessonSlug=:slug — Lịch sử submissions

**Query Params:**
| Param | Type | Description |
|-------|------|-------------|
| lessonSlug | string | Required |
| page | number | Default: 0 |
| size | number | Default: 20 |

**Response:** `ApiResponse<PageResponse<SubmissionSummary>>`

```json
{
  "success": true,
  "data": {
    "data": [
      {
        "id": "submission-uuid",
        "timestamp": "2024-02-05T10:30:00Z",
        "language": "javascript",
        "status": "ACCEPTED",
        "passedCount": 5,
        "totalCount": 5,
        "executionTime": 35,
        "memoryUsed": 14
      },
      {
        "id": "submission-uuid-2",
        "timestamp": "2024-02-05T10:25:00Z",
        "language": "javascript",
        "status": "WRONG_ANSWER",
        "passedCount": 3,
        "totalCount": 5,
        "executionTime": 28,
        "memoryUsed": 12
      }
    ],
    "pageSize": 20,
    "totalPages": 1,
    "totalElements": 2,
    "currentPage": 0
  }
}
```

---

## 4. Enums & Constants

### Level
```
BEGINNER | INTERMEDIATE | ADVANCED
```

### Difficulty
```
EASY | MEDIUM | HARD
```

### LessonType
```
THEORY | QUIZ | CODING
```

### ProgressStatus
```
NOT_STARTED | IN_PROGRESS | COMPLETED
```

### EnrollmentStatus
```
ACTIVE | COMPLETED | DROPPED
```

### SubmissionStatus
```
ACCEPTED | WRONG_ANSWER | RUNTIME_ERROR | TIME_LIMIT_EXCEEDED | COMPILATION_ERROR
```

### Supported Languages (Code Judge)
```
javascript | python | java | cpp
```

---

## 7. Business Logic quan trọng

### 7.1 Topic Locking
- Topic đầu tiên luôn unlocked
- Topic tiếp theo unlock khi user hoàn thành ≥ 80% lessons trong topic trước
- Hoặc: unlock khi hoàn thành tất cả lessons trong topic trước (tùy config)

### 7.2 Lesson Progress Rules
- **THEORY**: Frontend tự detect khi user đọc ≥ 90% content → gọi PATCH progress
- **QUIZ**: Khi user đạt `passingScore` → frontend gọi PATCH progress
- **CODING**: Khi submit ACCEPTED → backend tự động update progress

### 7.3 Enrollment Completion
- Khi tất cả lessons (không bị locked) đều COMPLETED → enrollment status = COMPLETED

### 7.4 Code Judge Flow
```
1. User clicks "Run" → POST /judge/run (chỉ visible test cases)
2. User clicks "Submit" → POST /judge/submit (tất cả test cases)
3. Backend compile & execute code trong sandbox
4. So sánh output với expected
5. Trả kết quả + tự động update progress nếu ACCEPTED
```

### 7.5 Test Case Input Format
- Dùng `|` để phân tách các tham số khác nhau
- Ví dụ Two Sum: `"2,7,11,15|9"` → nums=[2,7,11,15], target=9
- Backend cần parse input theo format riêng của từng bài
- Mỗi bài cần có "judge template" để wrap user code + parse I/O

---

## 8. Frontend Integration Checklist

Khi backend sẵn sàng, cần thay đổi ở frontend:

### File: `app/learn/[roadmapSlug]/[lessonSlug]/page.tsx`
- [ ] Thay `MOCK_THEORY_LESSONS[lessonSlug]` → `useSWR(/lessons/${lessonSlug}/theory)`
- [ ] Thay `MOCK_QUIZZES[lessonSlug]` → `useSWR(/lessons/${lessonSlug}/quiz)`
- [ ] Thay `MOCK_CODING_PROBLEMS[lessonSlug]` → `useSWR(/lessons/${lessonSlug}/coding)`
- [ ] Bỏ `detectLessonType()` → lấy `type` từ roadmap data (đã có trong `LessonWithProgress`)

### File: `components/learn/coding-content.tsx`
- [ ] Thay `runCode()` mock → `POST /judge/run`
- [ ] Thay `handleSubmit()` mock → `POST /judge/submit`
- [ ] Thêm fetch submission history từ `GET /judge/submissions`

### File: `api/api-client.ts`
- [ ] Bỏ mock fallback khi backend sẵn sàng (set `NEXT_PUBLIC_USE_MOCK=false`)

### File: `lib/mock/` (toàn bộ thư mục)
- [ ] Có thể xóa sau khi tích hợp xong (giữ lại cho testing)

---

## 9. Ví dụ Seed Data

### Roadmap: DSA Fundamentals

| Topic | Lesson | Type | Slug |
|-------|--------|------|------|
| Arrays & Hashing | Introduction to Arrays | THEORY | `intro-to-arrays` |
| Arrays & Hashing | Arrays Fundamentals Quiz | QUIZ | `arrays-fundamentals-quiz` |
| Arrays & Hashing | The Two Sum Problem | THEORY | `two-sum-problem` |
| Arrays & Hashing | Two Sum — Coding | CODING | `two-sum-coding` |
| Arrays & Hashing | Two Sum Quiz | QUIZ | `two-sum-coding-quiz` |
| Two Pointers | Array Traversal Patterns | THEORY | `array-traversal-patterns` |
| Two Pointers | Reverse String | CODING | `reverse-string-coding` |
| Two Pointers | Valid Palindrome | CODING | `valid-palindrome-coding` |
| Two Pointers | Maximum Subarray | CODING | `max-subarray-coding` |
| Binary Search | Binary Search | CODING | `binary-search-coding` |
| Binary Search | Merge Sorted Arrays | CODING | `merge-sorted-arrays-coding` |
| Binary Search | Climbing Stairs | CODING | `climbing-stairs-coding` |
| Dynamic Programming 🔒 | Introduction to DP | THEORY | `intro-to-dp` |
| Dynamic Programming 🔒 | Climbing Stairs (DP) | CODING | `climbing-stairs-dp-coding` |

---

## 10. Error Handling

| HTTP Status | Meaning | Frontend Action |
|-------------|---------|-----------------|
| 200 | Success | Process data |
| 400 | Bad Request (validation) | Show field errors |
| 401 | Unauthorized | Redirect to login |
| 403 | Forbidden (premium/locked) | Show upgrade prompt |
| 404 | Not Found | Show 404 page |
| 409 | Conflict (already enrolled) | Ignore, treat as success |
| 429 | Rate Limited (judge) | Show "try again later" |
| 500 | Server Error | Show generic error toast |

---

## 11. Performance Considerations

- **Roadmap detail** được cache bằng SWR (`revalidateOnFocus: false`)
- **Lesson content** nên có cache header (content ít thay đổi)
- **Code judge** nên có rate limiting (max 10 runs/min, 5 submits/min)
- **AI chat** nên stream response (SSE) để UX tốt hơn
- **Submissions** chỉ fetch khi user mở tab "History"
