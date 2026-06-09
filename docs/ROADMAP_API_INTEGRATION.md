# Hướng Dẫn Tích Hợp API Roadmap — AlgoTutor Frontend

Tài liệu này cung cấp hướng dẫn tích hợp chi tiết các API endpoints liên quan đến lộ trình học tập (**Roadmap**) từ lớp `RoadmapController.java` cho Frontend (FE).

---

## 1. Tổng Quan & Cấu Hình Chung

- **Base URL:** `{NEXT_PUBLIC_API_BASE_URL}/api/v1` (tất cả các endpoint dưới đây đều đi sau Base URL này).
- **Endpoint Prefix:** `/roadmaps`
- **Authentication:** Cookie-based session. Đối với các request yêu cầu đăng nhập, FE bắt buộc phải gửi kèm credentials:
  - **Axios:** `withCredentials: true`
  - **Fetch API:** `credentials: 'include'`

---

## 2. Cấu Trúc Response Chuẩn

Backend sử dụng hai cấu trúc wrapper chuẩn cho tất cả các response.

### 2.1 Cấu trúc API Response chuẩn (`ApiResponse<T>`)
Áp dụng cho các API trả về object đơn lẻ hoặc thông báo.

```json
{
  "success": true,
  "message": "OK", // Hoặc null/thông báo thành công
  "data": { ... }  // Payload chính
}
```

Khi có lỗi xảy ra (ví dụ lỗi nghiệp vụ từ hệ thống):
```json
{
  "success": false,
  "code": 4017, // Mã lỗi nghiệp vụ (xem phần 4)
  "errors": "errors.lesson-locked" // Chuỗi mô tả lỗi hoặc lỗi validation
}
```

### 2.2 Cấu trúc Phân trang (`PageResponse<T>`)
Áp dụng cho các API lấy danh sách có phân trang (như API lấy danh sách Roadmap).

```json
{
  "data": [ ... ], // Mảng danh sách kết quả
  "pageSize": 20,  // Số phần tử trên một trang
  "totalPages": 5, // Tổng số trang
  "totalElements": 98, // Tổng số phần tử
  "currentPage": 0 // Trang hiện tại (0-indexed)
}
```

---

## 3. Danh Sách Endpoints Chi Tiết

| # | HTTP Method | Path | Auth | Chức năng |
|---|---|---|---|---|
| 1 | `GET` | `/roadmaps` | Optional | Lấy danh sách Roadmap (có phân trang & lọc theo level) |
| 2 | `GET` | `/roadmaps/{slug}` | Optional | Lấy chi tiết một Roadmap (bao gồm Topics, Lessons và tiến độ) |
| 3 | `POST` | `/roadmaps/{slug}/enroll` | **Bắt buộc** | Đăng ký (Enroll) tham gia một khóa học/Roadmap |
| 4 | `POST` | `/roadmaps/{slug}/lessons/{lessonSlug}/start` | **Bắt buộc** | Bắt đầu học một bài học (chuyển sang `IN_PROGRESS`) |
| 5 | `PATCH` | `/roadmaps/{slug}/lessons/{lessonSlug}/progress` | **Bắt buộc** | Cập nhật thủ công tiến độ bài học |
| 6 | `GET` | `/roadmaps/{slug}/enrollment` | **Bắt buộc** | Xem chi tiết thông tin đăng ký & tiến độ của user |

---

### 3.1 GET `/roadmaps` — Danh sách Roadmap

Lấy danh sách các lộ trình học tập đã được xuất bản (`isPublished = true`).

- **Query Parameters:**
  - `page` *(number, optional)*: Trang hiện tại cần lấy (mặc định: `0`, chỉ số bắt đầu từ `0`).
  - `size` *(number, optional)*: Số lượng bản ghi mỗi trang (mặc định: `20`).
  - `sort` *(string, optional)*: Sắp xếp theo trường (ví dụ: `name,asc`).
  - `level` *(string, optional)*: Bộ lọc độ khó/cấp độ. Giá trị chấp nhận: `BEGINNER`, `INTERMEDIATE`, `ADVANCED` (không phân biệt hoa thường).

