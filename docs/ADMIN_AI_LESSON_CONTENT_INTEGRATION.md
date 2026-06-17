# Tài Liệu Tích Hợp Admin AI Generate Lesson Content

Tài liệu này mô tả contract tích hợp Front-End cho tính năng dùng AI tạo bản nháp nội dung bài học dựa trên learning path, topic, lesson hiện tại và prompt của admin.

## 1. Thông Tin Chung

- **Base URL:** `/api/v1`
- **Base path:** `/admin/ai/lessons`
- **Xác thực:** JWT trong HttpOnly cookie `access-token`
- **Phân quyền:** Chỉ tài khoản có vai trò `ADMIN`
- **Content-Type:** `application/json`
- **Cách hoạt động:** API chỉ generate bản nháp, không tự động lưu hoặc ghi đè lesson

Front-End phải gửi cookie trong request:

```ts
const api = axios.create({
  baseURL: "/api/v1",
  withCredentials: true,
});
```

## 2. Generate Nội Dung Lesson

```http
POST /api/v1/admin/ai/lessons/{lessonId}/generate-content
Content-Type: application/json
```

### Path Parameter

| Tên | Kiểu | Bắt buộc | Mô tả |
|---|---|---|---|
| `lessonId` | number | Có | ID lesson cần generate nội dung |

### Request Body

```json
{
  "provider": "GEMINI",
  "prompt": "Viết lại nội dung dễ hiểu cho người mới, bổ sung ví dụ thực tế"
}
```

| Trường | Kiểu | Bắt buộc | Quy tắc |
|---|---|---|---|
| `provider` | string hoặc `null` | Không | `OPENAI`, `GEMINI`, `CLAUDE`; bỏ trống để dùng provider mặc định |
| `prompt` | string | Có | Không được rỗng, tối đa 5000 ký tự |

Backend tự lấy context gồm:

- Learning path: tên, level, description, goal
- Topic: tên, description
- Danh sách lesson cùng topic
- Lesson hiện tại: title, type, difficulty và nội dung hiện có

LLM chỉ trả về Markdown thuần. Backend validate Markdown và build nội dung phù hợp với loại lesson hiện tại. Admin không thể dùng API này để đổi `type`, `title`, `difficulty` hoặc `displayOrder`.

| Lesson type | Cách backend build DTO |
|---|---|
| `THEORY` | Đưa toàn bộ Markdown vào `TheoryLessonRequestDTO.content` |
| `CODING` | Đưa toàn bộ Markdown vào `CodingLessonRequestDTO.statement`; giữ nguyên limits, constraints, starter code, examples và hints hiện tại |
| `QUIZ` | Parse Markdown checkbox và chỉ trả danh sách `QuizQuestion[]` |

Format Markdown quiz mà backend hỗ trợ:

```md
## Question 1
Độ phức tạp truy cập phần tử theo index trong array là gì?
- [x] O(1)
- [ ] O(n)
> Explanation: Array hỗ trợ random access.
```

- `- [x]` là đáp án đúng.
- `- [ ]` là đáp án sai.
- Nhiều đáp án `- [x]` tạo question type `MULTIPLE_CHOICE`.
- Mỗi question phải có ít nhất hai choices và ít nhất một đáp án đúng.

## 3. Response Chung

```json
{
  "success": true,
  "data": {
    "lessonId": 30,
    "lessonType": "THEORY",
    "content": {},
    "context": {
      "learningPathId": 10,
      "learningPathName": "Data Structures",
      "topicId": 20,
      "topicName": "Arrays",
      "siblingLessons": [
        "Array Basics (THEORY)",
        "Array Quiz (QUIZ)"
      ]
    },
    "inputTokens": 1240,
    "outputTokens": 860
  }
}
```

| Trường | Kiểu | Mô tả |
|---|---|---|
| `lessonId` | number | ID lesson được dùng làm context |
| `lessonType` | enum | `THEORY`, `CODING` hoặc `QUIZ` |
| `content` | `LessonRequestDTO` hoặc `QuizQuestion[]` | Bản nháp lesson; riêng `QUIZ` chỉ là danh sách câu hỏi |
| `context` | object | Thông tin curriculum đã dùng để generate |
| `inputTokens` | number hoặc `null` | Số input token provider báo cáo |
| `outputTokens` | number hoặc `null` | Số output token provider báo cáo |

Với `THEORY` và `CODING`, `content` là DTO có trường `type`. Với `QUIZ`, `content` là array `QuizQuestion[]`. Đây là dữ liệu backend build sau khi nhận Markdown từ LLM, không phải JSON do LLM trả trực tiếp.

## 4. Response Theo Loại Lesson

### 4.1. Theory Lesson

