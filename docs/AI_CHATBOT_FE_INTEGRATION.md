# AI Chatbot FE Integration Guide

Tài liệu này là hướng dẫn tích hợp hoàn chỉnh module AI Chatbot từ Backend AlgoTutor sang Frontend.

> Lưu ý base URL: controller hiện tại expose trực tiếp dưới prefix `/ai`. Nếu FE đang gọi qua API gateway hoặc reverse proxy có prefix `/api/v1`, hãy map tương ứng thành `/api/v1/ai/...`.

---

## 1. Tổng quan

Module AI Chatbot có 2 luồng chính:

1. **Lesson Chat**
   - Chat theo ngữ cảnh bài học.
   - Có mode hỗ trợ: `HINT`, `EXPLAIN`, `DEBUG`, `REVIEW`, `COMPLEXITY`, `SOLUTION`, `NEXT_STEP`.
   - Có quick actions và hint limit.
   - Có guardrails chống lộ lời giải/full code ngoài mode `SOLUTION`.

2. **General Chat**
   - Chat chung, hỏi lập trình, hỏi nền tảng, hoặc tư vấn roadmap.
   - Backend tự route intent: `GENERAL`, `CODING_HELP`, `PLATFORM_HELP`, `ROADMAP_ADVISORY`.
   - Nếu là roadmap advisory, response có thể kèm danh sách roadmap cards.

Tất cả API yêu cầu user đã đăng nhập. FE nên gửi credential/JWT giống các API authenticated khác của hệ thống.

---

## 2. Response Wrapper

Các endpoint blocking trả về `ApiResponse<T>`:

```json
{
  "success": true,
  "data": {
    "...": "..."
  }
}
```

Các endpoint streaming trả về `text/event-stream`, không bọc `ApiResponse`.

Error response thường có dạng:

```json
{
  "success": false,
  "errors": "Invalid chat mode",
  "code": 8000
}
```

Riêng rate limit có header:

```http
Retry-After: 45
```

---

## 3. Auth, Rate Limit, Provider

### Auth

Tất cả endpoint `/ai/**` cần authenticated user.

FE cần gửi:

```ts
fetch(url, {
  credentials: "include",
  headers: {
    "Content-Type": "application/json"
  }
})
```

Nếu hệ thống FE dùng Bearer token thay vì cookie, thêm:

```http
Authorization: Bearer <accessToken>
```

### Rate Limit

Backend đang giới hạn mặc định:

```txt
20 requests / 60 seconds / user / chat type
```

Khi bị limit:

```http
HTTP 429 Too Many Requests
Retry-After: <seconds>
```

FE nên disable input/send button và hiển thị countdown theo `Retry-After`.

### Provider

Request có thể truyền:

```txt
OPENAI | GEMINI | CLAUDE
```

Nếu bỏ trống `provider`, backend dùng provider mặc định. Backend có fallback chain nếu provider chính lỗi.

Khuyến nghị FE:
- Không expose provider selector cho user thường.
- Nếu cần debug/admin, truyền provider explicit.

---

## 4. Lesson Chat API

### 4.1 Bootstrap Lesson Chat

Dùng khi mở trang học để lấy `conversationId` gần nhất hoặc tạo conversation mới.

```http
GET /ai/chat/bootstrap?lessonSlug={lessonSlug}
```

Response:

```json
{
  "success": true,
  "data": {
    "conversationId": "5b1b7e5f-1d1d-4b0d-9c93-67f7d087c0fd",
    "answer": "Bạn đang học bài \"Two Sum\".\n\nMình có thể hỗ trợ bạn theo từng bước...",
    "mode": "BOOTSTRAP",
    "quickActions": null,
    "sources": [],
    "canAskNextHint": true
  }
}
```

FE usage:
- Gọi khi mount lesson page.
- Lưu `conversationId` vào state của chat widget.
- Render `answer` như assistant message đầu tiên nếu UI muốn có onboarding.

---

### 4.2 Lesson Chat Blocking

