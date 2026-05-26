# Admin API - Quy trình tạo Learning Path, Topic, Lesson

## Base URL

```
/api/v1
```

Tất cả endpoint admin yêu cầu header:
```
Authorization: Bearer <admin_token>
```

---

## Response Format

**Thành công (single object):**
```json
{
  "data": { ... },
  "success": true
}
```

**Thành công (paginated):**
```json
{
  "data": [ ... ],
  "pageSize": 10,
  "totalPages": 5,
  "totalElements": 48,
  "currentPage": 0
}
```

**Thành công (message only):**
```json
{
  "message": "Deleted successfully",
  "success": true
}
```

---

## Quy trình tạo nội dung (Workflow)

```
1. Tạo Learning Path
2. Tạo Topic(s) trong Learning Path
3. Tạo Lesson(s) trong Topic (THEORY / QUIZ / CODING)
4. Publish Lesson → Publish Learning Path
```

---

## 1. Learning Path

### Enums

| Field | Values |
|-------|--------|
| `level` | `BEGINNER`, `INTERMEDIATE`, `ADVANCED` |

### 1.1 Tạo Learning Path

```
POST /api/v1/learning-paths
```

**Request Body:**
```json
{
  "name": "Data Structures & Algorithms",
  "description": "Khóa học DSA từ cơ bản đến nâng cao",
  "goal": "Nắm vững các cấu trúc dữ liệu và thuật toán phổ biến",
  "isPremium": false,
  "thumbnailUrl": "https://example.com/thumbnail.png",
  "level": "BEGINNER"
}
```

| Field | Type | Required | Note |
|-------|------|----------|------|
| `name` | string | ✅ | Tên learning path |
| `description` | string | ✅ | Mô tả |
| `goal` | string | ✅ | Mục tiêu |
| `isPremium` | boolean | ✅ | Có phải premium không |
| `thumbnailUrl` | string | ❌ | URL ảnh thumbnail |
| `level` | enum | ✅ | BEGINNER / INTERMEDIATE / ADVANCED |

**Response:**
```json
{
  "data": {
    "id": 1,
    "name": "Data Structures & Algorithms",
    "slug": "data-structures-algorithms",
    "level": "BEGINNER",
    "description": "Khóa học DSA từ cơ bản đến nâng cao",
    "goal": "Nắm vững các cấu trúc dữ liệu và thuật toán phổ biến",
    "thumbnailUrl": "https://example.com/thumbnail.png",
    "topicCount": 0,
    "totalLessonCount": 0,
    "publishedLessonCount": 0,
    "enrollmentCount": 0,
    "isPublished": false,
    "isPremium": false,
    "topics": [],
    "createdAt": "2025-01-15T10:00:00Z",
    "updatedAt": "2025-01-15T10:00:00Z"
  },
  "success": true
}
```

### 1.2 Lấy danh sách Learning Path (paginated)

```
GET /api/v1/learning-paths?page=0&size=10&level=BEGINNER&search=algo
```

| Param | Type | Required | Default | Note |
|-------|------|----------|---------|------|
| `page` | int | ❌ | 0 | Trang (0-indexed) |
| `size` | int | ❌ | 10 | Số item/trang |
| `level` | enum | ❌ | null | Filter theo level |
| `search` | string | ❌ | null | Tìm theo tên |

### 1.3 Lấy chi tiết Learning Path (kèm topics + lessons)

```
GET /api/v1/learning-paths/{id}
```

### 1.4 Cập nhật Learning Path

```
PUT /api/v1/learning-paths/{id}
```

Body giống request tạo mới.

### 1.5 Xóa Learning Path

```
DELETE /api/v1/learning-paths/{id}
```

> ⚠️ Hard delete — xóa luôn tất cả topics và lessons bên trong.

### 1.6 Toggle Publish

```
PATCH /api/v1/learning-paths/{id}/publish
```

Toggle `isPublished` giữa `true` ↔ `false`. Chỉ learning path đã published mới hiển thị cho user.

---

## 2. Topic

### 2.1 Tạo Topic

```
POST /api/v1/topics/learning-paths/{pathId}
```