- **Response:** `PageResponse<RoadmapResponseDTO>`

- **Ví dụ Response thành công:**
```json
{
  "data": [
    {
      "name": "Cấu trúc dữ liệu & Giải thuật cơ bản",
      "slug": "dsa-fundamentals",
      "level": "BEGINNER",
      "thumbnailUrl": "https://images.algotutor.io/dsa.png",
      "description": "Nắm vững các cấu trúc dữ liệu và giải thuật kinh điển.",
      "goal": "Xây dựng nền tảng tư duy lập trình vững chắc.",
      "isPremium": false,
      "topicCount": 4,
      "lessonCount": 18,
      "enrollmentCount": 1542
    }
  ],
  "pageSize": 20,
  "totalPages": 1,
  "totalElements": 1,
  "currentPage": 0
}
```

---

### 3.2 GET `/roadmaps/{slug}` — Chi tiết Roadmap

Trả về chi tiết lộ trình học cùng danh sách Topic và các bài học của Roadmap đó. Nếu người dùng đã đăng ký (hoặc đã đăng nhập), API trả về kèm theo trạng thái mở khóa (`unlocked`), trạng thái hoàn thành (`completed`) và tiến trình học cụ thể của từng bài.

- **Path Parameters:**
  - `slug` *(string, bắt buộc)*: Slug định danh của Roadmap (ví dụ: `dsa-fundamentals`).

- **Response:** `ApiResponse<RoadmapDetailResponseDTO>`