```json
{
  "lessonId": 30,
  "lessonType": "THEORY",
  "content": {
    "title": "Array Basics",
    "type": "THEORY",
    "difficulty": "EASY",
    "displayOrder": 1,
    "content": "# Array Basics\n\nMảng là...",
    "estimatedMinutes": 12
  },
  "context": {
    "learningPathId": 10,
    "learningPathName": "Data Structures",
    "topicId": 20,
    "topicName": "Arrays",
    "siblingLessons": ["Array Basics (THEORY)"]
  },
  "inputTokens": 1240,
  "outputTokens": 860
}
```

### 4.2. Coding Lesson

```json
{
  "lessonId": 31,
  "lessonType": "CODING",
  "content": {
    "title": "Two Sum",
    "type": "CODING",
    "difficulty": "EASY",
    "displayOrder": 2,
    "statement": "Cho một mảng số nguyên và một target...",
    "baseTimeLimitMs": 2000,
    "baseMemoryLimitMb": 256,
    "constraints": [
      "2 <= nums.length <= 10000"
    ],
    "starterCode": {
      "java": "class Solution {\n    public int[] twoSum(int[] nums, int target) {\n    }\n}",
      "python": "class Solution:\n    def twoSum(self, nums, target):\n        pass"
    },
    "examples": [
      {
        "input": "nums = [2,7,11,15], target = 9",
        "output": "[0,1]",
        "explanation": "nums[0] + nums[1] = 9",
        "imageUrl": null
      }
    ],
    "hints": [
      "Với mỗi phần tử, hãy xác định giá trị còn thiếu để đạt target."
    ]
  },
  "context": {
    "learningPathId": 10,
    "learningPathName": "Data Structures",
    "topicId": 20,
    "topicName": "Arrays",
    "siblingLessons": ["Two Sum (CODING)"]
  },
  "inputTokens": 1510,
  "outputTokens": 1040
}
```

LLM chỉ generate Markdown cho `statement`. Backend giữ nguyên các trường coding lesson có cấu trúc từ lesson hiện tại.

API generate coding lesson không tạo mới:

- Test cases
- Editorial/solution
- Publish state

Các phần này vẫn cần quản lý bằng API riêng.

### 4.3. Quiz Lesson

```json
{
  "lessonId": 32,
  "lessonType": "QUIZ",
  "content": [
    {
      "question": "Độ phức tạp truy cập phần tử theo index trong array là gì?",
      "type": "SINGLE_CHOICE",
      "points": 1,
      "explanation": "Array hỗ trợ random access.",
      "orderIndex": 1,
      "choices": [
        {
          "id": "a",
          "text": "O(1)",
          "isCorrect": true,
          "explanation": "Địa chỉ phần tử được tính trực tiếp."
        },
        {
          "id": "b",
          "text": "O(n)",
          "isCorrect": false,
          "explanation": null
        }
      ]
    }
  ],
  "context": {
    "learningPathId": 10,
    "learningPathName": "Data Structures",
    "topicId": 20,
    "topicName": "Arrays",
    "siblingLessons": ["Array Quiz (QUIZ)"]
  },
  "inputTokens": 1200,
  "outputTokens": 950
}
```

Backend parse question type từ số đáp án đúng:

- `SINGLE_CHOICE`
- `MULTIPLE_CHOICE`

Mỗi câu hỏi phải có ít nhất hai choices và ít nhất một choice đúng.

## 5. TypeScript Types

```ts
type LessonType = "THEORY" | "CODING" | "QUIZ";
type Difficulty = "EASY" | "MEDIUM" | "HARD";
type AiProvider = "OPENAI" | "GEMINI" | "CLAUDE";

interface LessonBase {
  title: string;
  type: LessonType;
  difficulty: Difficulty;
  displayOrder: number | null;
}

interface TheoryLessonDraft extends LessonBase {
  type: "THEORY";
  content: string;
  estimatedMinutes: number | null;
}

interface ProblemExample {
  input: string | null;
  output: string | null;
  explanation: string | null;
  imageUrl: string | null;
}

interface CodingLessonDraft extends LessonBase {
  type: "CODING";
  statement: string;
  baseTimeLimitMs: number | null;
  baseMemoryLimitMb: number | null;
  constraints: string[] | null;
  starterCode: Record<string, string> | null;
  examples: ProblemExample[] | null;
  hints: string[] | null;
}

interface QuizChoice {
  id: string | null;
  text: string;
  isCorrect: boolean;
  explanation: string | null;
}

interface QuizQuestion {
  question: string;
  type: "SINGLE_CHOICE" | "MULTIPLE_CHOICE";
  points: number | null;
  explanation: string | null;
  orderIndex: number | null;
  choices: QuizChoice[];
}

type LessonDraft =
  | TheoryLessonDraft
  | CodingLessonDraft;

interface GenerateLessonContentResponse {
  lessonId: number;
  lessonType: LessonType;
  content: LessonDraft | QuizQuestion[];
  context: {
    learningPathId: number;
    learningPathName: string;
    topicId: number;
    topicName: string;
    siblingLessons: string[];
  };
  inputTokens: number | null;
  outputTokens: number | null;
}
```