**Request Body:**
```json
{
  "name": "Array & String",
  "description": "Các bài toán về mảng và chuỗi",
  "isLocked": true
}
```

| Field | Type | Required | Note |
|-------|------|----------|------|
| `name` | string | ✅ | Tên topic |
| `description` | string | ❌ | Mô tả |
| `isLocked` | boolean | ❌ | Mặc định `true`, user phải hoàn thành topic trước mới unlock |

> `displayOrder` được tự động gán (tăng dần).

**Response:**
```json
{
  "data": {
    "id": 1,
    "name": "Array & String",
    "description": "Các bài toán về mảng và chuỗi",
    "displayOrder": 1,
    "isLocked": true,
    "learningPathId": 1,
    "lessonCount": 0,
    "createdAt": "2025-01-15T10:05:00Z",
    "updatedAt": "2025-01-15T10:05:00Z",
    "lessons": []
  },
  "success": true
}
```

### 2.2 Lấy danh sách Topics theo Learning Path

```
GET /api/v1/topics/learning-paths/{pathId}
```

### 2.3 Lấy chi tiết Topic

```
GET /api/v1/topics/{topicId}
```

### 2.4 Cập nhật Topic

```
PUT /api/v1/topics/{topicId}
```

### 2.5 Xóa Topic

```
DELETE /api/v1/topics/{topicId}
```

> ⚠️ Hard delete — xóa luôn tất cả lessons bên trong.

---

## 3. Lesson

### Enums

| Field | Values |
|-------|--------|
| `type` | `THEORY`, `QUIZ`, `CODING` |
| `difficulty` | `EASY`, `MEDIUM`, `HARD` |

### Polymorphic Request

Lesson sử dụng **polymorphic JSON** — field `type` quyết định cấu trúc body:

### 3.1 Tạo Theory Lesson

```
POST /api/v1/lessons/topics/{topicId}
```

```json
{
  "title": "Introduction to Arrays",
  "type": "THEORY",
  "difficulty": "EASY",
  "content": "<h1>Arrays</h1><p>Mảng là cấu trúc dữ liệu...</p>"
}
```

| Field | Type | Required | Note |
|-------|------|----------|------|
| `title` | string | ✅ | Tiêu đề |
| `type` | enum | ✅ | Phải là `"THEORY"` |
| `difficulty` | enum | ✅ | EASY / MEDIUM / HARD |
| `content` | string | ✅ | Nội dung HTML/Markdown |

### 3.2 Tạo Quiz Lesson

```
POST /api/v1/lessons/topics/{topicId}
```

```json
{
  "title": "Array Quiz",
  "type": "QUIZ",
  "difficulty": "EASY",
  "passingScore": 70,
  "timeLimitMinutes": 15,
  "questions": [
    {
      "question": "Array có index bắt đầu từ?",
      "type": "SINGLE_CHOICE",
      "points": 1,
      "explanation": "Array luôn bắt đầu từ index 0",
      "choices": [
        { "text": "0", "isCorrect": true, "explanation": "Đúng!" },
        { "text": "1", "isCorrect": false, "explanation": "Sai, bắt đầu từ 0" }
      ]
    }
  ]
}
```

| Field | Type | Required | Note |
|-------|------|----------|------|
| `title` | string | ✅ | |
| `type` | enum | ✅ | Phải là `"QUIZ"` |
| `difficulty` | enum | ✅ | |
| `passingScore` | int | ❌ | Điểm đạt (%), mặc định 70 |
| `timeLimitMinutes` | int | ❌ | Giới hạn thời gian (phút) |
| `questions` | array | ❌ | Danh sách câu hỏi |

**Question object:**

| Field | Type | Required | Note |
|-------|------|----------|------|
| `question` | string | ✅ | Nội dung câu hỏi |
| `type` | enum | ✅ | `SINGLE_CHOICE` / `MULTIPLE_CHOICE` / `TRUE_FALSE` |
| `points` | int | ❌ | Điểm, mặc định 1 |
| `explanation` | string | ❌ | Giải thích đáp án |
| `choices` | array | ✅ | Tối thiểu 2 lựa chọn |

**Choice object:**

