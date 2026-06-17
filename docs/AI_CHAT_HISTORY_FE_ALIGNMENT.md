# FE Alignment - AI Chat History

Tài liệu này mô tả contract lấy lịch sử tin nhắn cho hai luồng:

- **AI Tutor Chat**: chat theo bài học, conversation có type `LESSON`.
- **General Chat**: chat chung, conversation có type `GENERAL`.

## 1. Endpoint

Base URL mặc định:

```text
/api/v1
```

| Luồng | Method | Endpoint |
|---|---|---|
| AI Tutor Chat | `GET` | `/ai/chat/history/{conversationId}` |
| General Chat | `GET` | `/ai/general/chat/history/{conversationId}` |

Hai endpoint yêu cầu user đã đăng nhập. BE chỉ trả conversation thuộc user hiện tại và đúng loại endpoint.

## 2. Response Contract

Response thành công:

```json
{
  "success": true,
  "data": {
    "conversationId": "5b1b7e5f-1d1d-4b0d-9c93-67f7d087c0fd",
    "type": "LESSON",
    "lessonId": 42,
    "title": "Chat về Two Sum",
    "provider": "GEMINI",
    "createdAt": "2026-06-10T08:00:00Z",
    "updatedAt": "2026-06-10T08:05:00Z",
    "messages": [
      {
        "id": "6933cc77-0a21-4e43-bd87-e09ad6b11216",
        "role": "USER",
        "content": "Cho mình một gợi ý",
        "mode": "HINT",
        "createdAt": "2026-06-10T08:01:00Z"
      },
      {
        "id": "1e44ab51-1e21-4b6b-b326-3703de98d16a",
        "role": "ASSISTANT",
        "content": "Hãy thử dùng hash map...",
        "mode": "HINT",
        "createdAt": "2026-06-10T08:01:02Z"
      }
    ]
  }
}
```

Quy ước:

- `messages` luôn được sắp xếp từ cũ đến mới.
- `lessonId` là `null` đối với General Chat.
- `mode` có thể là `null`, đặc biệt ở General Chat.
- `role` hiện có thể là `SYSTEM`, `USER`, `ASSISTANT`, hoặc `TOOL`. UI chat thông thường chỉ cần render `USER` và `ASSISTANT`.
- Token usage nội bộ không được trả về FE.
- Conversation hợp lệ nhưng chưa có tin nhắn trả `messages: []`.

## 3. Error Contract

Nếu conversation không tồn tại, không thuộc user hiện tại, hoặc gọi nhầm endpoint theo type:

```http
HTTP/1.1 404 Not Found
```

```json
{
  "success": false,
  "errors": "Conversation not found",
  "code": 8002
}
```

FE nên xóa `conversationId` đang lưu khi nhận code `8002`.

## 4. TypeScript Types

```ts
type AiConversationType = "LESSON" | "GENERAL";
type AiMessageRole = "SYSTEM" | "USER" | "ASSISTANT" | "TOOL";
type AiProvider = "OPENAI" | "GEMINI" | "CLAUDE";

type AiChatHistoryMessage = {
  id: string;
  role: AiMessageRole;
  content: string;
  mode: string | null;
  createdAt: string;
};

type AiChatHistory = {
  conversationId: string;
  type: AiConversationType;
  lessonId: number | null;
  title: string | null;
  provider: AiProvider;
  createdAt: string;
  updatedAt: string | null;
  messages: AiChatHistoryMessage[];
};

type ApiResponse<T> = {
  success: boolean;
  data: T;
};
```

## 5. FE Integration

```ts
export async function getAiTutorChatHistory(conversationId: string) {
  const response = await api.get<ApiResponse<AiChatHistory>>(
    `/ai/chat/history/${conversationId}`,
  );
  return response.data.data;
}

export async function getGeneralChatHistory(conversationId: string) {
  const response = await api.get<ApiResponse<AiChatHistory>>(
    `/ai/general/chat/history/${conversationId}`,
  );
  return response.data.data;
}
```

Flow khuyến nghị khi restore chat:

1. Lấy `conversationId` đang lưu cho đúng lesson hoặc General Chat.
2. Gọi endpoint history tương ứng.
3. Thay danh sách message hiện tại bằng `data.messages`.
4. Chỉ render các message có role `USER` hoặc `ASSISTANT`.
5. Tiếp tục gửi `data.conversationId` trong request chat/stream kế tiếp.
6. Nếu nhận lỗi `8002`, xóa ID cũ và tạo conversation mới theo flow hiện tại.

## 6. FE Checklist

- [ ] Lưu riêng `conversationId` cho từng lesson và General Chat.
- [ ] Gọi đúng endpoint theo loại conversation.
- [ ] Render message theo thứ tự BE trả về, không đảo lại.
- [ ] Hỗ trợ `mode: null` và `lessonId: null`.
- [ ] Xóa conversation ID cũ khi nhận `CONVERSATION_NOT_FOUND (8002)`.
- [ ] Không hiển thị message role `SYSTEM`/`TOOL` trong chat UI thông thường.