- **Ví dụ Response thành công:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "Cấu trúc dữ liệu & Giải thuật cơ bản",
    "slug": "dsa-fundamentals",
    "level": "BEGINNER",
    "description": "Nắm vững các cấu trúc dữ liệu và giải thuật kinh điển.",
    "goal": "Xây dựng nền tảng tư duy lập trình vững chắc.",
    "thumbnailUrl": "https://images.algotutor.io/dsa.png",
    "isPublished": true,
    "isPremium": false,
    "enrollmentCount": 1542,
    "topicCount": 4,
    "lessonCount": 18,
    "enrolled": true, // true nếu user đã đăng ký roadmap này
    "createdAt": "2026-05-30T10:00:00Z",
    "updatedAt": "2026-06-01T15:30:00Z",
    "topics": [
      {
        "id": 10,
        "name": "Arrays & Hashing",
        "description": "Mảng và bảng băm cơ bản",
        "displayOrder": 1,
        "lessonCount": 2,
        "unlocked": true, // Topic đầu tiên luôn luôn unlocked khi đã enrolled
        "completed": false,
        "completedLessons": 1,
        "totalLessons": 2,
        "createdAt": "2026-05-30T10:05:00Z",
        "updatedAt": "2026-05-30T10:05:00Z",
        "lessons": [
          {
            "id": 101,
            "title": "Mảng động là gì?",
            "slug": "what-is-dynamic-array",
            "type": "THEORY",
            "displayOrder": 1,
            "difficulty": "EASY",
            "status": "COMPLETED", // Trạng thái tiến độ bài học của user
            "unlocked": true,
            "createdAt": "2026-05-30T10:10:00Z",
            "updatedAt": "2026-05-30T10:15:00Z"
          },
          {
            "id": 102,
            "title": "Tìm số trùng lặp",
            "slug": "contains-duplicate",
            "type": "CODING",
            "displayOrder": 2,
            "difficulty": "EASY",
            "status": "IN_PROGRESS",
            "unlocked": true,
            "createdAt": "2026-05-30T10:12:00Z",
            "updatedAt": "2026-06-02T03:00:00Z"
          }
        ]
      }
    ]
  }
}
```

---

### 3.3 POST `/roadmaps/{slug}/enroll` — Đăng ký học

Cho phép người dùng hiện tại tham gia học tập theo Roadmap chỉ định.

- **Path Parameters:**
  - `slug` *(string, bắt buộc)*: Slug định danh của Roadmap.

- **Request Body:** Không có (để trống).

- **Response:** `ApiResponse<EnrollmentResponseDTO>` (HTTP Status: `201 Created`)

> [!WARNING]
> Nếu người dùng đã đăng ký trước đó, API vẫn trả về thông tin đăng ký hiện tại với mã thành công (tránh crash FE) thay vì trả về lỗi.

- **Ví dụ Response thành công:**
```json
{
  "success": true,
  "data": {
    "id": "b18b456e-827c-473d-82fd-76aef0b99182", // UUID dạng chuỗi
    "userId": "e83cf65a-fa63-4720-9bf7-2856f6ba3a8b", // UUID dạng chuỗi của user
    "learningPathId": 1,
    "learningPathName": "Cấu trúc dữ liệu & Giải thuật cơ bản",
    "status": "ACTIVE", // Chú ý: Backend map giá trị enum IN_PROGRESS thành chuỗi "ACTIVE" ở JSON
    "completedAt": null,
    "enrolledAt": "2026-06-02T03:50:00Z"
  }
}
```

---

### 3.4 POST `/roadmaps/{slug}/lessons/{lessonSlug}/start` — Bắt đầu học bài học

Được gọi khi người dùng bấm vào một bài học để bắt đầu đọc lý thuyết hoặc làm bài tập. API này sẽ khởi tạo bản ghi tiến độ bài học với trạng thái `IN_PROGRESS` (nếu bài học chưa từng hoàn thành trước đó).

- **Path Parameters:**
  - `slug` *(string, bắt buộc)*: Slug định danh của Roadmap.
  - `lessonSlug` *(string, bắt buộc)*: Slug định danh của bài học.


- **Response:** `ApiResponse<LessonProgressUpdateResponse>`

- **Ví dụ Response thành công:**
```json
{
  "success": true,
  "data": {
    "lessonId": 102,
    "roadmapId": 1,
    "status": "IN_PROGRESS",
    "updatedAt": "2026-06-02T03:52:00Z"
  }
}
```

---

### 3.5 PATCH `/roadmaps/{slug}/lessons/{lessonSlug}/progress` — Cập nhật tiến độ học tập

Sử dụng để cập nhật trạng thái tiến trình của bài học (ví dụ: đánh dấu hoàn thành một bài lý thuyết/quiz hoặc reset lại bài học).

- **Path Parameters:**
  - `slug` *(string, bắt buộc)*: Slug định danh của Roadmap.
  - `lessonSlug` *(string, bắt buộc)*: Slug định danh của bài học.

- **Request Body:** `LessonProgressUpdateRequest`
```json
{
  "status": "COMPLETED" // Các giá trị hợp lệ: "NOT_STARTED", "IN_PROGRESS", "COMPLETED"
}
```

- **Response:** `ApiResponse<LessonProgressUpdateResponse>`

- **Ví dụ Response thành công:**
```json
{
  "success": true,
  "data": {
    "lessonId": 101,
    "roadmapId": 1,
    "status": "COMPLETED",
    "updatedAt": "2026-06-02T03:54:00Z"
  }
}
```

---

### 3.6 GET `/roadmaps/{slug}/enrollment` — Xem tiến trình chi tiết của Enrollment

Lấy thông tin tổng quan về quá trình học của user hiện tại đối với Roadmap cụ thể cùng danh sách trạng thái của toàn bộ bài học đã tương tác.

- **Path Parameters:**
  - `slug` *(string, bắt buộc)*: Slug định danh của Roadmap.

- **Response:** `ApiResponse<EnrollmentDetailResponseDTO>`

- **Ví dụ Response thành công:**
```json
{
  "success": true,
  "data": {
    "id": "b18b456e-827c-473d-82fd-76aef0b99182",
    "userId": "e83cf65a-fa63-4720-9bf7-2856f6ba3a8b",
    "learningPathId": 1,
    "learningPathName": "Cấu trúc dữ liệu & Giải thuật cơ bản",
    "status": "ACTIVE", // "ACTIVE" | "COMPLETED" | "DROPPED"
    "completedAt": null,
    "createdAt": "2026-06-02T03:50:00Z",
    "lessonProgressions": [
      {
        "lessonId": 101,
        "status": "COMPLETED",
        "updatedAt": "2026-06-02T03:54:00Z"
      },
      {
        "lessonId": 102,
        "status": "IN_PROGRESS",
        "updatedAt": "2026-06-02T03:52:00Z"
      }
    ]
  }
}
```

---

## 4. Các Enums & Mã Lỗi Quan Trọng

### 4.1 Danh sách Enums trên API

#### Cấp độ lộ trình (`Level`)
Dùng để hiển thị hoặc lọc danh sách khóa học:
`BEGINNER` | `INTERMEDIATE` | `ADVANCED`

#### Kiểu bài học (`LessonType`)
Dùng để phân nhánh hiển thị UI (giao diện đọc lý thuyết, làm bài tập trắc nghiệm, hoặc viết code):
`THEORY` | `QUIZ` | `CODING`

#### Độ khó bài tập (`Difficulty`)
`EASY` | `MEDIUM` | `HARD`

#### Tiến trình học bài học (`ProgressStatus`)
`NOT_STARTED` | `IN_PROGRESS` | `COMPLETED`

#### Trạng thái đăng ký học (`EnrollmentStatus`)
> [!IMPORTANT]
> Ở Database / Java, enum này lưu giá trị `IN_PROGRESS`, `COMPLETED`, `DROPPED`.
> Tuy nhiên, trên API JSON Response, do cấu hình Jackson `@JsonValue`, nó trả ra các chuỗi sau:
> - `"ACTIVE"` (tương ứng với Java `IN_PROGRESS`)
> - `"COMPLETED"` (tương ứng với Java `COMPLETED`)
> - `"DROPPED"` (tương ứng với Java `DROPPED`)
>
> FE cần so khớp trạng thái đang học là `"ACTIVE"`, không phải `"IN_PROGRESS"`.

---

### 4.2 Các Mã Lỗi Nghiệp Vụ từ Backend (`code` trong JSON lỗi)

Khi response có `"success": false`, FE nên đọc trường `code` (kiểu số nguyên) để xử lý UX tương ứng:

| Mã lỗi (`code`) | Mô tả nghiệp vụ | HTTP Status tương ứng | Gợi ý xử lý trên FE |
|---|---|---|---|
| `3` | Chưa xác thực tài khoản / Chưa đăng nhập | `401 Unauthorized` | Chuyển hướng sang trang `/login` hoặc mở modal login. |
| `4000` | Lộ trình học không tồn tại | `404 Not Found` | Hiển thị thông báo lỗi/404 trang không tìm thấy lộ trình. |
| `4005` | Bài học không tồn tại | `404 Not Found` | Hiển thị thông báo lỗi bài học không tồn tại. |
| `4017` | Bài học đang bị khóa (`LESSON_LOCKED`) | `403 Forbidden` | Hiển thị thông báo bài học chưa được mở khóa, ngăn người dùng truy cập. |
| `6000` | Lộ trình chưa xuất bản (`LEARNING_PATH_NOT_PUBLISHED`) | `403 Forbidden` | Hiển thị giao diện 404 hoặc thông báo Roadmap chưa sẵn sàng. |
| `6002` | Chưa đăng ký học Roadmap này (`NOT_ENROLLED`) | `403 Forbidden` | Hiển thị nút "Đăng ký khóa học" (Enroll) trước khi cho phép vào học. |
| `6004` | Cú pháp level tìm kiếm không đúng | `400 Bad Request` | Kiểm tra lại các query param gửi đi. |

---

## 5. Quy Luật Mở Khóa & Tiến Trình (Business Logic)

FE cần chú ý các quy luật nghiệp vụ bên dưới để đồng bộ hiển thị giao diện:

1. **Mở khóa Topic (`Topic Unlocking`):**
   - Lộ trình học được chia thành nhiều **Topic**.
   - Topic đầu tiên luôn luôn được mở khóa mặc định ngay sau khi user đăng ký học (`enrolled = true`).
   - Một Topic tiếp theo chỉ được mở khóa khi **Topic ngay trước nó đã hoàn thành** (tức là tất cả các bài học trong Topic trước đã chuyển sang trạng thái `COMPLETED`).
   - Khi một Topic bị khóa (`unlocked = false`), tất cả các bài học bên trong Topic đó đều bị khóa (`unlocked = false`).

2. **Mở khóa Bài học (`Lesson Unlocking`):**
   - Bài học được mở khóa khi và chỉ khi Topic chứa bài học đó được mở khóa (`unlocked = true`).
   - Khi bài học bị khóa, FE cần hiển thị icon ổ khóa, vô hiệu hóa các tương tác click và nếu truy cập trực tiếp bằng URL, cần hiển thị thông báo lỗi/chặn hiển thị nội dung.

3. **Cập nhật Tiến trình:**
   - **Với bài học lý thuyết (`THEORY`):** FE gọi API `PATCH .../progress` với status `"COMPLETED"` khi người dùng cuộn đến cuối trang hoặc đọc đủ thời gian quy định.
   - **Với bài học trắc nghiệm (`QUIZ`):** FE kiểm tra kết quả bài làm của người dùng, nếu số câu đúng đạt yêu cầu tối thiểu (`passingScore` trả về từ API Quiz), FE gọi API `PATCH .../progress` để ghi nhận `"COMPLETED"`.
   - **Với bài học viết code (`CODING`):** Khi người dùng gửi code thành công và backend chấm bài đạt tất cả test case (`ACCEPTED`), **hệ thống backend sẽ tự động cập nhật tiến độ bài học này thành `COMPLETED`**. FE không cần gọi thêm API cập nhật tiến độ nữa, chỉ cần fetch lại trạng thái hoặc nhận kết quả từ API submit để cập nhật UI. Ngoài ra, FE có thể gọi API `GET /submissions/{lessonSlug}` để lấy lịch sử nộp bài code của người dùng cho bài học đó.

---

## 6. Gợi Ý Các Tác Vụ Cần Cập Nhật Trên Frontend

Khi tiến hành chuyển đổi từ dữ liệu Mock sang tích hợp API thật, lập trình viên FE cần rà soát và cập nhật tại các vị trí sau:

1. **Giao diện Danh sách Roadmap (`/roadmaps`):**
   - Sử dụng `useSWR` hoặc `react-query` để fetch dữ liệu từ `GET /roadmaps?level=...&page=...`.
   - Map các trường `thumbnailUrl`, `topicCount`, `lessonCount`, `enrollmentCount` lên giao diện Card Roadmap.

2. **Giao diện Sidebar Học Tập / Chi tiết khóa học (`/learn/[roadmapSlug]`):**
   - Gọi API `GET /roadmaps/[roadmapSlug]` để lấy cấu trúc Topics và Lessons.
   - Sử dụng thông tin `enrolled` của Roadmap để quyết định hiển thị màn hình giới thiệu khóa học kèm nút **"Đăng ký học ngay"** hay hiển thị màn hình học tập chi tiết.
   - Dựa vào trường `unlocked` của Topic / Lesson và trạng thái `status` (`COMPLETED`, `IN_PROGRESS`) để render icon trạng thái tương ứng (ổ khóa, dấu tích xanh hoàn thành, icon hình tròn đang học, hoặc chấm xám chưa học).

3. **Luồng sự kiện khi bắt đầu click học bài mới (`lessonSlug`):**
   - Gọi API `POST /roadmaps/[roadmapSlug]/lessons/[lessonSlug]/start` kèm body `{"status": "IN_PROGRESS"}` để báo cho backend biết user đã bắt đầu học bài này.