| Field | Type | Required | Note |
|-------|------|----------|------|
| `id` | string | ❌ | ID tùy chọn (cho FE tracking) |
| `text` | string | ✅ | Nội dung lựa chọn |
| `isCorrect` | boolean | ✅ | Đáp án đúng hay sai |
| `explanation` | string | ❌ | Giải thích |

### 3.3 Tạo Coding Lesson

```
POST /api/v1/lessons/topics/{topicId}
```

```json
{
  "title": "Two Sum",
  "type": "CODING",
  "difficulty": "EASY",
  "statement": "Cho mảng nums và target, tìm 2 phần tử có tổng bằng target...",
  "baseTimeLimitMs": 2000,
  "baseMemoryLimitMb": 256,
  "constraints": [
    "2 <= nums.length <= 10^4",
    "-10^9 <= nums[i] <= 10^9"
  ],
  "starterCode": {
    "java": "class Solution {\n  public int[] twoSum(int[] nums, int target) {\n    \n  }\n}",
    "python": "class Solution:\n  def twoSum(self, nums, target):\n    pass"
  },
  "hints": [
    "Thử dùng HashMap để lưu giá trị đã duyệt"
  ],
  "examples": [
    {
      "input": "nums = [2,7,11,15], target = 9",
      "output": "[0,1]",
      "explanation": "nums[0] + nums[1] = 2 + 7 = 9"
    }
  ],
  "testCases": [
    {
      "stdin": "4\n2 7 11 15\n9",
      "expectedStdout": "0 1",
      "isHidden": false,
      "orderIndex": 1,
      "explanation": "Basic case"
    },
    {
      "stdin": "3\n3 2 4\n6",
      "expectedStdout": "1 2",
      "isHidden": true,
      "orderIndex": 2
    }
  ]
}
```

| Field | Type | Required | Note |
|-------|------|----------|------|
| `title` | string | ✅ | |
| `type` | enum | ✅ | Phải là `"CODING"` |
| `difficulty` | enum | ✅ | |
| `statement` | string | ✅ | Đề bài (HTML/Markdown) |
| `baseTimeLimitMs` | int | ❌ | Time limit (ms), mặc định 2000 |
| `baseMemoryLimitMb` | int | ❌ | Memory limit (MB), mặc định 256 |
| `constraints` | string[] | ❌ | Ràng buộc bài toán |
| `starterCode` | map | ❌ | Key = language, value = code template |
| `hints` | string[] | ❌ | Gợi ý (tối đa 10) |
| `examples` | array | ❌ | Ví dụ minh họa (tối đa 5) |
| `testCases` | array | ❌ | Test cases (tối đa 50) |

**TestCase object:**

| Field | Type | Required | Note |
|-------|------|----------|------|
| `stdin` | string | ✅ | Input |
| `expectedStdout` | string | ✅ | Expected output |
| `isHidden` | boolean | ❌ | Ẩn khỏi user, mặc định false |
| `orderIndex` | int | ❌ | Thứ tự |
| `explanation` | string | ❌ | Giải thích test case |

### 3.4 Lấy danh sách Lessons theo Topic (paginated)

```
GET /api/v1/lessons/topics/{topicId}?page=0&size=20
```

**Response:**
```json
{
  "data": [
    {
      "id": 1,
      "title": "Introduction to Arrays",
      "slug": "introduction-to-arrays",
      "type": "THEORY",
      "displayOrder": 1,
      "difficulty": "EASY",
      "createdAt": "...",
      "updatedAt": "..."
    }
  ],
  "success": true
}
```

### 3.5 Lấy chi tiết Lesson

```
GET /api/v1/lessons/{lessonId}
```

Response khác nhau tùy type:

**Theory:**
```json
{
  "data": {
    "id": 1,
    "title": "Introduction to Arrays",
    "content": "<h1>...</h1>",
    "type": "THEORY",
    "displayOrder": 1,
    "difficulty": "Easy"
  },
  "success": true
}
```

