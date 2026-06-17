# Current Lesson API yêu cầu Backend

Tài liệu này mô tả API riêng để Frontend lấy bài học nên tiếp tục trên dashboard.

Endpoint này đang được hook `useCurrentLesson` gọi từ:

- `hooks/use-current-lesson.ts`
- `components/dashboard/learning-overview-card.tsx`

## 1. Mục tiêu

Dashboard cần một CTA chính để user quay lại bài học gần nhất hoặc bài học kế tiếp.

API này trả về đúng một bài học được đề xuất cho user hiện tại, dựa trên enrollment và lesson progress.

## 2. Endpoint

```http
GET /users/me/current-lesson
```

Nếu backend đang chạy sau API gateway với prefix `/api/v1`, route đầy đủ là:

```http
GET /api/v1/users/me/current-lesson
```

## 3. Auth

Yêu cầu user đã đăng nhập.

Frontend gửi credential giống các authenticated API khác:

```http
Cookie: <session-cookie>
```

Hoặc nếu backend dùng bearer token:

```http
Authorization: Bearer <accessToken>
```

## 4. Response wrapper

Frontend đang dùng `fetcher` chung, nên response cần theo wrapper `ApiResponse<T>`:

```ts
type ApiResponse<T> = {
  success: boolean;
  data: T;
  message?: string;
  errors?: string | Record<string, string[]>;
  code?: number;
}
```

## 5. Success response

```http
HTTP 200 OK
Content-Type: application/json
```

```json
{
  "success": true,
  "data": {
    "roadmapSlug": "dsa-fundamentals",
    "lessonSlug": "binary-search",
    "lessonTitle": "Binary Search",
    "roadmapName": "DSA Fundamentals",
    "completionPercentage": 42
  }
}
```

### DTO tối thiểu

```ts
type CurrentLessonResponse = {
  roadmapSlug: string;
  lessonSlug: string;
  lessonTitle: string;
  roadmapName: string;
  completionPercentage: number;
}
```

### Field rules

| Field | Type | Required | Ghi chú |
| --- | --- | --- | --- |
| `roadmapSlug` | `string` | yes | Dùng để tạo link `/learn/{roadmapSlug}/{lessonSlug}` |
| `lessonSlug` | `string` | yes | Lesson được đề xuất học tiếp |
| `lessonTitle` | `string` | yes | Hiển thị trên dashboard |
| `roadmapName` | `string` | yes | Hiển thị dưới lesson title |
| `completionPercentage` | `number` | yes | Số nguyên `0-100` |

## 6. Empty response

Khi user chưa enroll roadmap nào, hoặc tất cả enrollment không còn bài học kế tiếp, backend nên trả:

```http
HTTP 200 OK
Content-Type: application/json
```

```json
{
  "success": true,
  "data": null
}
```

Không khuyến nghị trả `204 No Content` cho endpoint này ở thời điểm hiện tại, vì hook `useCurrentLesson` đang dùng `fetcher` chung và `fetcher` kỳ vọng response body dạng `ApiResponse<T>`.

## 7. Error response

### Chưa đăng nhập

```http
HTTP 401 Unauthorized
```

```json
{
  "success": false,
  "data": null,
  "message": "Unauthorized",
  "code": 1004
}
```

### Lỗi hệ thống

```http
HTTP 500 Internal Server Error
```

```json
{
  "success": false,
  "data": null,
  "message": "Failed to resolve current lesson"
}
```

## 8. Logic chọn bài học

Backend nên chọn bài theo thứ tự ưu tiên:

1. Lấy tất cả enrollment `ACTIVE` của user.
2. Bỏ enrollment đã hoàn thành hoặc không có lesson nào được unlock.
3. Ưu tiên roadmap có lesson progress được cập nhật gần nhất (`lesson_progress.updatedAt DESC`).
4. Trong roadmap được chọn:
   - Nếu có lesson `IN_PROGRESS`, chọn lesson `IN_PROGRESS` cập nhật gần nhất.
   - Nếu không có, chọn lesson đầu tiên chưa `COMPLETED` trong các topic đã unlock, theo `topic.displayOrder ASC`, `lesson.displayOrder ASC`.
5. Nếu roadmap đã hoàn thành toàn bộ lesson, bỏ qua roadmap đó.
6. Nếu không tìm thấy lesson nào, trả `data: null`.

## 9. Tính `completionPercentage`

Tính theo roadmap được chọn:

```txt
completionPercentage = round(completedLessons / totalLessons * 100)
```

Quy tắc:

- `completedLessons`: số lesson có progress `COMPLETED`.
- `totalLessons`: tổng số lesson trong roadmap hoặc tổng số lesson user có quyền học, tùy business rule hiện tại.
- Clamp kết quả trong khoảng `0-100`.
- Trả integer, không trả float.

## 10. Gợi ý query backend

Pseudo logic:

```txt
activeEnrollments = find enrollments by userId where status = ACTIVE

for each enrollment:
  roadmap = enrollment.roadmap
  unlockedTopics = roadmap.topics where unlocked for user
  lessons = unlockedTopics.lessons sorted by topic.displayOrder, lesson.displayOrder
  progress = lessonProgress by userId and lessonIds

  inProgressLesson = progress
    where status = IN_PROGRESS
    order by updatedAt desc
    first

  nextLesson = inProgressLesson
    or first lesson where progress missing or status != COMPLETED

  if nextLesson exists:
    candidate = {
      roadmap,
      lesson: nextLesson,
      lastActivityAt: max(progress.updatedAt, enrollment.updatedAt)
    }

return candidate sorted by lastActivityAt desc first
```

## 11. DTO mở rộng trong tương lai

Frontend hiện chưa cần các field dưới đây, nhưng backend có thể cân nhắc nếu muốn dashboard giàu hơn:

```ts
type CurrentLessonResponse = {
  roadmapSlug: string;
  roadmapName: string;
  lessonSlug: string;
  lessonTitle: string;
  topicName: string;
  lessonType: "THEORY" | "QUIZ" | "CODING" | "VIDEO";
  completionPercentage: number;
  lastStudiedAt: string | null;
  thumbnailUrl: string | null;
}
```

Nếu thêm field mở rộng, vẫn giữ các field tối thiểu để không breaking frontend.

## 12. Acceptance criteria

- `GET /users/me/current-lesson` trả `200 + ApiResponse<CurrentLessonResponse | null>`.
- User chưa có enrollment trả `data: null`.
- User có enrollment active trả lesson link được bằng `/learn/{roadmapSlug}/{lessonSlug}`.
- `completionPercentage` là integer `0-100`.
- Không trả lesson thuộc topic đang khóa.
- Không trả lesson đã `COMPLETED` nếu còn lesson chưa hoàn thành.
- API chạy nhanh cho dashboard. Nên tránh N+1 query khi user có nhiều enrollment.
