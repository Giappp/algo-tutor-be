# Tài liệu Tích hợp API Admin Dashboard - AlgoTutor

Tài liệu này đặc tả chi tiết các endpoints thuộc nhóm chức năng **Admin Dashboard** giúp Quản trị viên (Admin) giám sát toàn bộ hệ thống, đo lường AI Token tiêu thụ và kiểm soát API Quota / Rate Limiting trong thời gian thực.

---

## 1. Thông tin chung về Bảo mật & Định tuyến

- **Base URL:** `/api/v1`
- **Bảo mật:** Tất cả các API dưới đây đều yêu cầu Authentication qua JWT (Header `Authorization: Bearer <token>`).
- **Phân quyền:** Chỉ cho phép tài khoản có vai trò `ADMIN` truy cập (HTTP `403 Forbidden` sẽ trả về đối với các vai trò khác).

---

## 2. Danh sách API Endpoints

### 2.1. Thống kê Tổng quan Hệ thống (System Overview)

Trả về các chỉ số đo lường chung và phân bố trạng thái các thực thể trên toàn hệ thống.

- **Endpoint:** `GET /admin/dashboard/overview`
- **Headers:** `Authorization: Bearer <JWT>`
- **Response mẫu (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "totalUsers": 1250,
    "activeSessions": 45,
    "totalLessons": 80,
    "totalEnrollments": 450,
    "totalSubmissions": 12890,
    "totalQuizAttempts": 845,
    "verdictDistribution": {
      "ACCEPTED": 7820,
      "WRONG_ANSWER": 3200,
      "TIME_LIMIT_EXCEEDED": 450,
      "COMPILATION_ERROR": 820,
      "RUNTIME_ERROR": 600
    },
    "lessonDistribution": {
      "THEORY": 30,
      "CODING": 35,
      "QUIZ": 15
    }
  }
}
```

**Mô tả các trường dữ liệu:**
| Trường | Kiểu dữ liệu | Ý nghĩa |
| :--- | :--- | :--- |
| `totalUsers` | Long | Tổng số người dùng đăng ký trong hệ thống |
| `activeSessions` | Long | Số phiên đăng nhập còn hạn (Active Refresh Tokens) |
| `totalLessons` | Long | Tổng số bài học |
| `totalEnrollments` | Long | Tổng số lượt ghi danh học của người dùng |
| `totalSubmissions` | Long | Tổng số lần nộp bài tập coding |
| `totalQuizAttempts` | Long | Tổng số lần thử làm quiz của học viên |
| `verdictDistribution` | Map<String, Long> | Biểu đồ phân bổ kết quả chấm bài của hệ thống |
| `lessonDistribution` | Map<String, Long> | Biểu đồ phân bổ bài học theo thể loại (Lý thuyết, Thực hành, Trắc nghiệm) |

---

### 2.2. Giám sát lượng tiêu thụ AI Token (AI Token Usage)

Cung cấp thông tin chi tiết về lượng token đã tiêu thụ, lịch sử hàng ngày và danh sách top người dùng sử dụng nhiều nhất để phát hiện lạm dụng.

- **Endpoint:** `GET /admin/dashboard/ai-tokens`
- **Query Params:**
  - `days` (int, default `30`): Số ngày gần đây muốn truy vấn lịch sử tiêu thụ.
- **Headers:** `Authorization: Bearer <JWT>`
- **Response mẫu (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "totalInputTokens": 450200,
    "totalOutputTokens": 890400,
    "totalTokensCombined": 1340600,
    "dailyUsage": [
      {
        "date": "2026-05-25",
        "inputTokens": 12000,
        "outputTokens": 24000,
        "totalTokens": 36000
      },
      {
        "date": "2026-05-26",
        "inputTokens": 15000,
        "outputTokens": 31000,
        "totalTokens": 46000
      }
    ],
    "usageByMode": {
      "CHAT": 920400,
      "ROADMAP_ADVISORY": 320200,
      "EXPLANATION": 100000
    },
    "topConsumers": [
      {
        "userId": "d72d242c-a2b1-4f81-80a1-7c9b83b38151",
        "username": "coder_pro",
        "email": "coder_pro@gmail.com",
        "inputTokens": 50000,
        "outputTokens": 95000,
        "totalTokens": 145000
      }
    ]
  }
}
```

---

### 2.3. Giám sát API Quota & Rate Limiting (Active API Quotas)

Lấy trực tiếp trạng thái bộ nhớ đệm (in-memory sliding window rate limiter) để hiển thị danh sách các tài khoản đang hoạt động mạnh nhất và tỉ lệ tiêu thụ quota của họ.

- **Endpoint:** `GET /admin/dashboard/api-quotas`
- **Headers:** `Authorization: Bearer <JWT>`
- **Response mẫu (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "key": "ai-chat:d72d242c-a2b1-4f81-80a1-7c9b83b38151",
      "action": "AI Chat",
      "userId": "d72d242c-a2b1-4f81-80a1-7c9b83b38151",
      "username": "coder_pro",
      "email": "coder_pro@gmail.com",
      "currentRequests": 18,
      "maxLimit": 20,
      "windowSeconds": 60,
      "oldestTimestampMs": 1779958045000
    },
    {
      "key": "judge:submit:c92e10a2-f81d-40c2-901d-5b23d9b1836a",
      "action": "Submit Code",
      "userId": "c92e10a2-f81d-40c2-901d-5b23d9b1836a",
      "username": "learning_newbie",
      "email": "newbie@yahoo.com",
      "currentRequests": 3,
      "maxLimit": 5,
      "windowSeconds": 60,
      "oldestTimestampMs": 1779958060000
    }
  ]
}
```

**Chi tiết thuộc tính của đối tượng Quota:**
- `action`: Phân loại hành động (`AI Chat`, `AI Advisor / General Chat`, `Run Code`, `Submit Code`).
- `currentRequests`: Số lượng requests đã thực hiện trong window hiện tại.
- `maxLimit`: Số lượng requests tối đa được cho phép trong window.
- `windowSeconds`: Độ rộng của window trượt (giây) - ví dụ `60` giây.
- `oldestTimestampMs`: Thời điểm request đầu tiên của window hiện tại được thực hiện (dùng để tính toán thời gian giải phóng quota ở FE).