Dùng làm fallback khi browser/client không xử lý streaming tốt.

```http
POST /ai/chat
Content-Type: application/json
```

Request:

```json
{
  "conversationId": "5b1b7e5f-1d1d-4b0d-9c93-67f7d087c0fd",
  "lessonId": 10,
  "lessonSlug": "two-sum",
  "provider": "GEMINI",
  "mode": "HINT",
  "message": "Tôi nên bắt đầu từ đâu?",
  "code": null,
  "language": null,
  "judgeResult": null,
  "errorMessage": null,
  "failedTestCases": []
}
```

Response:

```json
{
  "success": true,
  "data": {
    "conversationId": "5b1b7e5f-1d1d-4b0d-9c93-67f7d087c0fd",
    "answer": "Hãy thử nghĩ xem mỗi phần tử cần tìm một giá trị bù là gì...",
    "mode": "HINT",
    "quickActions": [
      {
        "label": "Gợi ý tiếp theo",
        "intent": "NEXT_HINT",
        "mode": "HINT",
        "message": "Cho tôi xin gợi ý tiếp theo nhé."
      }
    ],
    "sources": [],
    "canAskNextHint": true
  }
}
```

---

### 4.3 Lesson Chat Streaming

Endpoint FE nên ưu tiên dùng.

```http
POST /ai/chat/stream
Content-Type: application/json
Accept: text/event-stream
```

Request body giống `/ai/chat`.

SSE events:

```txt
event: message
data: {"answer":"Hãy thử nghĩ xem "}

event: message
data: {"answer":"mỗi phần tử cần tìm "}

event: metadata
data: {"conversationId":"5b1b7e5f-1d1d-4b0d-9c93-67f7d087c0fd","answer":null,"mode":"HINT","quickActions":[...],"sources":[],"canAskNextHint":true}
```

Quan trọng:
- `message` event chứa chunk text trong field `answer`.
- `metadata` event là event cuối, dùng để cập nhật `conversationId`, quick actions, `canAskNextHint`.
- Với mode không phải `SOLUTION`, backend có thể buffer response để chạy guardrails trước. FE vẫn xử lý giống nhau, nhưng có thể thấy chunk ít hơn hoặc chỉ một chunk cuối.

---

## 5. Lesson Chat Request Fields

`AiLessonChatRequest`

| Field | Type | Required | Mô tả |
|---|---:|---:|---|
| `conversationId` | UUID | No | Null khi tạo cuộc trò chuyện mới. Sau response đầu tiên, FE nên reuse. |
| `lessonId` | number | Yes | ID bài học hiện tại. |
| `lessonSlug` | string | Yes | Slug bài học hiện tại. |
| `provider` | string | No | `OPENAI`, `GEMINI`, `CLAUDE`; null để dùng default. |
| `mode` | string | Yes | Một trong các mode ở phần 6. |
| `message` | string | Conditional | Nội dung user hỏi. Cần có `message` hoặc `code`. Max 5000 chars. |
| `code` | string | Conditional | Code hiện tại trong editor. Max 10000 chars. |
| `language` | string | No | Ví dụ `java`, `python`, `cpp`, `javascript`. |
| `judgeResult` | string | No | Kết quả judge gần nhất, ví dụ `WRONG_ANSWER`, `ACCEPTED`. |
| `errorMessage` | string | No | Compile/runtime error hoặc message từ judge. |
| `failedTestCases` | string[] | No | Danh sách failed cases FE muốn gửi cho AI. |

Validation:
- `message` và `code` không được cùng rỗng.
- `DEBUG`, `REVIEW`, `COMPLEXITY` bắt buộc có `code`.
- `message` tối đa 5000 ký tự.
- `code` tối đa 10000 ký tự.

---

## 6. Lesson Chat Modes