## 6. Ví Dụ Gọi API

```ts
interface GenerateLessonContentRequest {
  provider?: AiProvider;
  prompt: string;
}

async function generateLessonContent(
  lessonId: number,
  request: GenerateLessonContentRequest,
) {
  const response = await api.post<{
    success: true;
    data: GenerateLessonContentResponse;
  }>(`/admin/ai/lessons/${lessonId}/generate-content`, request);

  return response.data.data;
}
```

Render form theo discriminated union:

```ts
function openDraftEditor(generated: GenerateLessonContentResponse) {
  switch (generated.lessonType) {
    case "THEORY":
      return openTheoryEditor(generated.content as TheoryLessonDraft);
    case "CODING":
      return openCodingEditor(generated.content as CodingLessonDraft);
    case "QUIZ":
      return openQuizQuestionEditor(generated.content as QuizQuestion[]);
  }
}
```

## 7. Flow UI Khuyến Nghị

1. Admin mở trang chỉnh sửa lesson.
2. Admin nhập prompt và tùy chọn provider.
3. FE gọi API generate và hiển thị loading vì request có thể mất nhiều giây.
4. FE nhận `data.content`, đưa vào form editor dưới dạng bản nháp. Với quiz, đây là `QuizQuestion[]`.
5. Admin review và chỉnh sửa nội dung.
6. Khi admin xác nhận lưu, FE lưu draft theo loại lesson.

Với `THEORY` và `CODING`, gửi toàn bộ `content` sang API update lesson:

```http
PUT /api/v1/lessons/{lessonId}
Content-Type: application/json
```

```ts
await api.put(`/lessons/${generated.lessonId}`, generated.content);
```

Với `QUIZ`, AI chỉ generate danh sách câu hỏi. FE lưu từng câu bằng API questions:

```http
POST /api/v1/questions/lessons/{lessonId}
PUT /api/v1/questions/{questionId}
DELETE /api/v1/questions/{questionId}
```

Ví dụ thêm các câu hỏi AI vừa generate:

```ts
const questions = generated.content as QuizQuestion[];

for (const question of questions) {
  await api.post(`/questions/lessons/${generated.lessonId}`, question);
}
```

Nếu quiz đã có questions, FE cần hiển thị thao tác merge/replace cho admin. Khi replace, lấy danh sách hiện tại qua `GET /questions/lessons/{lessonId}`, xóa các câu cũ rồi thêm draft mới. Backend chưa có bulk replace endpoint nên không nên tự động replace mà không có xác nhận.

Không gọi API update tự động ngay sau khi generate. Admin nên có cơ hội review vì nội dung AI có thể chưa chính xác.

## 8. Xử Lý Lỗi

Error response:

```json
{
  "errors": "AI returned invalid lesson content. Please try again",
  "success": false,
  "code": 8009
}
```

| HTTP | Code | Trường hợp |
|---|---:|---|
| `400 Bad Request` | `1` | Prompt rỗng, dài quá 5000 ký tự hoặc request không hợp lệ |
| `401 Unauthorized` | `2` hoặc `3` | Chưa đăng nhập hoặc token không hợp lệ |
| `404 Not Found` | `4005` | Không tìm thấy lesson |
| `502 Bad Gateway` | `8009` | AI trả về nội dung không hợp lệ |
| `503 Service Unavailable` | `8005` | Các AI provider đều không khả dụng |
| `503 Service Unavailable` | `8007` | Provider chưa được cấu hình |

FE nên:

- Giữ nguyên prompt khi request lỗi để admin có thể retry.
- Hiển thị lỗi inline hoặc toast.
- Cho phép đổi provider rồi generate lại.
- Không xóa nội dung form hiện tại trước khi request generate thành công.
- Hiển thị token usage như thông tin phụ, không phụ thuộc vào trường này vì provider có thể trả `null`.

## 9. Lưu Ý Tích Hợp

- `content.type` quyết định cấu trúc của draft; dùng trường này thay vì tự suy luận từ các field.
- Backend giữ nguyên metadata lesson hiện tại: `title`, `type`, `difficulty`, `displayOrder`.
- LLM luôn trả Markdown thuần; backend chuyển Markdown thành DTO trước khi trả response.
- Theory `content` và coding `statement` là chuỗi Markdown; editor cần hỗ trợ Markdown.
- Coding limits, starter code, constraints, examples và hints được giữ nguyên từ lesson hiện tại.
- Quiz Markdown phải đúng format heading và checkbox để backend parse được.
- Validate lại draft trên FE trước khi gọi API update.
- `PUT /lessons/{lessonId}` chưa lưu `questions` của quiz; dùng các API `/questions/**`.
- Coding lesson cần test cases riêng trước khi publish.
- Nội dung generate chưa được publish tự động.