**Quiz:**
```json
{
  "data": {
    "id": 2,
    "title": "Array Quiz",
    "type": "QUIZ",
    "displayOrder": 2,
    "difficulty": "Easy",
    "passingScore": 70,
    "timeLimitMinutes": 15,
    "questions": [
      {
        "id": 1,
        "question": "Array có index bắt đầu từ?",
        "type": "SINGLE_CHOICE",
        "points": 1,
        "explanation": "...",
        "orderIndex": 1,
        "choices": [
          { "id": "a1", "text": "0", "isCorrect": true, "explanation": "..." }
        ]
      }
    ]
  },
  "success": true
}
```

**Coding:**
```json
{
  "data": {
    "id": 3,
    "title": "Two Sum",
    "type": "CODING",
    "statement": "...",
    "displayOrder": 3,
    "difficulty": "Easy",
    "baseTimeLimitMs": 2000,
    "baseMemoryLimitMb": 256,
    "constraints": ["..."],
    "starterCode": { "java": "...", "python": "..." },
    "hints": ["..."],
    "examples": [{ "input": "...", "output": "...", "explanation": "..." }],
    "testCases": [{ "id": 1, "stdin": "...", "expectedStdout": "...", "isHidden": false, "orderIndex": 1 }],
    "editorials": []
  },
  "success": true
}
```

### 3.6 Lấy Lesson theo slug

```
GET /api/v1/lessons/slug/{slug}
```

### 3.7 Cập nhật Lesson

```
PUT /api/v1/lessons/{lessonId}
```

Body giống request tạo mới (phải giữ đúng `type`). Không thể đổi type của lesson đã tạo.

### 3.8 Xóa Lesson

```
DELETE /api/v1/lessons/{lessonId}
```

### 3.9 Toggle Publish Lesson

```
PATCH /api/v1/lessons/{lessonId}/publish
```

Toggle `isPublished` giữa `true` ↔ `false`.

---

## 4. Quy trình hoàn chỉnh (Step-by-step)

```
┌─────────────────────────────────────────────────────┐
│ 1. POST /learning-paths                             │
│    → Tạo Learning Path (isPublished = false)        │
├─────────────────────────────────────────────────────┤
│ 2. POST /topics/learning-paths/{pathId}             │
│    → Tạo Topic(s) trong Learning Path               │
├─────────────────────────────────────────────────────┤
│ 3. POST /lessons/topics/{topicId}                   │
│    → Tạo Lesson (THEORY / QUIZ / CODING)            │
│    → Lặp lại cho mỗi lesson cần tạo                │
├─────────────────────────────────────────────────────┤
│ 4. PATCH /lessons/{lessonId}/publish                │
│    → Publish từng lesson khi nội dung sẵn sàng     │
├─────────────────────────────────────────────────────┤
│ 5. PATCH /learning-paths/{id}/publish               │
│    → Publish Learning Path để user thấy            │
└─────────────────────────────────────────────────────┘
```

### Lưu ý quan trọng

1. **Slug tự động sinh** — Không cần gửi slug, BE tự tạo từ `name`/`title`.
2. **displayOrder tự động** — Topic và Lesson tự tăng thứ tự khi tạo mới.
3. **Polymorphic Lesson** — Field `type` trong JSON body quyết định cấu trúc request. Gửi sai type sẽ bị reject.
4. **Không đổi type** — Khi update lesson, `type` phải giống type ban đầu.
5. **Cascade delete** — Xóa Learning Path → xóa tất cả Topics → xóa tất cả Lessons.
6. **Publish flow** — Learning Path chỉ hiển thị cho user khi `isPublished = true`. Lesson cũng có `isPublished` riêng.

---

## Error Codes thường gặp

| HTTP | Code | Mô tả |
|------|------|--------|
| 404 | `LEARNING_PATH_NOT_FOUND` | Không tìm thấy learning path |
| 404 | `TOPIC_NOT_FOUND` | Không tìm thấy topic |
| 404 | `LESSON_NOT_FOUND` | Không tìm thấy lesson |
| 400 | `INVALID_LESSON_TYPE` | Type không hợp lệ hoặc không khớp |
| 400 | `QUIZ_LESSON_REQUIRED` | Lesson không phải quiz |
| 403 | — | Không có quyền ADMIN |