| Mode | Khi FE dùng | Code required | Ghi chú UI |
|---|---|---:|---|
| `HINT` | User xin gợi ý từng bước | No | Respect `canAskNextHint`; disable nút hint khi false. |
| `EXPLAIN` | Giải thích đề/lý thuyết/ý tưởng | No | Không kỳ vọng full solution. |
| `DEBUG` | User có lỗi code/testcase | Yes | Gửi `code`, `language`, `errorMessage`, `failedTestCases` nếu có. |
| `REVIEW` | Đánh giá code/style/correctness | Yes | Gửi code hiện tại. |
| `COMPLEXITY` | Phân tích Big-O | Yes | Gửi code hiện tại. |
| `SOLUTION` | User muốn lời giải đầy đủ | No | Chỉ mode này backend cho phép full code solution. |
| `NEXT_STEP` | User muốn một bước tiếp theo | No | Dùng cho quick action “Tôi cần làm gì tiếp theo?”. |

Guardrails:
- Ngoài `SOLUTION`, backend sẽ cố chặn full code/full solution.
- Nếu model trả lời vượt policy, backend có thể thay câu trả lời bằng hướng dẫn an toàn.
- FE không cần tự filter code, nhưng nên label rõ mode `SOLUTION` là “xem lời giải đầy đủ”.

---

## 7. Quick Actions

Response lesson chat có `quickActions`.

```ts
type AiQuickAction = {
  label: string;
  intent:
    | "FREE_CHAT"
    | "EXPLAIN_PROBLEM"
    | "GIVE_HINT"
    | "NEXT_HINT"
    | "DEBUG_CODE"
    | "EXPLAIN_CODE"
    | "EXPLAIN_ERROR"
    | "ANALYZE_COMPLEXITY"
    | "REVIEW_CODE"
    | "SUGGEST_NEXT_STEP"
    | "CONTINUE"
    | "REGENERATE";
  mode: string;
  message: string;
};
```

FE behavior:
- Render quick actions sau assistant answer.
- Khi user click quick action:
  - Fill request `mode = quickAction.mode`.
  - Fill request `message = quickAction.message`.
  - Vẫn gửi kèm `conversationId`, `lessonId`, `lessonSlug`.
  - Nếu mode yêu cầu code, gửi code editor hiện tại.

---

## 8. General Chat API

### 8.1 General Chat Blocking

```http
POST /ai/general/chat
Content-Type: application/json
```

Request:

```json
{
  "conversationId": null,
  "provider": "GEMINI",
  "message": "Tôi mới học Java, nên bắt đầu lộ trình nào?"
}
```

Response:

```json
{
  "success": true,
  "data": {
    "conversationId": "1a89a0c4-bcd2-47d0-885a-b258d8736810",
    "answer": "Dựa trên việc bạn mới học Java, mình đề xuất...",
    "roadmaps": [
      {
        "name": "DSA Fundamentals",
        "slug": "dsa-fundamentals",
        "level": "BEGINNER",
        "description": "Master the essential data structures...",
        "thumbnailUrl": "https://...",
        "topicCount": 4,
        "lessonCount": 14,
        "isPremium": false
      }
    ]
  }
}
```

---

### 8.2 General Chat Streaming

```http
POST /ai/general/chat/stream
Content-Type: application/json
Accept: text/event-stream
```

Request body giống `/ai/general/chat`.

SSE events:

```txt
event: message
data: {"answer":"Dựa trên mục tiêu của bạn, "}

event: message
data: {"answer":"mình đề xuất..."}

event: metadata
data: {"conversationId":"1a89a0c4-bcd2-47d0-885a-b258d8736810","roadmaps":[...]}
```

Khác với lesson metadata:
- General metadata không có `mode`, `quickActions`, `canAskNextHint`.
- General metadata có `roadmaps`.

---

## 9. General Chat Intent Router

Backend tự phân loại intent trong general chat:

| Intent | Khi nào xảy ra | FE cần làm gì |
|---|---|---|
| `ROADMAP_ADVISORY` | User hỏi lộ trình, học gì, bắt đầu học gì, chọn course | Render `roadmaps` cards nếu metadata/response có. |
| `CODING_HELP` | User hỏi debug/code/error/testcase/Big-O chung | Render answer bình thường. Có thể gợi ý user chuyển sang lesson chat nếu đang ở bài học. |
| `PLATFORM_HELP` | User hỏi cách dùng AlgoTutor, nộp bài, quiz, tài khoản | Render answer bình thường. |
| `GENERAL` | Small talk hoặc câu hỏi học tập chung | Render answer bình thường. |

FE không cần gửi intent. Backend tự classify.

---

## 10. FE SSE Implementation

Vì `EventSource` mặc định không hỗ trợ `POST`, FE nên dùng `fetch` + ReadableStream parser hoặc thư viện hỗ trợ POST SSE.

Ví dụ TypeScript tối giản:

```ts
type SseHandler = {
  onMessage: (chunk: string) => void;
  onMetadata: (data: unknown) => void;
  onError?: (error: unknown) => void;
};

export async function postSse(
  url: string,
  body: unknown,
  handlers: SseHandler,
  signal?: AbortSignal
) {
  const res = await fetch(url, {
    method: "POST",
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      "Accept": "text/event-stream"
    },
    body: JSON.stringify(body),
    signal
  });

  if (!res.ok || !res.body) {
    const error = await res.json().catch(() => null);
    throw { status: res.status, retryAfter: res.headers.get("Retry-After"), error };
  }

  const reader = res.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";

  while (true) {
    const { value, done } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });
    const events = buffer.split(/\n\n/);
    buffer = events.pop() ?? "";

    for (const rawEvent of events) {
      const eventName = rawEvent.match(/^event:\s*(.+)$/m)?.[1]?.trim();
      const dataLine = rawEvent.match(/^data:\s*(.+)$/m)?.[1];
      if (!eventName || !dataLine) continue;

      const data = JSON.parse(dataLine);
      if (eventName === "message") {
        handlers.onMessage(data.answer ?? "");
      }
      if (eventName === "metadata") {
        handlers.onMetadata(data);
      }
    }
  }
}
```

Abort/cancel:

```ts
const controller = new AbortController();

postSse("/ai/chat/stream", payload, handlers, controller.signal);

// user clicks stop
controller.abort();
```

---

## 11. Recommended FE State Model

```ts
type ChatRole = "user" | "assistant";

type ChatMessage = {
  id: string;
  role: ChatRole;
  content: string;
  mode?: string;
  pending?: boolean;
};

type LessonChatState = {
  conversationId?: string;
  messages: ChatMessage[];
  quickActions: AiQuickAction[];
  canAskNextHint: boolean | null;
  isStreaming: boolean;
  error?: string;
};

type GeneralChatState = {
  conversationId?: string;
  messages: ChatMessage[];
  roadmaps: RoadmapInfo[];
  isStreaming: boolean;
  error?: string;
};
```

Lesson chat submit flow:

1. Add user message locally.
2. Add empty assistant message with `pending=true`.
3. POST `/ai/chat/stream`.
4. Append each `message.answer` chunk to pending assistant message.
5. On `metadata`, update:
   - `conversationId`
   - `quickActions`
   - `canAskNextHint`
   - `pending=false`
6. On error, mark pending assistant message as failed or remove it.

General chat submit flow:

1. Add user message locally.
2. Add empty assistant message.
3. POST `/ai/general/chat/stream`.
4. Append `message.answer` chunks.
5. On `metadata`, update:
   - `conversationId`
   - `roadmaps`
6. Render roadmap cards if `roadmaps.length > 0`.

---

## 12. Lesson Chat Payload Builders

### Hỏi hint

```ts
const payload = {
  conversationId,
  lessonId,
  lessonSlug,
  provider: null,
  mode: "HINT",
  message: "Cho tôi xin một gợi ý",
  code: editorCode || null,
  language,
  judgeResult: lastJudge?.verdict ?? null,
  errorMessage: lastJudge?.errorMessage ?? null,
  failedTestCases: lastJudge?.failedTestCases ?? []
};
```

### Debug code

```ts
const payload = {
  conversationId,
  lessonId,
  lessonSlug,
  provider: null,
  mode: "DEBUG",
  message: "Giúp tôi tìm lỗi trong code này",
  code: editorCode,
  language,
  judgeResult: lastJudge?.verdict ?? null,
  errorMessage: lastJudge?.errorMessage ?? null,
  failedTestCases: lastJudge?.failedTestCases ?? []
};
```

### Xem solution

```ts
const payload = {
  conversationId,
  lessonId,
  lessonSlug,
  provider: null,
  mode: "SOLUTION",
  message: "Cho tôi xem lời giải đầy đủ",
  code: editorCode || null,
  language,
  judgeResult: null,
  errorMessage: null,
  failedTestCases: []
};
```

---

## 13. Error Handling Map

| HTTP | Code | ErrorCode | FE behavior |
|---:|---:|---|---|
| 400 | 8000 | `INVALID_CHAT_MODE` | Check mode mapping. |
| 400 | 8001 | `CODE_REQUIRED` | Prompt user to submit/include code. |
| 404 | 8002 | `CONVERSATION_NOT_FOUND` | Reset `conversationId`, call bootstrap or start new chat. |
| 400 | 8003 | `UNSUPPORTED_PROVIDER` | Remove provider override. |
| 429 | 8004 | `RATE_LIMIT_EXCEEDED` | Disable send, show countdown from `Retry-After`. |
| 503 | 8005 | `AI_SERVICE_UNAVAILABLE` | Show retry action. |
| 400 | 8006 | `NO_MORE_HINTS` | Disable hint button. |
| 503 | 8007 | `PROVIDER_NOT_CONFIGURED` | Hide provider/admin option; retry default later. |

Validation example:

```json
{
  "success": false,
  "errors": {
    "code": ["Code must not exceed 10000 characters"]
  },
  "code": 1
}
```

---

## 14. UI/UX Notes

- Use streaming endpoint by default.
- Keep input disabled while `isStreaming=true`; allow “Stop” via `AbortController`.
- For `HINT`, disable quick action if `canAskNextHint === false`.
- For `SOLUTION`, show a confirmation UI if product wants to discourage early reveal.
- Render Markdown safely. Sanitize HTML if markdown renderer allows raw HTML.
- Code editor content can be large; avoid sending code for modes that do not need it unless useful.
- For `DEBUG`, `REVIEW`, `COMPLEXITY`, always send current editor code.
- For general roadmap chat, render roadmap cards from metadata/response `roadmaps`.
- Store `conversationId` per lesson for lesson chat and separately for general chat.

---

## 15. Endpoint Summary

| Feature | Method | Endpoint | Response |
|---|---|---|---|
| Bootstrap lesson chat | GET | `/ai/chat/bootstrap?lessonSlug={slug}` | `ApiResponse<AiChatResponse>` |
| Lesson chat blocking | POST | `/ai/chat` | `ApiResponse<AiChatResponse>` |
| Lesson chat streaming | POST | `/ai/chat/stream` | SSE: `message`, `metadata` |
| General chat blocking | POST | `/ai/general/chat` | `ApiResponse<AiGeneralChatResponse>` |
| General chat streaming | POST | `/ai/general/chat/stream` | SSE: `message`, `metadata` |

---

## 16. Minimal FE Checklist

- [ ] Call bootstrap when entering lesson page.
- [ ] Keep lesson `conversationId` in chat state.
- [ ] Implement POST SSE parser.
- [ ] Append `message.answer` chunks to current assistant bubble.
- [ ] Parse lesson `metadata` and update quick actions + hint state.
- [ ] Parse general `metadata` and update roadmap cards.
- [ ] Send editor code for `DEBUG`, `REVIEW`, `COMPLEXITY`.
- [ ] Respect `Retry-After` on 429.
- [ ] Reset conversation if backend returns `CONVERSATION_NOT_FOUND`.
- [ ] Render Markdown safely.
